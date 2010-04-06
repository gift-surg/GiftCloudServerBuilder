// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.util;

import javax.servlet.http.HttpServletRequest;

import org.restlet.data.Request;

import com.noelios.restlet.ext.servlet.ServletCall;

public class RequestUtil {
	public HttpServletRequest getHttpServletRequest(Request request) {
		return ServletCall.getRequest(request);
	}
}
