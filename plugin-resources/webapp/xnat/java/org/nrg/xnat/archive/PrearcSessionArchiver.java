/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.xnat.archive;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.framework.utilities.Reflection;
import org.nrg.status.ListenerUtils;
import org.nrg.status.StatusProducer;
import org.nrg.status.StatusProducerI;
import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.om.base.BaseXnatExperimentdata.UnknownPrimaryProjectException;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.XFTItem;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.db.ViewManager;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.event.persist.PersistentWorkflowUtils.EventRequirementAbsent;
import org.nrg.xft.event.persist.PersistentWorkflowUtils.JustificationAbsent;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.InvalidValueException;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xft.utils.ValidationUtils.ValidationResults;
import org.nrg.xnat.exceptions.InvalidArchiveStructure;
import org.nrg.xnat.helpers.merge.MergePrearcToArchiveSession;
import org.nrg.xnat.helpers.merge.MergeSessionsA.SaveHandlerI;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.xmlpath.XMLPathShortcuts;
import org.nrg.xnat.restlet.actions.PrearcImporterA.PrearcSession;
import org.nrg.xnat.restlet.actions.TriggerPipelines;
import org.nrg.xnat.turbine.utils.XNATSessionPopulater;
import org.nrg.xnat.turbine.utils.XNATUtils;
import org.restlet.data.Status;
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
public  class PrearcSessionArchiver extends StatusProducer implements Callable<String>,StatusProducerI {
	public static final String MERGED = "Merged";

	private static final String TRIGGER_PIPELINES = "triggerPipelines";

	public static final String PRE_EXISTS = "Session already exists, retry with overwrite enabled";

	public static final String SUBJECT_MOD = "Invalid modification of session subject via archive process.";

	public static final String PROJ_MOD = "Invalid modification of session project via archive process.";

	public static final String LABEL_MOD = "Invalid modification of session label via archive process.";

	public static final String UID_MOD = "Invalid modification of session UID via archive process.";

	public static final String LABEL2 = "label";

	public static final String PARAM_SESSION = "session";
	public static final String PARAM_SUBJECT = "subject";

	private final static Logger logger = LoggerFactory.getLogger(PrearcSessionArchiver.class);
	protected XnatImagesessiondata src;
	protected final XDATUser user;
	protected final String project;
	protected final Map<String,Object> params;
	
	protected final PrearcSession prearcSession;

	private final boolean allowDataDeletion;//should the process delete data from an existing resource
	private final boolean overwrite;//should process proceed if the session already exists
	private final boolean overwrite_files;//should process proceed if the same file is reuploaded
	private final boolean waitFor;
	

	protected PrearcSessionArchiver(final XnatImagesessiondata src, final PrearcSession prearcSession, final XDATUser user, final String project,final Map<String,Object> params, final Boolean allowDataDeletion, final Boolean overwrite, final Boolean waitFor, final Boolean overwrite_files) {
		super(src.getPrearchivePath());
		this.src = src;
		this.user = user;
		this.project = project;
		this.params = params;
		this.allowDataDeletion=(allowDataDeletion==null)?false:allowDataDeletion;
		this.overwrite=(overwrite==null)?false:overwrite;
		this.overwrite_files=(overwrite_files==null)?false:overwrite_files;
		this.prearcSession=prearcSession;
		this.waitFor=waitFor;
	}

	public PrearcSessionArchiver(final PrearcSession session,	
								 final XDATUser user, 
								 final Map<String,Object> params, 
								 boolean allowDataDeletion,
								 final boolean overwrite, 
								 final boolean waitFor, 
								 final Boolean overwrite_files)
	throws IOException,SAXException {
		this((new XNATSessionPopulater(user, 
									   session.getSessionDir(),  
									   session.getProject(), 
									   false)).populate(),
			  session, 
			  user, 
			  session.getProject(), 
			  params, 
			  allowDataDeletion,
			  overwrite, 
			  waitFor,
			  overwrite_files);
	}

	public XnatImagesessiondata getSrc(){
		return src;
	}

	public File getSrcDIR(){
		return prearcSession.getSessionDir();
	}


	public XnatImagesessiondata retrieveExistingExpt() {
		XnatImagesessiondata existing=null;

		//review existing sessions
		if(XNATUtils.hasValue(src.getId())){
			existing=(XnatImagesessiondata)XnatExperimentdata.getXnatExperimentdatasById(src.getId(), user, false);
	}

		if(existing==null){
			existing=(XnatImagesessiondata)XnatExperimentdata.GetExptByProjectIdentifier(project, src.getLabel(), user, false);
		}

		return existing;
	}

	/**
	 * Determine an appropriate session label.
	 * @throws ArchivingException
	 */
	protected void fixSessionLabel()  throws ClientException {
		String label = (String)params.get(PARAM_SESSION);
		
		if(StringUtils.isEmpty(label)){
			label = (String)params.get(URIManager.EXPT_LABEL);
			}

		if(StringUtils.isEmpty(label)){
			label = (String)params.get(LABEL2);
			}

		if (StringUtils.isEmpty(label)) {
		    label = src.getId();
		}

        if (StringUtils.isEmpty(label)) {
            label = prearcSession.getFolderName();
        }

		if (StringUtils.isNotEmpty(label)) {
			src.setLabel(XnatImagesessiondata.cleanValue(label));
		}

		if (!XNATUtils.hasValue(src.getLabel())) {
			failed("unable to deduce session label");
			throw new ClientException("unable to deduce session label");
		}
	}

	public static XnatSubjectdata retrieveMatchingSubject(final String id, final String project,final XDATUser user){
		XnatSubjectdata sub=null;
		if(StringUtils.isNotEmpty(project)){
			sub=XnatSubjectdata.GetSubjectByProjectIdentifier(project, id, user, false);
		}
		if(sub==null){
			sub=XnatSubjectdata.getXnatSubjectdatasById(id, user, false);
		}

		return sub;
	}

	/**
	 * Ensure that the subject label and ID are set in the session --
	 * by deriving and setting them, if necessary.
	 * @throws ArchivingException
	 */
	protected void fixSubject(EventMetaI c,boolean allowNewSubject)  throws ClientException,ServerException {
		String subjectID =  (String)params.get(PARAM_SUBJECT);

		if(!XNATUtils.hasValue(subjectID)){
			subjectID = (String)params.get(URIManager.SUBJECT_ID);
			}

		if(!XNATUtils.hasValue(subjectID)){
			subjectID = src.getSubjectId();
		}

		if (!XNATUtils.hasValue(subjectID)) {
			if (XNATUtils.hasValue(src.getDcmpatientname())) {
				subjectID=XnatImagesessiondata.cleanValue(src.getDcmpatientname());
			}
		}

		if(!XNATUtils.hasValue(subjectID)){
			failed("Unable to identify subject.");
			throw new ClientException("Unable to identify subject.");
		}

		processing("looking for subject " + subjectID);
		XnatSubjectdata subject = retrieveMatchingSubject(subjectID, project, user);

		if (null == subject && XNATUtils.hasValue(subjectID)) {
			final String cleaned = XnatSubjectdata.cleanValue(subjectID);
			if (!cleaned.equals(subjectID)) {
				subject = retrieveMatchingSubject(cleaned, project, user);
			}
		}

		if (null == subject) {
			if(!allowNewSubject){
				throw new ServerException("Unable to create new subject ID"); 
			}
			processing("creating new subject");
			subject = new XnatSubjectdata((UserI)user);
			subject.setProject(project);
			if (XNATUtils.hasValue(subjectID)) {
				subject.setLabel(XnatSubjectdata.cleanValue(subjectID));
			}
			final String newID;
			try {
				newID = XnatSubjectdata.CreateNewID();
			} catch (Exception e) {
				failed("unable to create new subject ID");
				throw new ServerException("Unable to create new subject ID", e);
			}
			subject.setId(newID);
			try {
				SaveItemHelper.authorizedSave(subject,user, false, false,c);
			} catch (Exception e) {
				failed("unable to save new subject " + newID);
				throw new ServerException("Unable to save new subject " + subject, e);
			}
			processing("created new subject " + subjectID);

			src.setSubjectId(subject.getId());
		} else {
			src.setSubjectId(subject.getId());
			processing("matches existing subject " + subjectID);
		}
	}

	/**
	 * Retrieves the archive session directory for the given session.
	 * @return archive session directory
	 * @throws UnknownPrimaryProjectException 
	 * @throws ArchivingException
	 */
	protected File getArcSessionDir() throws ServerException, UnknownPrimaryProjectException{
		final File currentArcDir;
		try {
			final String path = src.getCurrentArchiveFolder();
			currentArcDir = (null == path) ? null : new File(path);
		} catch (InvalidArchiveStructure e) {
			throw new ServerException("couldn't get archive folder for " + src, e);
		}
		final String sessDirName = src.getArchiveDirectoryName();
		final File relativeSessionDir;
		if (null == currentArcDir) {
			relativeSessionDir = new File(sessDirName);
		} else {
			relativeSessionDir = new File(currentArcDir, sessDirName);
		}

		final File rootArchiveDir = new File(src.getPrimaryProject(false).getRootArchivePath());
		final File arcSessionDir = new File(rootArchiveDir, relativeSessionDir.getPath());

		return arcSessionDir;
	}


	/**
	 * Verify that the session isn't already in the transfer pipeline.
	 * @throws AlreadyArchivingException
	 */
	protected void preventConcurrentArchiving(final String id, final XDATUser user) throws ClientException {
		if(!allowDataDeletion){//allow overriding of this behavior via the overwrite parameter
			Collection<? extends PersistentWorkflowI> wrks=PersistentWorkflowUtils.getOpenWorkflows(user, id);
			if (!wrks.isEmpty()){
				this.failed("Session processing in progress:" + ((WrkWorkflowdata)CollectionUtils.get(wrks, 0)).getOnlyPipelineName());
				throw new ClientException(Status.CLIENT_ERROR_CONFLICT,"Session processing in progress:" + ((WrkWorkflowdata)CollectionUtils.get(wrks, 0)).getOnlyPipelineName(),new Exception());
			}
		}
	}

	/**
	 * Updates the prearchive session XML, if possible. Errors here are logged but not
	 * otherwise handled; messing up the prearchive session XML is not a disaster.
	 * @param prearcSessionPath path of session directory in prearchive
	 */
	protected void updatePrearchiveSessionXML(final String prearcSessionPath, final XnatImagesessiondata newSession) {
		final File prearcSessionDir = new File(prearcSessionPath);
		try {
			final FileWriter prearcXML = new FileWriter(prearcSessionDir.getPath() + ".xml");
			try {
				logger.debug("Preparing to update prearchive XML for {}", newSession);
				newSession.toXML(prearcXML, false);
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


	/**
	 * This method will allow users to pass xml path as parameters.  The values supplied will be copied into the loaded session.
	 *
	 * @throws ElementNotFoundException
	 * @throws FieldNotFoundException
	 * @throws InvalidValueException
	 */
	protected void populateAdditionalFields() throws ClientException{
		//prepare params by removing non xml path names
		final Map<String,Object> cleaned=XMLPathShortcuts.identifyUsableFields(params,XMLPathShortcuts.EXPERIMENT_DATA,false);
		XFTItem i = src.getItem();
		
		if(cleaned.size()>0){
		try {
			i.setProperties(cleaned,true);
			i.removeEmptyItems();
		} catch (Exception e) {
			failed("unable to map parameters to valid xml path: " + e.getMessage());
			throw new ClientException("unable to map parameters to valid xml path: ", e);
			}
		}
		src=(XnatImagesessiondata)BaseElement.GetGeneratedItem(i);
	}

	@SuppressWarnings("deprecation")
	public void checkForConflicts(final XnatImagesessiondata src, final File srcDIR, final XnatImagesessiondata existing, final File destDIR) throws ClientException{
		if(existing!=null){
			if(!overwrite){
				failed(PRE_EXISTS);
				throw new ClientException(Status.CLIENT_ERROR_CONFLICT,PRE_EXISTS, new Exception());
			}

			if(!StringUtils.equals(src.getLabel(),existing.getLabel())){
				this.failed(LABEL_MOD);
				throw new ClientException(Status.CLIENT_ERROR_CONFLICT,LABEL_MOD, new Exception());
			}
	
			if(!StringUtils.equals(existing.getProject(),src.getProject())){
				failed(PROJ_MOD);
				throw new ClientException(Status.CLIENT_ERROR_CONFLICT,PROJ_MOD, new Exception());
			}
	
			if(!StringUtils.equals(existing.getSubjectId(),src.getSubjectId())){
				String subjectId = existing.getLabel();
				String newError = SUBJECT_MOD + ": " + subjectId + " Already Exists for another Subject";
				failed(newError);
				throw new ClientException(Status.CLIENT_ERROR_CONFLICT,newError, new Exception());
			}
			
			if(!allowDataDeletion){
				if(StringUtils.isNotEmpty(existing.getUid()) && StringUtils.isNotEmpty(src.getUid())){
					if(!StringUtils.equals(existing.getUid(), src.getUid())){
						failed(UID_MOD);
						throw new ClientException(Status.CLIENT_ERROR_CONFLICT,UID_MOD, new Exception());
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.util.concurrent.Callable#call()
	 */
	public String call()  throws ClientException,ServerException {
		if(StringUtils.isEmpty(project)){
			failed("unable to identify destination project");
			throw new ClientException("unable to identify destination project", new Exception());
		}
		populateAdditionalFields();

		fixSessionLabel();
		
		final XnatImagesessiondata existing=retrieveExistingExpt();
		
		if(existing==null){
			try {
				if(!XNATUtils.hasValue(src.getId()))src.setId(XnatExperimentdata.CreateNewID());
			} catch (Exception e) {
				failed("unable to create new session ID");
				throw new ServerException("unable to create new session ID", e);
			}
		}else{
			src.setId(existing.getId());
			preventConcurrentArchiving(existing.getId(),user);
		}
		

		
		final PersistentWorkflowI workflow;
		final EventMetaI c;
		
		try {
			String justification=(String)params.get(EventUtils.EVENT_REASON);
			if(justification==null){
				justification="standard upload";
			}
			workflow = PersistentWorkflowUtils.buildOpenWorkflow(user, src.getItem(),EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.getType((String)params.get(EventUtils.EVENT_TYPE),EventUtils.TYPE.WEB_SERVICE), (existing==null)?EventUtils.TRANSFER:MERGED, (String)params.get(EventUtils.EVENT_REASON), (String)params.get(EventUtils.EVENT_COMMENT)));
			workflow.setStepDescription("Validating");
			c=workflow.buildEvent();
			try {
				PersistentWorkflowUtils.save(workflow,c);
			} catch (Exception e) {
				throw new ServerException(e);
			}
		} catch (JustificationAbsent e2) {
			throw new ClientException(Status.CLIENT_ERROR_FORBIDDEN, e2);
		} catch (EventRequirementAbsent e2) {
			throw new ClientException(Status.CLIENT_ERROR_BAD_REQUEST, e2);
		}

		fixSubject(c,true);

		
		try {
			processing("validating loaded data");
			validateSesssion();

			final File arcSessionDir = getArcSessionDir();

			if(existing!=null)checkForConflicts(src,this.prearcSession.getSessionDir(),existing,arcSessionDir);

			if(arcSessionDir.exists()){
				this.setStep("Merging", workflow,c);
				processing("merging files data with existing session");
			}else{
				this.setStep("Archiving", workflow,c);
				processing("archiving session");
			}

			final boolean shouldForceQuarantine;
			if(params.containsKey(ViewManager.QUARANTINE) && params.get(ViewManager.QUARANTINE).toString().equalsIgnoreCase("true")){
				shouldForceQuarantine=true;
			}else{
				shouldForceQuarantine=false;
			}

			final EventMetaI savetime=workflow.buildEvent();
			SaveHandlerI<XnatImagesessiondata> saveImpl=new SaveHandlerI<XnatImagesessiondata>() {
				public void save(XnatImagesessiondata merged) throws Exception {					
					if(SaveItemHelper.authorizedSave(merged,user,false,false,c)){
						user.clearLocalCache();
						try {
							MaterializedView.DeleteByUser(user);
						} catch (Exception e) {
							logger.error("",e);
						}
						
						try {
							if (shouldForceQuarantine) {
								src.quarantine(user);
							} else {
								final XnatProjectdata proj = src.getPrimaryProject(false);
								if (null != proj.getArcSpecification().getQuarantineCode() &&
										proj.getArcSpecification().getQuarantineCode().equals(1)) {
									src.quarantine(user);
								}
							}
						} catch (Exception e) {
							logger.error("",e);
						}
					}
				}
			};

			ListenerUtils.addListeners(this, new MergePrearcToArchiveSession(src.getPrearchivePath(),
																			 this.prearcSession.getSessionDir(),
																			 src,
																			 src.getPrearchivepath(),
																			 arcSessionDir,
																			 existing,
																			 arcSessionDir.getAbsolutePath(),
																			 overwrite, 
																			 (allowDataDeletion)?allowDataDeletion:overwrite_files,
																			 saveImpl,user,workflow.buildEvent())).call();

			org.nrg.xft.utils.FileUtils.DeleteFile(new File(this.prearcSession.getSessionDir().getAbsolutePath()+".xml"));
			org.nrg.xft.utils.FileUtils.DeleteFile(this.prearcSession.getSessionDir());
			File timestampedDir = new File(this.prearcSession.getSessionDir().getParent());
			File projectDir = timestampedDir.getParentFile();
			if (timestampedDir.listFiles().length == 0)  {
				org.nrg.xft.utils.FileUtils.DeleteFile(timestampedDir);
			}
			if (projectDir.listFiles().length == 0)  {
				// to keep things tidy, also direct the project-level dir if it's empty
				org.nrg.xft.utils.FileUtils.DeleteFile(projectDir);
			}

			try {
				workflow.setStepDescription(PersistentWorkflowUtils.COMPLETE);
				workflow.setStatus(PersistentWorkflowUtils.COMPLETE);
				PersistentWorkflowUtils.save(workflow,c);
				if(workflow instanceof WrkWorkflowdata){
					((WrkWorkflowdata)workflow).postSave();
				}
			} catch (Exception e1) {
				logger.error("", e1);
			}
			
			postArchive(user,src,params);

			if(!params.containsKey(TRIGGER_PIPELINES) || !params.get(TRIGGER_PIPELINES).equals("false")){
				TriggerPipelines tp=new TriggerPipelines(src,false,user,waitFor);
				tp.call();
			}
		} catch (ServerException e) {
			try {
				workflow.setStatus(PersistentWorkflowUtils.FAILED);
				PersistentWorkflowUtils.save(workflow,c);
				if(workflow instanceof WrkWorkflowdata){
					((WrkWorkflowdata)workflow).postSave();
				}
			} catch (Exception e1) {
				logger.error("", e1);
			}
			throw e;
		} catch (ClientException e) {
			try {
				workflow.setStatus(PersistentWorkflowUtils.FAILED);
				PersistentWorkflowUtils.save(workflow,c);
				if(workflow instanceof WrkWorkflowdata){
					((WrkWorkflowdata)workflow).postSave();
				}
			} catch (Exception e1) {
				logger.error("", e1);
			}
			throw e;
		} catch (Throwable e) {
			try {
				workflow.setStatus(PersistentWorkflowUtils.FAILED);
				PersistentWorkflowUtils.save(workflow,c);
				if(workflow instanceof WrkWorkflowdata){
					((WrkWorkflowdata)workflow).postSave();
				}
			} catch (Exception e1) {
				logger.error("", e1);
			}
			logger.error("",e);
			throw new ServerException(e.getMessage(),new Exception());
		}

		final String url = buildURI(project,src);

		completed("archiving operation complete");
		return url;

	}

	public interface PostArchiveAction {
		public Boolean execute(XDATUser user, XnatImagesessiondata src, Map<String,Object> params);
	}
	
	private void postArchive(XDATUser user, XnatImagesessiondata src, Map<String,Object> params){
		List<Class<?>> classes;
	     try {
	    	 classes = Reflection.getClassesForPackage("org.nrg.xnat.actions.postArchive");

	    	 if(classes!=null && classes.size()>0){
				 for(Class<?> clazz: classes){
					 PostArchiveAction action=(PostArchiveAction)clazz.newInstance();
					 action.execute(user,src,params);
				 }
			 }
	     } catch (Exception exception) {
	         throw new RuntimeException(exception);
	     }
	}
	
	public void setStep(String step,PersistentWorkflowI workflow,EventMetaI c){
		try {
			workflow.setStepDescription(step);
			PersistentWorkflowUtils.save(workflow,c);
		} catch (Exception e1) {
			logger.error("", e1);
		}
	}

	public void validateSesssion() throws ServerException{
		try {
			if(!XNATUtils.hasValue(src.getId()))src.setId(XnatExperimentdata.CreateNewID());
		} catch (Exception e) {
			throw new ServerException("unable to create new session ID", e);
		}

		try {
			final ValidationResults validation = src.validate();
			if (null != validation && !validation.isValid()) {
				throw new ValidationException(validation);
			}
		} catch (Exception e) {
			failed("unable to perform session validation: " + e.getMessage());
			throw new ServerException(e.getMessage(), e);
		}
		}

	public static String buildURI(final String project, final XnatImagesessiondata session){
		final StringBuilder urlb = new StringBuilder();
			urlb.append("/archive/projects/").append(project);
			urlb.append("/subjects/");
			final XnatSubjectdata subjectData = session.getSubjectData();
			if (XNATUtils.hasValue(subjectData.getLabel())) {
				urlb.append(subjectData.getLabel());
			} else {
				urlb.append(subjectData.getId());
			}
			urlb.append("/experiments/").append(session.getLabel());
		return urlb.toString();
	}

	}
