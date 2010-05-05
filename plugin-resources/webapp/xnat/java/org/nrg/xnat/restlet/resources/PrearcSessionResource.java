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
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
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
		// TODO: handle POST. This probably only means archiving the session.
		final File sessionDir = getSessionDir();
		throw new UnsupportedOperationException();
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
		}
	}

	
	/*
	 * (non-Javadoc)
	 * @see org.restlet.resource.Resource#represent(org.restlet.resource.Variant)
	 */
	@Override
	public Representation represent(final Variant variant) throws ResourceException {
		// TODO: try to read session XML
		// TODO: if it doesn't exist, maybe build it?
		// TODO: return session XML
		// TODO: what about an option for returning the entire session?
		final File sessionDir = getSessionDir();
		throw new UnsupportedOperationException();
	}
}
