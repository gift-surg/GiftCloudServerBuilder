// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.nrg.xdat.security.XDATUser;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

public class UserSession extends SecureResource {
	protected XDATUser user = null;
	HttpServletRequest http_request = null;

	public UserSession(Context context, Request request, Response response) {
		super(context, request, response);

		getVariants().add(new Variant(MediaType.TEXT_PLAIN));

		// copy the user from the request into the session
		getHttpSession().setAttribute(USER_ATTRIBUTE,
				getRequest().getAttributes().get(USER_ATTRIBUTE));
	}

	@Override
	public boolean allowDelete() {
		return true;
	}

	@Override
	public boolean allowPost() {
		return true;
	}

	@Override
	public void removeRepresentations() throws ResourceException {
		getHttpSession().invalidate();
	}

	@Override
	public void acceptRepresentation(Representation entity)
			throws ResourceException {
		getResponse().setEntity(sessionIdRepresentation());
	}

	@Override
	public Representation represent(Variant variant) throws ResourceException {
		return sessionIdRepresentation();
	}

	private Representation sessionIdRepresentation() {
		return new StringRepresentation(getHttpSession().getId(),
				MediaType.TEXT_PLAIN);
	}
	}
