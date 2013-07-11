/*
 * org.nrg.xnat.restlet.actions.UserSessionId
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.restlet.actions;

import org.json.JSONObject;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.restlet.representations.JSONObjectRepresentation;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;

import java.util.List;

public class UserSessionId extends SecureResource {

	String userID = null;

	public UserSessionId(Context context, Request request, Response response)
			throws Exception {
		super(context, request, response);

		userID = (String) getParameter(request, "USER_ID");
		// XDATUser user = new XDATUser(userID);
		SessionRegistry sessionRegistry = null;
		sessionRegistry = XDAT.getContextService().getBean("sessionRegistry",
				SessionRegistryImpl.class);
		List<Object> allPrincipals = sessionRegistry.getAllPrincipals();
		List<SessionInformation> l = null;
		int sessionCount = 0;
		for (Object p : allPrincipals) {
			if (p instanceof XDATUser) {
				if (((XDATUser) p).getLogin().equalsIgnoreCase(userID)) {
					l = sessionRegistry.getAllSessions(p, false);
				}
			}
		}
		if (l == null) {
			JSONObject json = new JSONObject();
			json.put(userID, 0);
			Representation rep = new JSONObjectRepresentation(null, json);
			response.setEntity(rep);
			response.setStatus(Status.SUCCESS_CREATED);
		} else {
			sessionCount = l.size();
			JSONObject json = new JSONObject();
			json.put(userID, sessionCount);
			Representation rep = new JSONObjectRepresentation(null, json);
			response.setEntity(rep);
			response.setStatus(Status.SUCCESS_CREATED);
		}

	}

}
