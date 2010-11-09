/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.xnat.archive;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.collections.MultiHashMap;
import org.apache.commons.collections.MultiMap;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.InvalidValueException;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xft.utils.ValidationUtils.ValidationResults;
import org.nrg.xnat.exceptions.InvalidArchiveStructure;
import org.nrg.xnat.restlet.util.XNATRestConstants;
import org.nrg.xnat.turbine.utils.XNATSessionPopulater;
import org.nrg.xnat.turbine.utils.XNATUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 * @author Timothy R. Olsen <olsent@wustl.edu>
 *
 * Archiving new sessions should be straight-forward.
 * For existing sessions
 *   If it contains only new scans
 *      If Modality of session matches
 *      	Add new scans
 *          Regenerate session xml
 *      Else
 *          Throw Exception (we may need to add support for this later)
 *   Else
 *      If Contains only new Files (identified by UID and Class UID From catalogs)
 *          Add new files
 *          Regenerate session xml
 *      Else
 *          If Overwrite=true
 *              Copy new files (delete previous ones)
 *              Regenerate session xml
 *          Else
 *              If also contains new files
 *                  Should it add the files or Fail
 *              Else
 *                  Fail
 *
 */
public final class PrearcSessionArchiver extends StatusProducer implements Callable<URL>,StatusPublisherI {
	private static final String[] SCANS_DIR_NAMES = {"SCANS", "RAW"};
	
	public static final String PARAM_SESSION = "session";
	public static final String PARAM_SUBJECT = "subject";

	private final Logger logger = LoggerFactory.getLogger(PrearcSessionArchiver.class);
	private final XnatImagesessiondata session;
	private final XDATUser user;
	private final String project;
	private final Map<String,Object> params;
	
	private final boolean allowDataDeletion;//should the process delete data from an existing resource
	private final boolean overwrite;//should process proceed if the session already exists

	private boolean shouldForceQuarantine = false;
	

	public PrearcSessionArchiver(final XnatImagesessiondata session,
			final XDATUser user, final String project,
			final Map<String,Object> params, final Boolean allowDataDeletion, final Boolean overwrite) {
		super(session.getPrearchivePath());
		this.session = session;
		this.user = user;
		this.project = project;
		this.params = params;
		this.allowDataDeletion=(allowDataDeletion==null)?false:allowDataDeletion;
		this.overwrite=(overwrite==null)?false:overwrite;
	}

	public PrearcSessionArchiver(final File sessionDir,
			final XDATUser user, final String project,
			final MultiMap params, boolean allowDataDeletion)
	throws IOException,SAXException {
		this((new XNATSessionPopulater(user, sessionDir, project, false)).populate(), user, project, params, allowDataDeletion,overwrite);
	}

	public boolean forceQuarantine(final boolean shouldForce) {
		final boolean prev = this.shouldForceQuarantine;
		this.shouldForceQuarantine = shouldForce;
		return prev;
	}
	
	public void fail(final String msg) throws ArchivingException{
		failed(msg);
		throw new ArchivingException(msg);
	}

	public XnatExperimentdata retrieveExistingExpt() throws ArchivingException{
		XnatExperimentdata existing=null;

		//review existing sessions
		if(XNATUtils.hasValue(session.getId())){
			existing=XnatExperimentdata.getXnatExperimentdatasById(session.getId(), user, false);
		}

		if(existing!=null){
			if(!session.getLabel().equals(existing.getLabel())){
				fail("new session label matches preexisting label for a different experiment");
			}
		}else{
			existing=XnatExperimentdata.GetExptByProjectIdentifier(project, session.getLabel(), user, false);
		}

		if(existing!=null){
			if(!existing.getProject().equals(project)){
				fail("conflict: illegal project change.");
			}

			if(existing!=null && !overwrite){
				fail("conflict: session already exists.");
			}

			if(!existing.getXSIType().equals(session.getXSIType())){
				//this should catch differences between session types as well as label reuse vs other data types.
				//TODO: this needs to be smarter, if a single secondary capture is uploaded for an existing MR, that should be allowed.
				fail("new data conflicts with XSI type of previously archived data.");
			}
		}

		return existing;
	}
	
	/**
	 * Determine an appropriate session label.
	 * @throws ArchivingException
	 */
	private void fixSessionLabel() throws ArchivingException {
		final String label = (String)params.get(PARAM_SESSION);
			if (null != label) {
				session.setLabel(XnatImagesessiondata.cleanValue(label));
			}
		if (!XNATUtils.hasValue(session.getLabel())) {
			if (XNATUtils.hasValue(session.getDcmpatientid())) {
				session.setLabel(XnatImagesessiondata.cleanValue(session.getDcmpatientid()));
			}
		}
		if (!XNATUtils.hasValue(session.getLabel())) {
			failed("unable to deduce session label");
			throw new ArchivingException("unable to deduce session label");
		}		
	}
	
	/**
	 * Ensure that the subject label and ID are set in the session --
	 * by deriving and setting them, if necessary.
	 * @throws ArchivingException
	 */
	private void fixSubject() throws ArchivingException {
		final String paramLabel = (String)params.get(PARAM_SUBJECT);
			if (null != paramLabel) {
				session.setSubjectId(paramLabel);
			}

		final String subjectID = session.getSubjectId();

		processing("looking for subject " + subjectID);
		XnatSubjectdata subject = session.getSubjectData();
		if (null == subject && XNATUtils.hasValue(subjectID)) {
			final String cleaned = XnatSubjectdata.cleanValue(subjectID);
			if (!cleaned.equals(subjectID)) {
				session.setSubjectId(cleaned);
				subject = session.getSubjectData();
			}
		}

		if (null == subject) {
			processing("creating new subject");
			subject = new XnatSubjectdata((UserI)user);
			subject.setProject(project);
			if (XNATUtils.hasValue(subjectID)) {
				subject.setLabel(XnatSubjectdata.cleanValue(subjectID));
			}
			final String newID;
			try {
				newID = XnatSubjectdata.CreateNewID();
			} catch (Throwable e) {
				failed("unable to create new subject ID");
				throw new ArchivingException("Unable to create new subject ID", e);
			}
			subject.setId(newID);
			try {
				subject.save(user, false, false);
			} catch (Throwable e) {
				failed("unable to save new subject " + newID);
				throw new ArchivingException("Unable to save new subject " + subject, e);
			}
			processing("created new subject " + subjectID);

			session.setSubjectId(subject.getId());
		} else {
			processing("matches existing subject " + subjectID);
		}
	}
	
	/**
	 * Retrieves the archive session directory for the given session.
	 * Verifies that the path will not overwrite data in an existing session.
	 * @return archive session directory
	 * @throws ArchivingException
	 */
	private File getArcSessionDir()
	throws ArchivingException {
		final File currentArcDir;
		try {
			final String path = session.getCurrentArchiveFolder();
			currentArcDir = null == path ? null : new File(path);
		} catch (InvalidArchiveStructure e) {
			throw new ArchivingException("couldn't get archive folder for " + session, e);
		}
		final String sessDirName = session.getArchiveDirectoryName();
		final File relativeSessionDir;
		if (null == currentArcDir) {
			relativeSessionDir = new File(sessDirName);
		} else {
			relativeSessionDir = new File(currentArcDir, sessDirName);
		}
		
		final File rootArchiveDir = new File(session.getPrimaryProject(false).getRootArchivePath());
		final XnatExperimentdata existing=this.retrieveExistingExpt();
		final File arcSessionDir = new File(rootArchiveDir, relativeSessionDir.getPath());
		
		// Verify that the proposed archive session directory does not already contain data
		if (arcSessionDir.exists() && !overwrite) {
			for (final String scansDirName : SCANS_DIR_NAMES) {
				final File scansDir = new File(arcSessionDir, scansDirName);
				if (scansDir.exists() && FileUtils.HasFiles(scansDir)) {
					failed("project " + project + " already contains a session directory named " + session.getLabel());
					throw new DuplicateSessionLabelException(session.getLabel(), project);
				}
			}
		}
		return arcSessionDir;
	}

	
	/**
	 * Verify that the session isn't already in the transfer pipeline.
	 * @throws AlreadyArchivingException
	 */
	private void preventConcurrentArchiving() throws AlreadyArchivingException {
		final CriteriaCollection cc= new CriteriaCollection("AND");
		cc.addClause("wrk:workFlowData.ID",session.getId());
		cc.addClause("wrk:workFlowData.pipeline_name","Transfer");
		cc.addClause("wrk:workFlowData.status","In Progress");
		if (!WrkWorkflowdata.getWrkWorkflowdatasByField(cc, user, false).isEmpty()){
			throw new AlreadyArchivingException();
		}	
	}
	
	/**
	 * Makes the scan paths absolute; also sets the scan content type to RAW if it's not already set.
	 * @param arcSessionPath
	 */
	private void fixScans(final File arcSessionDir) {
		final String root = arcSessionDir.getPath().replace('\\','/') + "/";
		for (final XnatImagescandataI scan : session.getScans_scan()) {
			for (final XnatAbstractresourceI file : scan.getFile()) {
				// appendToPaths() is poorly named: should maybe be prependPathsWith()
				((XnatAbstractresource)file).prependPathsWith(root);
				// TODO: this is surrounded by a try/catch(Throwable) that logs but
				// TODO: otherwise discards the Throwable. Was this needed?
				if (XNATUtils.isNullOrEmpty(((XnatAbstractresource)file).getContent())) {
					((XnatResource)file).setContent("RAW");
				}
			}
		}
	}
	
	/**
	 * Updates the prearchive session XML, if possible. Errors here are logged but not
	 * otherwise handled; messing up the prearchive session XML is not a disaster.
	 * @param prearcSessionPath path of session directory in prearchive
	 */
	private void updatePrearchiveSessionXML(final String prearcSessionPath) {
		final File prearcSessionDir = new File(prearcSessionPath);
		try {
			final FileWriter prearcXML = new FileWriter(prearcSessionDir.getPath() + ".xml");
			try {
				logger.debug("Preparing to update prearchive XML for {}", session);
				session.toXML(prearcXML, true);
			} catch (RuntimeException e) {
				logger.error("unable to update prearchive session XML", e);
				warning("updated prearchive session XML could not be written: " + e.getMessage());
			} catch (SAXException e) {
				logger.error("attempted to write invalid updated prearchive session XML", e);
				warning("updated prearchive session XML is invalid: " + e.getMessage());
			} finally {
				prearcXML.close();
			}
		} catch (FileNotFoundException e) {
			logger.error("unable to update prearchive session XML", e);
			warning("prearchive session XML not found, cannot update");
		} catch (IOException e) {
			logger.error("error updating prearchive session XML", e);
			warning("could not update prearchive session XML: " + e.getMessage());
		}
	}
	
	private boolean doTransfer(final String prearcSessionPath) {
		final Transfer transfer = new Transfer(TurbineUtils.GetFullServerPath(),
				TurbineUtils.GetSystemName(),
				AdminUtils.getAdminEmailId());
		if (null == session.getUser()) {
		    session.getItem().setUser(user);
		}
		transfer.setImageSession(session);
		transfer.setPlaceInRaw(false);
		transfer.setPrearc(prearcSessionPath);
		return transfer.execute();
	}
	

	/**
	 * This method will allow users to pass xml path as parameters.  The values supplied will be copied into the loaded session.
	 *
	 * @throws ElementNotFoundException
	 * @throws FieldNotFoundException
	 * @throws InvalidValueException
	 */
	private void populateAdditionalFields() throws ElementNotFoundException,FieldNotFoundException,InvalidValueException{
		//prepare params by removing non xml path names
		final Map<String,Object> cleaned=new HashMap<String,Object>();
		for(final Object key: params.keySet()){
			if(key instanceof String){
				if(((String)key).matches(XNATRestConstants.XML_PATH_REGEXP)){
					cleaned.put((String)key, params.get(key));
				}
			}
		}

		session.getItem().setProperties(cleaned, true);
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.concurrent.Callable#call()
	 */
	public URL call() throws ArchivingException {
		fixSessionLabel();
		fixSubject();
		
		try {
			populateAdditionalFields();
		} catch (Exception e) {
			failed("unable to map parameters to valid xml path: " + e.getMessage());
			throw new ArchivingException("unable to map parameters to valid xml path: ", e);
		}

		final File arcSessionDir = getArcSessionDir();
		

		processing("validating loaded data");
		
		try {
			if(!XNATUtils.hasValue(session.getId()))session.setId(XnatExperimentdata.CreateNewID());
		} catch (Exception e) {
			throw new ArchivingException("unable to create new session ID", e);
		}

		try {
			final ValidationResults validation = session.validate();
			if (null != validation && !validation.isValid()) {
				throw new ValidationException(validation);
			}
		} catch (ArchivingException e) {
			throw e;
		} catch (Exception e) {
			failed("unable to perform session validation: " + e.getMessage());
			throw new ArchivingException("unable to perform session validation", e);
		}
		
		preventConcurrentArchiving();
		fixScans(arcSessionDir);
				
		processing("archiving session");
		
		// save the session to the database
		try {
			if (session.save(user, false, allowDataDeletion)) {
				user.clearLocalCache();
				MaterializedView.DeleteByUser(user);
			    
				if (this.shouldForceQuarantine) {
					session.quarantine(user);
				} else {
					final XnatProjectdata proj = session.getPrimaryProject(false);
					if (null != proj.getArcSpecification().getQuarantineCode() &&
							proj.getArcSpecification().getQuarantineCode().equals(1)) {
						session.quarantine(user);
					}
				}				
			}
		} catch (Exception e) {
			logger.error("unable to commit session to database", e);
			failed("error committing session to database: " + e.getMessage());
			throw new ArchivingException("unable to commit session to database", e);
		}
		
		final String prearcSessionPath = session.getPrearchivepath();
		updatePrearchiveSessionXML(prearcSessionPath);
		final boolean successful = doTransfer(prearcSessionPath);
		// TODO: what about schema element manipulation?
		// TODO: what about project scoping line?

		if (successful) {
			final StringBuilder urlb = new StringBuilder(TurbineUtils.GetFullServerPath());
			urlb.append("/REST/projects/").append(project);
			urlb.append("/subjects/");
			final XnatSubjectdata subjectData = session.getSubjectData();
			if (XNATUtils.hasValue(subjectData.getLabel())) {
				urlb.append(subjectData.getLabel());
			} else {
				urlb.append(subjectData.getId());
			}
			urlb.append("/experiments/").append(session.getLabel());
			try {
				final URL url = new URL(urlb.toString());
				completed("archiving operation complete");
				return url;
			} catch (MalformedURLException e) {
				throw new RuntimeException("invalid session URL", e);
			}
		} else {
			failed("archiving operation failed");
			throw new ArchivingException("archiving operation failed");
		}
	}

	}
