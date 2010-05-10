/**
 * Copyright 2010 Washington University
 */
package org.nrg.xnat.restlet.resources;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xnat.turbine.modules.actions.LoadImageData;
import org.nrg.xnat.turbine.modules.actions.StoreImageSession;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.FileRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public final class PrearcSessionResource extends Resource {
	private static final String USER_ATTR = "user";
	private static final String PROJECT_ATTR = "PROJECT_ID";
	private static final String SESSION_TIMESTAMP = "SESSION_TIMESTAMP";
	private static final String SESSION_LABEL = "SESSION_LABEL";
	private static final String CRLF = "\r\n";
	private static final String[] SCANS_DIR_NAMES = {"RAW", "SCANS"};
	
	private final Logger logger = LoggerFactory.getLogger(PrearcSessionResource.class);

	private final XDATUser user;
	private final String project, timestamp, session;
	private final Form queryForm;

	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public PrearcSessionResource(Context context, Request request,
			Response response) {
		super(context, request, response);

		final Map<String,Object> attrs = getRequest().getAttributes();

		// User comes either from traditional session or via XnatSecureGuard
		user = (XDATUser) attrs.get(USER_ATTR);

		// Project, timestamp, session are explicit in the request
		project = (String)attrs.get(PROJECT_ATTR);
		timestamp = (String)attrs.get(SESSION_TIMESTAMP);
		session = (String)attrs.get(SESSION_LABEL);

		queryForm = request.getResourceRef().getQueryAsForm();

		getVariants().add(new Variant(MediaType.TEXT_XML));
		//		getVariants().add(new Variant(MediaType.APPLICATION_GNU_ZIP));
		//		getVariants().add(new Variant(MediaType.APPLICATION_ZIP));
	}

	private File getSessionDir() throws ResourceException {
		final File prearcDir = PrearcSessionListResource.getPrearcDir(user, project);
		final File tsDir = new File(prearcDir, timestamp);
		final File sessDir = new File(tsDir, session);
		if (sessDir.isDirectory()) {
			return sessDir;
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.restlet.resource.Resource#acceptRepresentation(org.restlet.resource.Representation)
	 */
	@Override
	public void acceptRepresentation(final Representation representation)
	throws ResourceException {
		// TODO: handle POST. Operations handled via POST:
		final File sessionDir = getSessionDir();
		final String action = queryForm.getFirstValue("action");
		if ("archive".equals(action)) {
			doArchive(sessionDir);
		} else if ("build".equals(action)) {
			throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED, "session build operation not yet implemented");
		} else if ("move".equals(action)) {
			// TODO:   move the session to a different project
			throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED, "move operation not yet implemented");
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"unsupported action on prearchive session: " + action);
		}
	}

	private String doArchive(final File sessionDir) throws ResourceException {
		final StringBuilder messages = new StringBuilder();
		final LoadImageData loader = new LoadImageData();
		final File sessionXML = new File(sessionDir.getPath() + ".xml");
		final XnatImagesessiondata session;
		try {
			session = loader.getSession(user, sessionXML, project, false);
		} catch (FileNotFoundException e) {
			logger.debug("user attempted to archive session with no XML", e);
			throw new ResourceException(Status.CLIENT_ERROR_CONFLICT,
					"Session metadata could not be read. Send a build request and try again.", e);
		} catch (IOException e) {
			logger.error("unable to read session document", e);
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Unable to read session document", e);
		} catch (SAXException e) {
			logger.error("error in session document", e);
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Unable to parse session document", e);
		}

		/*
		 * Ensure that the subject label and ID are set (by setting them, if necessary)
		 */
		logger.trace("looking for subject for session " + sessionDir.getName());
		XnatSubjectdata subject = session.getSubjectData();
		// TODO: check for REST-specified subject label
		final String subjectID = session.getSubjectId();
		if (null == subject && LoadImageData.hasValue(subjectID)) {
			final String cleaned = XnatSubjectdata.cleanValue(subjectID);
			if (!cleaned.equals(subjectID)) {
				session.setSubjectId(cleaned);
				subject = session.getSubjectData();
			}
		}

		if (null == subject) {
			subject = new XnatSubjectdata((UserI)user);
			subject.setProject(project);
			if (LoadImageData.hasValue(subjectID)) {
				subject.setLabel(XnatSubjectdata.cleanValue(subjectID));
			}
			try {
				subject.setId(XnatSubjectdata.CreateNewID());
				subject.save(user, false, false);
				
				logger.trace("created new subject {}", subject);
				messages.append("PROCESSING: Created new subject ").append(subjectID);
				messages.append(" (").append(subject.getId()).append(")");
				messages.append(CRLF);

				session.setSubjectId(subject.getId());
			} catch (Exception e) {
				logger.error("unable to build new subject", e);
				throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
						"Unable to build new subject", e);
			}
		} else {
			messages.append("PROCESSING: Matches existing subject ").append(subjectID);
			messages.append(" (").append(subject.getId()).append(")");
			messages.append(CRLF);
		}

		/*
		 * Determine a session label
		 */
		// TODO: check for REST-specified session label
		if (!LoadImageData.hasValue(session.getLabel())) {
			if (LoadImageData.hasValue(session.getDcmpatientid())) {
				session.setLabel(session.getDcmpatientid());
			}
		}
		if (!LoadImageData.hasValue(session.getLabel())) {
			logger.debug("unable to deduce session label for {}", session);
			throw new ResourceException(Status.CLIENT_ERROR_CONFLICT,
					"No session label could be deduced for this session. " +
			"Resubmit the request, specifying a session label.");
		}

		/*
		 * Don't overwrite an existing session.
		 */
		final StoreImageSession store = new StoreImageSession();	// TODO: remove StoreImageSession; see below
		final String arcSessionPath;
		try {
			arcSessionPath = store.getArcSessionPath(session);
		} catch (Exception e) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Unable to determine archive path for session", e);
		}
		final File archivedSessionDir = new File(arcSessionPath);
		if (archivedSessionDir.exists()) {
			for (final String scansDirName : SCANS_DIR_NAMES) {
				final File scansDir = new File(archivedSessionDir, scansDirName);
				if (scansDir.exists() && FileUtils.HasFiles(scansDir)) {
					throw new ResourceException(Status.CLIENT_ERROR_CONFLICT,
							"Session " + session.getLabel() + " already exists in project " + project);
				}
			}
		}

		store.template = session;
		logger.trace("archiving session {}", session);
		
		// TODO: StoreImageSession is really a Turbine thing. We need a common foundation
		// to do the transfer without requiring RunData and Context. Once we have that,
		// much of the above code can likely be removed. (See also
		// org.nrg.xnat.turbine.modules.actions.ImageUpload)

		throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED, "archive operation not implemented");
	}

	/*
	 * (non-Javadoc)
	 * @see org.restlet.resource.Resource#removeRepresentations()
	 */
	@Override
	public void removeRepresentations() throws ResourceException {
		final File sessionDir = getSessionDir();
		final File sessionXML = new File(sessionDir.getPath() + ".xml");
		sessionXML.delete();
		if (sessionXML.exists()) {
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Unable to delete session XML " + sessionXML);
		}

		try {
			final File tsDir = sessionDir.getParentFile();

			final Project antProject = new Project();
			antProject.setBaseDir(tsDir);

			final Delete delete = new Delete();
			delete.setProject(antProject);
			delete.setDir(sessionDir);
			delete.execute();

			if (0 == tsDir.listFiles().length) {
				tsDir.delete();
			}
		} finally {
			if (sessionXML.exists()) {
				throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
						"Unable to delete session XML " + sessionXML);
			}
			if (sessionDir.exists()) {
				throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
						"Unable to delete session " + session);
			}
		}
	}


	/*
	 * (non-Javadoc)
	 * @see org.restlet.resource.Resource#represent(org.restlet.resource.Variant)
	 */
	@Override
	public Representation represent(final Variant variant) throws ResourceException {
		final File sessionDir = getSessionDir();
		final MediaType mt = variant.getMediaType();
		if (MediaType.TEXT_XML.equals(mt)) {
			// Return the session XML, if it exists
			final File sessionXML = new File(sessionDir.getPath());
			if (!sessionXML.isFile()) {
				throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
						"The named session exists, but its XNAT session document is not available." +
				"The session is likely invalid or incomplete.");
			}
			return new FileRepresentation(sessionXML, variant.getMediaType(), 0);
		} else if (MediaType.APPLICATION_GNU_ZIP.equals(mt)) {
			throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED,
			".tgz request not yet implemented");
		} else if (MediaType.APPLICATION_ZIP.equals(mt)) {
			throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED,
			".zip request not yet implemented");
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"Requested type " + mt + " is not supported");
		}
	}
}
