/*
 * org.nrg.xnat.restlet.util.BrowserDetectorTest
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.restlet.util;

import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BrowserDetectorTest {
	public static final String SAMPLE_FIREFOX_UA = "Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.1.3) Gecko/20090913 Firefox/3.5.3";
	public static final String SAMPLE_SAFARI_UA = "Mozilla/5.0 (Macintosh; U; PPC Mac OS X 10_5_8; en-us) AppleWebKit/532.0+ (KHTML, like Gecko) Version/4.0.3 Safari/531.9.2009";
	public static final String SAMPLE_IE_UA = "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; WOW64; Trident/4.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; InfoPath.3; .NET CLR 4.0.20506)";
	public static final String SAMPLE_CHROME_UA = "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/532.1 (KHTML, like Gecko) Chrome/4.0.219.4 Safari/532.1";
	public static final String SAMPLE_OPERA_UA = "Opera 9.7 (Windows NT 5.2; U; en) ";

	private BrowserDetectorI dectector;
	private HttpServletRequest request;

	@Before
	public void setUp() throws Exception {
		dectector = new BrowserDetector();

		request = mock(HttpServletRequest.class);
		setUserAgent(SAMPLE_FIREFOX_UA);
	}

	@Test
	public void getUserAgentShouldProvideUserAgentFromRequest() {
		assertEquals(SAMPLE_FIREFOX_UA, dectector.getUserAgent(request));
	}

	@Test
	public void getUserAgentShouldPassNull() {
		setUserAgent(null);
		assertNull(dectector.getUserAgent(request));
	}

	@Test
	public void getUserAgentShouldIgnoreNullRequests() {
		assertNull(dectector.getUserAgent(null));
	}

	@Test
	public void isBrowserShouldDetectFirefox() {
		assertTrue(dectector.isBrowser(request));
	}

	@Test
	public void isBrowserShouldDetectIE() {
		setUserAgent(SAMPLE_IE_UA);
		assertTrue(dectector.isBrowser(request));
	}

	@Test
	public void isBrowserShouldDetectChrome() {
		setUserAgent(SAMPLE_CHROME_UA);
		assertTrue(dectector.isBrowser(request));
	}

	@Test
	public void isBrowserShouldDetectSafari() {
		setUserAgent(SAMPLE_SAFARI_UA);
		assertTrue(dectector.isBrowser(request));
	}

	@Test
	public void isBrowserShouldDetectOpera() {
		setUserAgent(SAMPLE_OPERA_UA);
		assertTrue(dectector.isBrowser(request));
	}

	@Test
	public void isBrowserShouldNotDetectPythonUrllib() {
		setUserAgent("Python-urllib/2.5");
		assertFalse(dectector.isBrowser(request));
	}

	@Test
	public void isBrowserShouldNotDetectEmptyUA() {
		setUserAgent("");
		assertFalse(dectector.isBrowser(request));
	}

	@Test
	public void isBrowserShouldNotDetectNullUA() {
		setUserAgent(null);
		assertFalse(dectector.isBrowser(request));
	}

	private void setUserAgent(String userAgent) {
		when(request.getHeader("User-Agent")).thenReturn(userAgent);
	}
}
