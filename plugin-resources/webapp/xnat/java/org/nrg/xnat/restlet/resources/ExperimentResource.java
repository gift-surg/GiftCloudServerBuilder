// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources;

import java.util.ArrayList;

import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatSubjectassessordata;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

public class ExperimentResource extends ItemResource {
	XnatProjectdata proj=null;

	XnatExperimentdata expt = null;
	String exptID=null;
	
	public ExperimentResource(Context context, Request request, Response response) {
		super(context, request, response);
		
			String pID= (String)request.getAttributes().get("PROJECT_ID");
			if(pID!=null){
				proj = XnatProjectdata.getXnatProjectdatasById(pID, user, false);
			}
			
		if (proj == null) {
			ArrayList<XnatProjectdata> matches = XnatProjectdata
					.getXnatProjectdatasByField(
							"xnat:projectData/aliases/alias/alias", pID, user,
							false);
			if (matches.size() > 0) {
				proj = matches.get(0);
			}
		}

			exptID= (String)request.getAttributes().get("EXPT_ID");
			if(exptID!=null){
				this.getVariants().add(new Variant(MediaType.TEXT_XML));
			}else{
			response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
		}
	}
	

	@Override
	public Representation getRepresentation(Variant variant) {	
		MediaType mt = overrideVariant(variant);

		if(expt==null&& exptID!=null){
			expt=XnatExperimentdata.getXnatExperimentdatasById(exptID, user, false);
			

			if(proj!=null){
				if(expt==null){
					expt=(XnatSubjectassessordata)XnatExperimentdata.GetExptByProjectIdentifier(proj.getId(), exptID,user, false);
				}
			}
		}
		
		if(expt!=null){
			return this.representItem(expt.getItem(),mt);
		}else{
			this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,
					"Unable to find the specified experiment.");
			return null;
		}

	}

}
