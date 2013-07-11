/*
 * org.nrg.xnat.restlet.util.BrowserDetectorI
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:40 PM
 */
package org.nrg.xnat.restlet.util;

import javax.servlet.http.HttpServletRequest;

public interface BrowserDetectorI {

	public abstract String getUserAgent(HttpServletRequest request);

	public abstract boolean isBrowser(HttpServletRequest request);

}