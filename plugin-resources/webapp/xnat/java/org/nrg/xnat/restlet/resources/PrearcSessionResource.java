/**
 * Copyright 2010 Washington University
 */
package org.nrg.xnat.restlet.resources;

import java.io.File;
import java.util.Map;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.nrg.xdat.security.XDATUser;
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

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public final class PrearcSessionResource extends Resource {
	private static final String USER_ATTR = "user";
	private static final String PROJECT_ATTR = "PROJECT_ID";
	private static final String SESSION_TIMESTAMP = "SESSION_TIMESTAMP";
	private static final String SESSION_LABEL = "SESSION_LABEL";

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
			// TODO:   archive the session in the current project
			throw new UnsupportedOperationException("archive operation not implemented");
		} else if ("move".equals(action)) {
			// TODO:   move the session to a different project
			throw new UnsupportedOperationException("move operation not implemented");
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,
					"unsupported action on prearchive session: " + action);
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
