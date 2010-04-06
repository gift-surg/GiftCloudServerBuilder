// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.guard;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.Test;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.restlet.util.BrowserDetectorTest;
import org.restlet.Filter;
import org.restlet.data.ChallengeRequest;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;

public class XnatSecureGuardTest {
	private CustomXnatSecureGuard guard;
	private XDATUser user;
	private Response response;
	private Request request;
	private HttpServletRequest httpRequest;
	private HttpSession httpSession;
	private Map<String, Object> requestAttributes;

	@Before
	public void setUp() throws Exception {

		guard = new CustomXnatSecureGuard();

		user = mock(XDATUser.class);
		response = mock(Response.class);
		request = mock(Request.class);
		requestAttributes = new HashMap<String, Object>();
		when(request.getAttributes()).thenReturn(requestAttributes);

		httpSession = mock(HttpSession.class);
		httpRequest = mock(HttpServletRequest.class);
		when(httpRequest.getSession()).thenReturn(httpSession);
	}

	@Test
	public void shouldContinueWhenSessionExists() {
		when(httpSession.getAttribute("user")).thenReturn(user);
		assertEquals(Filter.CONTINUE, beforeHandle());
		assertEquals(requestAttributes.get("user"), user);
	}

	@Test
	public void shouldContinueWhenCorrectCredentials() {
		when(request.getChallengeResponse()).thenReturn(
				createChallengeResponse());

		assertEquals(Filter.CONTINUE, beforeHandle());
		assertEquals(requestAttributes.get("user"), user);
	}

	@Test
	public void shouldStopWhenBadUsername() {
		when(request.getChallengeResponse()).thenReturn(
				createChallengeResponse());

		user = null;
		assertEquals(Filter.STOP, beforeHandle());
		verify(httpSession).invalidate();
	}

	@Test
	public void shouldStopWhenBadPassword() throws Exception {
		when(request.getChallengeResponse()).thenReturn(
				createChallengeResponse());
		when(user.login(anyString())).thenThrow(
				new XDATUser.PasswordAuthenticationException("bob"));

		assertEquals(Filter.STOP, beforeHandle());
		verify(httpSession).invalidate();
	}

	@Test
	public void shouldStopWhenNoCredentialsOrSession() throws Exception {
		assertEquals(Filter.STOP, beforeHandle());
		verify(httpSession).invalidate();
	}

	@Test
	public void shouldPreferExistingSessionOverCredentials() throws Exception {
		when(httpSession.getAttribute("user")).thenReturn(user);

		when(request.getChallengeResponse()).thenReturn(
				createChallengeResponse());
		when(user.login(anyString())).thenThrow(
				new XDATUser.PasswordAuthenticationException("bob"));

		assertEquals(Filter.CONTINUE, beforeHandle());
	}

	@Test
	public void shouldChallengeNonBrowsers() {
		when(httpRequest.getHeader("User-Agent")).thenReturn("Java/1.6");

		assertEquals(Filter.STOP, beforeHandle());
		verify(response).setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
		verify(response).setChallengeRequest(any(ChallengeRequest.class));
	}

	@Test
	public void shouldRedirectBrowsersToLogin() {
		when(httpRequest.getHeader("User-Agent")).thenReturn(
				BrowserDetectorTest.SAMPLE_FIREFOX_UA);

		assertEquals(Filter.STOP, beforeHandle());

		verify(response).setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
		verify(response, never()).setChallengeRequest(
				any(ChallengeRequest.class));
		verify(response).setEntity(any(Representation.class));
	}

	private int beforeHandle() {
		return guard.beforeHandle(request, response);
	}

	private ChallengeResponse createChallengeResponse() {
		return new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "bob",
				"passw0rd");
	}

	class CustomXnatSecureGuard extends XnatSecureGuard {
		@Override
		protected HttpServletRequest getHttpServletRequest(Request request) {
			return httpRequest;
		}

		@Override
		protected XDATUser getUser(String login) throws Exception {
			return user;
		}
	}
}
