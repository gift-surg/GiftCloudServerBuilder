/**
 * Copyright 2010 Washington University
 */
package org.nrg.xnat.restlet.resources;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.archive.AlreadyArchivingException;
import org.nrg.xnat.archive.ArchivingException;
import org.nrg.xnat.archive.DuplicateSessionLabelException;
import org.nrg.xnat.archive.PrearcSessionArchiver;
import org.nrg.xnat.archive.ValidationException;
import org.nrg.xnat.restlet.util.XNATRestConstants;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
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

import com.google.common.collect.Multimap;

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
	
	public static final String POST_ACTION_ARCHIVE = "archive";
	public static final String POST_ACTION_BUILD = "build";
	public static final String POST_ACTION_MOVE = "move";
	
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

	
	@Override
	public final boolean allowPost() { return true; }
	
	/*
	 * (non-Javadoc)
	 * @see org.restlet.resource.Resource#acceptRepresentation(org.restlet.resource.Representation)
	 */
	@Override
	public void acceptRepresentation(final Representation representation)
	throws ResourceException {
		final File sessionDir = getSessionDir();
		final String action = queryForm.getFirstValue("action");
		if (POST_ACTION_ARCHIVE.equals(action)) {
			//allowDataDeletion will govern if prexisting xml data can be deleted by new xml.
			boolean allowDataDeletion=false;
			boolean overwrite=false;
			
			if(queryForm.contains(XNATRestConstants.ALLOW_DATA_DELETION)){
				allowDataDeletion=Boolean.valueOf(queryForm.getFirstValue(XNATRestConstants.ALLOW_DATA_DELETION));
			}
			
			if(queryForm.contains(XNATRestConstants.OVERWRITE)){
				allowDataDeletion=Boolean.valueOf(queryForm.getFirstValue(XNATRestConstants.OVERWRITE));
			}
			
			final String entity = doArchive(sessionDir,allowDataDeletion,overwrite);
			final Response response = getResponse();
			response.setEntity(entity + CRLF, MediaType.TEXT_URI_LIST);
		} else if (POST_ACTION_BUILD.equals(action)) {
			// TODO: (re)build the session document
			throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED, "session build operation not yet implemented");
		} else if (POST_ACTION_MOVE.equals(action)) {
			// TODO: move the session to a different project
			throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED, "move operation not yet implemented");
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"unsupported action on prearchive session: " + action);
		}
	}

	private Map<String,Object> makeMap(final Form form) {
		final Map<String,Object> m = new Hashtable<String,Object>();
		for (final Parameter param : form) {
			m.put(param.getName(), param.getValue());
		}
		
		// TODO: What if the user supplies paramaters via the body of the message as the session archiving page would.  
		// Need to parse it into a Form and review its parameters as well. Form bodyForm = new Form(entity);
		
		return m;
	}
	
	private String doArchive(final File sessionDir, final Boolean allowDataDeletion,final Boolean overwrite) throws ResourceException {
		final PrearcSessionArchiver archiver;
		try {
			archiver = new PrearcSessionArchiver(sessionDir, user, project, makeMap(queryForm),allowDataDeletion,overwrite);
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
		
		try {
			// TODO: need a status listener?
			return archiver.call().toString();
		} catch (AlreadyArchivingException e) {
			logger.debug("user attempted to archive session already in transfer", e);
			throw new ResourceException(e.getStatus(), e.getMessage(), e);
		} catch (DuplicateSessionLabelException e) {
			logger.debug("user attempted to archive session already in archive", e);
			throw new ResourceException(e.getStatus(), e.getMessage(), e);
		} catch (ValidationException e) {
			logger.error("session validation failed", e);
			throw new ResourceException(e.getStatus(), e.getMessage(), e);
		} catch (ArchivingException e) {
			// Other archiving exceptions may be noteworthy
			logger.warn("archiving failed", e);
			throw new ResourceException(e.getStatus(), e.getMessage(), e);
		} finally {
				archiver.dispose();
		}
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
			final File sessionXML = new File(sessionDir.getPath() + ".xml");
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
