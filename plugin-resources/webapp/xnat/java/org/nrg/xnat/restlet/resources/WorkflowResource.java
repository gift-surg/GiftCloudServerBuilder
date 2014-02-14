package org.nrg.xnat.restlet.resources;

import java.util.Date;

import org.apache.axis.utils.StringUtils;
import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.XFTItem;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xnat.utils.WorkflowUtils;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WorkflowResource extends ItemResource {

	private final String workflowId;
	
	private static final Logger log = LoggerFactory.getLogger(WorkflowResource.class);
	
	public WorkflowResource(Context context, Request request, Response response) {
		super(context, request, response);
		workflowId = (String)getParameter(request,"WORKFLOW_ID");
		getVariants().add(new Variant(MediaType.TEXT_XML));
	}
	
	@Override public boolean allowDelete(){ return false; }
	@Override public boolean allowPut()   { return true;  }
	@Override public boolean allowGet()   { return true;  }
	
	@Override
	public void handlePut() {
		
		XFTItem item             = null;
		WrkWorkflowdata workflow = null;
		
		try{
			// Create the new workflow item based on information from the user.
			item=this.loadItem("wrk:workflowData",true);
			String pipeline_name = item.getStringProperty("pipeline_name");
			Date launch_time   = item.getDateProperty("launch_time");
			String id            = item.getStringProperty("id");
			
			if(workflowId != null && !workflowId.isEmpty()){
				// Lookup the workflow by the ID provided by the user.
				workflow = (WrkWorkflowdata)WorkflowUtils.getUniqueWorkflow(user, workflowId);
				if(workflow != null){
					// If the workflow exists, set the workflow id on the new item. 
					item.setProperty("wrk_workflowData_id", workflowId);
				}else{
					// If we couldn't find the workflow, 404
					this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Unable to find the specified workflow.");
					return;
				}
			} else {
				// Lookup the workflow by pipeline_name, launch_time, and ID
				workflow = (WrkWorkflowdata)WorkflowUtils.getUniqueWorkflow(user, pipeline_name, id, launch_time);
			}
			
			// If the workflow exists, Make sure the user has permission to edit an existing workflow. 
			if(workflow != null && !canUserEditWorkflow(user, workflow)){
				// If the user is not allow to modify this workflow, 403
				this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "You are not allowed to make changes to this workflow.");
				return;
			}
			
			// Id, launch_time, data_type, and pipeline_name are all required in order to save a new workflow
			if(workflow == null && (StringUtils.isEmpty(id) || StringUtils.isEmpty(item.getStringProperty("launch_time")) || StringUtils.isEmpty(pipeline_name) || StringUtils.isEmpty(item.getStringProperty("data_type")))){
				this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Id, launch_time, data_type and pipeline_name are all required.");
				return;
			}
			
			// Save the workflow
			EventMetaI c = EventUtils.DEFAULT_EVENT(user, "Workflow Update");
			SaveItemHelper.authorizedSave(item, user, false, false, c);

		}catch(Exception e){ 
			log.error("Unable to save Workflow.", e);
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.toString());
			return;
		}
	}
	
	private boolean canUserEditWorkflow(XDATUser user, WrkWorkflowdata workflow){
		return workflow.getInsertUser() == user || user.isSiteAdmin();
	}
	
	@Override 
	public Representation getRepresentation(Variant variant) { 
		WrkWorkflowdata workflow = null;
		if(workflowId != null && !workflowId.isEmpty()){
			// Lookup the workflow by the ID provided by the user.
			workflow = (WrkWorkflowdata)WorkflowUtils.getUniqueWorkflow(user, workflowId);
		}else{
			try{
				// Lookup the workflow by pipeline_name, launch_time, and ID
				XFTItem item         = this.loadItem("wrk:workflowData",true);
				String pipeline_name = item.getStringProperty("pipeline_name");
				Date launch_time   = item.getDateProperty("launch_time");
				String id            = item.getStringProperty("id");
				workflow = (WrkWorkflowdata)WorkflowUtils.getUniqueWorkflow(user, pipeline_name, id, launch_time);
			}catch(Exception e) { 
				log.error("Unable to find Workflow.", e);
			}
		}
		
		if(workflow != null){
			// If we found the workflow, represent it with the requested media type
			MediaType mt = this.getRequestedMediaType();
			return this.representItem(workflow.getItem(), (mt == null) ? MediaType.TEXT_XML : mt);
		}
		else{
			// If we couldn't find the workflow, 404
			this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Unable to find the specified workflow.");
			return null;
		}
	}
}