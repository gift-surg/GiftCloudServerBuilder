/*
 * org.nrg.xnat.restlet.services.prearchive.PrearchiveBatchMove
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 12/19/13 3:01 PM
 */

/**
 * 
 */
package org.nrg.xnat.restlet.services.prearchive;

import org.apache.log4j.Logger;
import org.nrg.action.ClientException;
import org.nrg.xdat.XDAT;
import org.nrg.xnat.helpers.prearchive.*;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author tolsen01
 *
 */
public class PrearchiveBatchMove extends BatchPrearchiveActionsA {
	private static final String NEW_PROJECT = "newProject";
	static org.apache.log4j.Logger logger = Logger
			.getLogger(PrearchiveBatchMove.class);

	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public PrearchiveBatchMove(Context context, Request request,
			Response response) {
		super(context, request, response);
	}

	private String newProject = null;

	@Override
	public void handleParam(String key, Object o) throws ClientException {
		if (key.equals(SRC)) {
			srcs.add((String) o);
		} else if (key.equals(NEW_PROJECT)) {
			newProject = (String) o;
		} else if (key.equals(ASYNC)) {
			if (((String) o).equals("false")) {
				async = false;
			}
		}
	}

	@Override
	public void handlePost() {

		try {
			loadBodyVariables();
			// maintain parameters
			loadQueryVariables();
		} catch (ClientException e) {
			this.getResponse().setStatus(e.getStatus(), e);
			return;
		}

		if (newProject == null) {
			this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
					"Move operation requires 'newProject'");
			return;
		} else {
			try {
				if (!PrearcUtils.canModify(user, newProject)) {
					this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,
							"Invalid permissions for new project.");
					return;
				}
			} catch (Exception e) {
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e);
				return;
			}
		}

		final List<SessionDataTriple> ss = new ArrayList<SessionDataTriple>();

		for (final String src : srcs) {
			File sessionDir;
			try {
				SessionDataTriple s = buildSessionDataTriple(src);
				if (!PrearcUtils.canModify(user, s.getProject())) {
					this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,
							"Invalid permissions for new project.");
					return;
				}
				ss.add(s);
				sessionDir = PrearcUtils.getPrearcSessionDir(user,
						s.getProject(), s.getTimestamp(), s.getFolderName(),
						false);

				if (PrearcDatabase.setStatus(s.getFolderName(),
						s.getTimestamp(), s.getProject(),
						PrearcUtils.PrearcStatus.QUEUED_MOVING)) {
					SessionData session = new SessionData();
					session.setTimestamp(s.getTimestamp());
					session.setProject(s.getProject());
					session.setFolderName(s.getFolderName());

					MoveSessionRequest request = new MoveSessionRequest(user,
							session, sessionDir, newProject);
					XDAT.sendJmsRequest(request);
				}
			} catch (Exception e) {
				logger.error("", e);
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e);
				return;
			}
		}

		final Response response = getResponse();
		try {
			response.setEntity(updatedStatusRepresentation(ss,
					overrideVariant(getPreferredVariant())));
		} catch (Exception e) {
			logger.error("", e);
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e);
			return;
		}
	}

}
