/*
 * org.nrg.xnat.restlet.resources.UserSessionTest
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.restlet.resources;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.restlet.resource.StringRepresentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserSessionTest extends SecureResourceTestCase {
	private UserSession resource;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		resource = new UserSession(context, request, response);
	}

	@Test
	public void shouldSetUserIntoSession() {
		verify(httpSession).setAttribute("user", user);
	}

	@Test
	public void shouldAllowPost() {
		assertTrue(resource.allowPost());
	}

	@Test
	public void shouldAllowDelete() {
		assertTrue(resource.allowDelete());
	}

	@Test
	public void shouldAllowGet() {
		assertTrue(resource.allowGet());
	}

	@Test
	public void shouldInvalidateOnDelete() throws Exception {
		resource.removeRepresentations();

		verify(httpSession).invalidate();
	}

	@Test
	public void shouldProvideSessionIdOnGet() throws Exception {
		when(httpSession.getId()).thenReturn("ABC123");
		assertEquals("ABC123", resource.represent(null).getText());
	}

	@Test
	public void shouldProvideSessionIdOnPost() throws Exception {
		when(httpSession.getId()).thenReturn("ABC123");
		resource.acceptRepresentation(null);
		ArgumentCaptor<StringRepresentation> arg = ArgumentCaptor
				.forClass(StringRepresentation.class);
		verify(response).setEntity(arg.capture());
		assertEquals("ABC123", arg.getValue().getText());
	}
}
