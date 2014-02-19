/*
 * org.nrg.xnat.restlet.representations.RESTLoginRepresentation
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.restlet.representations;

import org.apache.log4j.Logger;
import org.apache.turbine.util.TurbineException;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.XFTItem;
import org.restlet.data.MediaType;
import org.restlet.data.Request;

import java.util.Hashtable;

public class RESTLoginRepresentation extends TurbineScreenRepresentation {
	static org.apache.log4j.Logger logger = Logger.getLogger(RESTLoginRepresentation.class);
	XFTItem item = null;
	boolean includeSchemaLocations=true;

	public RESTLoginRepresentation(MediaType mt, Request _request, XDATUser _user) throws TurbineException {
		super(mt,_request,_user,new Hashtable<String,Object>());	
		
    	data.getParameters().add("rest_uri", request.getOriginalRef().toString());
	}

	@Override
	public String getScreen() {
		return "Login.vm";
	}
}
