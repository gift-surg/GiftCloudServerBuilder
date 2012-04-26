package org.nrg.xnat.restlet.resources;

import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatPvisitdata;
import org.nrg.xdat.om.XnatSubjectassessordata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils.EventRequirementAbsent;
import org.nrg.xnat.utils.WorkflowUtils;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

public class VisitResource  extends ItemResource{

	XnatPvisitdata visit = null;
	XnatProjectdata proj = null;
	
	public VisitResource(Context context, Request request, Response response) {
		super(context, request, response);
		
		//validate the project ID if one was passed in.
		String pID = (String)request.getAttributes().get("PROJECT_ID");
		if(pID!=null){
			proj = XnatProjectdata.getProjectByIDorAlias(pID, user, false);
			if(proj == null){
				response.setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Unable to identify project " + pID);
				return;
			}
		}

		//let's try to find the visit. If a project was passed in, let's also make sure the visit is a member of that project.
		String visitID= (String)request.getAttributes().get("VISIT_ID");		
		if(visitID!=null){
			visit=XnatPvisitdata.getXnatPvisitdatasById(visitID, user, false);
			if(proj !=null && visit!=null){
				//make sure the visit has the passed in project
				if(!visit.hasProject(proj.getId())){
					response.setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Visit does not belong to project " + proj.getId());
				}
			}
		}
		
		if(visit!=null){			
			this.getVariants().add(new Variant(MediaType.TEXT_XML));
		}else{
			response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
		}
	}	
	
	@Override
	public boolean allowDelete() {
		return true;
	}
	@Override
	public void handleDelete(){
		try {
			if(proj == null){
				getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Unable to identify project. Please use the project/visit URI");
			}
			
			if(visit!=null){
				PersistentWorkflowI wrk;
				try {
					wrk = WorkflowUtils.buildOpenWorkflow(user, visit.getItem(),newEventInstance(EventUtils.CATEGORY.DATA,(getAction()!=null)?getAction():EventUtils.getDeleteAction(visit.getXSIType())));
					EventMetaI c=wrk.buildEvent();
					
					try {
						String msg=visit.delete(proj, user, this.isQueryVariableTrue("removeFiles"),c);
						if(msg!=null){
							WorkflowUtils.fail(wrk, c);
							this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,msg);
							return;
						}else{
							WorkflowUtils.complete(wrk, c);
						}
					} catch (Exception e) {
						try {
							WorkflowUtils.fail(wrk, c);
						} catch (Exception e1) {
							logger.error("",e1);
						}
						logger.error("",e);
						this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e.getMessage());
						return;
					}
				} catch (EventRequirementAbsent e1) {
					logger.error("",e1);
					this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,e1.getMessage());
					return;
				}
			}
		} catch (Exception e) {
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			logger.error("",e);
		}
	}
	
	@Override
	public Representation getRepresentation(Variant variant) {	
		MediaType mt = overrideVariant(variant);	
		if(visit!=null){
			return this.representItem(visit.getItem(),mt);
		}else{
			this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Unable to find the specified visit.");
			return null;
		}
	}
}
