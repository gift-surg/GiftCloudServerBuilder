// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.util;

import javax.servlet.http.HttpServletRequest;

public interface BrowserDetectorI {

	public abstract String getUserAgent(HttpServletRequest request);

	public abstract boolean isBrowser(HttpServletRequest request);

}