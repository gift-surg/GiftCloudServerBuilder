// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources;

import java.util.ArrayList;

import org.nrg.xdat.om.XnatProjectdata;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

public class ProjectArchive extends ItemResource {
	final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ProjectArchive.class);
	XnatProjectdata proj=null;
	
	public ProjectArchive(Context context, Request request, Response response) {
		super(context, request, response);
		
		String pID= (String)getParameter(request,"PROJECT_ID");
		if(pID!=null){
			proj = XnatProjectdata.getProjectByIDorAlias(pID, user, false);
		}
		
		if(proj!=null){
			this.getVariants().add(new Variant(MediaType.TEXT_XML));				
		}else{
			response.setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Unable to find project '"+ pID + "'");
			return;
		}
	}
	

	@Override
	public Representation getRepresentation(Variant variant) {	
		MediaType mt = overrideVariant(variant);
		
		if(proj!=null){
	        	return this.representItem(proj.getArcSpecification().getItem(),MediaType.TEXT_XML);
		}else{
			this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Unable to find the specified scan.");
		}

		return null;

	}
}