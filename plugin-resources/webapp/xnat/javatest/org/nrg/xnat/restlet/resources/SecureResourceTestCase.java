// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.nrg.xdat.security.XDATUser;
import org.restlet.Context;
import org.restlet.data.ClientInfo;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;

import com.noelios.restlet.ext.servlet.ServletCall;
import com.noelios.restlet.http.HttpRequest;

public class SecureResourceTestCase {
	protected Context context;
	protected HttpServletRequest httpRequest;
	protected Map<String, Object> requestAttributes;
	protected HttpSession httpSession;
	protected Request request;
	protected Response response;
	protected XDATUser user;

	@Before
	public void setUp() throws Exception {
		context = mock(Context.class);
		user = mock(XDATUser.class);
		response = mock(Response.class);
		httpSession = mock(HttpSession.class);

		httpRequest = mock(HttpServletRequest.class);
		when(httpRequest.getSession()).thenReturn(httpSession);

		requestAttributes = new HashMap<String, Object>();
		requestAttributes.put("user", user);

		ServletCall servletCall = mock(ServletCall.class);
		when(servletCall.getRequest()).thenReturn(httpRequest);

		request = mock(HttpRequest.class);
		when(request.getAttributes()).thenReturn(requestAttributes);
		when(((HttpRequest)request).getHttpCall()).thenReturn(servletCall);
		
		// just prevent null pointers, might need to revisit when more fully testing
		when(request.getResourceRef()).thenReturn(new Reference());
		when(request.getClientInfo()).thenReturn(new ClientInfo());
	}
}
