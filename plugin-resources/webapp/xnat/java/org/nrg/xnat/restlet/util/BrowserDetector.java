// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.util;

import javax.servlet.http.HttpServletRequest;

public class BrowserDetector implements BrowserDetectorI {
	private static final String UA_HEADER = "User-Agent";
	private static final String MOZILLA_FAMILY_ID = "Mozilla";
	private static final String OPERA_FAMILY_ID = "Opera";

	public String getUserAgent(HttpServletRequest request) {
		if (request != null) {
			return request.getHeader(UA_HEADER);
		}
		return null;
	}

	public boolean isBrowser(HttpServletRequest request) {
		final String userAgent = getUserAgent(request);
		if (userAgent != null) {
			if (userAgent.startsWith(MOZILLA_FAMILY_ID)
					|| userAgent.startsWith(OPERA_FAMILY_ID)) {
				return true;
			}
		}
		return false;
	}
}
