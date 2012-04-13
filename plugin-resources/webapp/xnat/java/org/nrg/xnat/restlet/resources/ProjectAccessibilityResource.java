// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources;

import java.util.ArrayList;

import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xnat.utils.WorkflowUtils;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

public class ProjectAccessibilityResource extends SecureResource {
	XnatProjectdata proj = null;
	String access = null;
	
	public ProjectAccessibilityResource(Context context, Request request, Response response) {
		super(context, request, response);
		
			String pID= (String)getParameter(request,"PROJECT_ID");
			if(pID!=null){
				proj = XnatProjectdata.getProjectByIDorAlias(pID, user, false);
			}
			access=(String)getParameter(request,"ACCESS_LEVEL");

			if(proj!=null)
			this.getVariants().add(new Variant(MediaType.TEXT_PLAIN));
			else{
				response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
			}
				
		}

	@Override
	public boolean allowGet() {
		return true;
	}

	@Override
	public boolean allowPut() {
		return true;
	}
	
	
	@Override
	public void handlePut() {
		if(proj!=null && access!=null){
            try {
				if (!user.canDelete(proj)){
					getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
					return;
				}
			
				String currentAccess = proj.getPublicAccessibility();
				if (!currentAccess.equals(access)){
					PersistentWorkflowI wrk=WorkflowUtils.buildProjectWorkflow(user, proj,newEventInstance(EventUtils.CATEGORY.PROJECT_ACCESS,EventUtils.MODIFY_PROJECT_ACCESS+" ("+ access + ")"));
                    EventMetaI c=wrk.buildEvent();
                    if(proj.initAccessibility(access, true,user,c)){
                    	WorkflowUtils.complete(wrk, c);
                    }
				}
				
				getResponse().setEntity(getRepresentation(getVariants().get(0)));
                Representation selectedRepresentation = getResponse().getEntity();
                if (getRequest().getConditions().hasSome()) {
                    final Status status = getRequest().getConditions()
                            .getStatus(getRequest().getMethod(),
                                    selectedRepresentation);

                    if (status != null) {
                        getResponse().setStatus(status);
                        getResponse().setEntity(null);
                    }
                }
			} catch (Exception e) {
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			}
		}
	}

	@Override
	public Representation getRepresentation(Variant variant) {
		String currentLevel="";
		try {
			if(proj!=null){
				currentLevel= proj.getPublicAccessibility();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new StringRepresentation(currentLevel,variant.getMediaType());
	}

	
}
