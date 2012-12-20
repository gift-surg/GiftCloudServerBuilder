package org.nrg.xnat.restlet.extensions;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatSubjectassessordata;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTable;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xnat.restlet.XnatRestlet;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

/*
 * This restlet will get the workflows for a given experiment in a project.
 * One can filter the workflows down to the pipeline_name and get the status
 * of the pipeline completion for the experiment
 */
/**
 * Uses:
 * /data/archive/workflows/pipeline_name?project=PROJECT_ID&experiment=PROJECT_LABEL&tatus=Running&display=Latest
 * /data/archive/workflows/pipeline_name?experiment=XNAT_ID
 * /data/archive/workflows/pipeline_name?experiment=XNAT_ID&param_NAME=STR&param_NAME_VALUE=STR

 * TODO: /data/archive/workflows?subject=XNAT_ID
 * TODO: /data/archive/workflows/pipeline_name upload a list file of XNAT session ids
 */


@XnatRestlet({"/services/workflows","/services/workflows/{PIPELINE_NAME}"})
public class WorkflowsRestlet extends SecureResource {
    private static final Log _log = LogFactory.getLog(WorkflowsRestlet.class);

	String pipeline_name = null;
	String project_id = null;
	String xnat_id = null;
	String status = null;
	final static String DISPLAY_LATEST = "LATEST";
	final static String DISPLAY_ALL = "ALL";
	String display = DISPLAY_LATEST;
	Map<String,Object> form_params = null;
	public WorkflowsRestlet(Context context, Request request, Response response) {
        super(context, request, response);
		pipeline_name= (String)getParameter(request,"PIPELINE_NAME");
		project_id = this.getQueryVariable("project");
		status = this.getQueryVariable("status");
		display = this.getQueryVariable("display");
		if (display == null) 
			display = DISPLAY_LATEST;
		
		if(pipeline_name != null){
			project_id = this.getQueryVariable("project");
			xnat_id = this.getQueryVariable("experiment");
			form_params = this.getQueryVariablesAsMap();

			if (project_id !=null && xnat_id != null) {
				if (display.equalsIgnoreCase(DISPLAY_LATEST))
				  this.getVariants().add(new Variant(MediaType.TEXT_XML));
				this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
			}else if (project_id == null && xnat_id != null) {
				if (display.equalsIgnoreCase(DISPLAY_LATEST))
					this.getVariants().add(new Variant(MediaType.TEXT_XML));
				this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
			}
		}else if (status != null) {
			this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
		}else {
		    response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
	    }
		System.out.println("WorkflowRestlet invoked " + status + " " + pipeline_name + " " + xnat_id + " " + project_id);
	}

	@Override
	public Representation getRepresentation(Variant variant) {
		MediaType mt = overrideVariant(variant);
		XnatExperimentdata expt = null;
		if(xnat_id != null){
			expt=XnatExperimentdata.getXnatExperimentdatasById(xnat_id, user, false);
			if(project_id!=null){
				if(expt==null){
					expt=(XnatSubjectassessordata)XnatExperimentdata.GetExptByProjectIdentifier(project_id, xnat_id,user, false);
				}
			}
		}

		if(expt!=null){
		   try {
			    org.nrg.xft.search.CriteriaCollection cc = new CriteriaCollection("AND");
			    addWorkflowParamsConstraints(cc);
			    cc.addClause("wrk:workflowData.ID",expt.getId());
		        if (status != null)
		          cc.addClause("wrk:workflowData.status",status);	
		        //cc.addClause("wrk:workflowData.ExternalID",expt.getProject());
		        cc.addClause("wrk:workflowData.pipeline_name","LIKE","%"+pipeline_name.toLowerCase()+"%");
		        
		        if (display.equalsIgnoreCase(DISPLAY_LATEST)) {
	            	org.nrg.xft.collections.ItemCollection items = org.nrg.xft.search.ItemSearch.GetItems(cc, user, false);
		            ArrayList workitems = items.getItems("wrk:workflowData.launch_time","DESC");
	            	XFTItem latestWrkFlow = (XFTItem)workitems.get(0);
				    return representItem(latestWrkFlow.getItem(), mt);
	            }else {
			        org.nrg.xft.search.ItemSearch itemSearch = new org.nrg.xft.search.ItemSearch(user, "wrk:workflowdata", cc);
	            	XFTTable table = itemSearch.executeToTable(false);
	        		Hashtable<String,Object> params=new Hashtable<String,Object>();
	            	params.put("title", "All workflows");
	            	return representTable(table, mt, params);
	            }
	       }catch(Exception e) {
			   _log.error(e);
				this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,
				"Unable to find workflow entry for pipeline " + pipeline_name + " and experiment." + expt.getId());
				return null;
		   }
		}else if (status != null) {
		    org.nrg.xft.search.CriteriaCollection cc = new CriteriaCollection("AND");
	        cc.addClause("wrk:workflowData.status",status);
	        if (pipeline_name != null) {
	        	cc.addClause("wrk:workflowData.pipeline_name","LIKE","%/"+pipeline_name+"%");
	        }
	        try {
		        org.nrg.xft.search.ItemSearch itemSearch = new org.nrg.xft.search.ItemSearch(user, "wrk:workflowdata", cc);
            	XFTTable table = itemSearch.executeToTable(false);
        		Hashtable<String,Object> params=new Hashtable<String,Object>();
            	params.put("title", "All " + status + " workflows");
            	return representTable(table, mt, params);
	        }catch(Exception e) {
				   logger.error(e);
					this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,
					"Unable to find workflow entry with status " + status);
					return null;
	        }
		}else{
			this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,
					"Unable to find the specified experiment.");
			return null;
		}
	}

	@Override
	public boolean allowPost() {
		return false;
	}
	
	@Override
	public boolean allowPut() {
		return false;
	}

	@Override
	public boolean allowDelete() {
		return false;
	}


	private void addWorkflowParamsConstraints(CriteriaCollection cc) {
		if (form_params.size()>0) {
		for(String s:form_params.keySet()){
			if (s.startsWith("param_")) {
				String wrkflow_paramName = s.substring(6);
				String wrkflow_paramValue = (String)form_params.get(s);
				cc.addClause("wrk:workflowData.executionEnvironment.parameters.parameter.name", s);
				cc.addClause("wrk:workflowData.executionEnvironment.parameters.parameter",wrkflow_paramValue);

			}
		}
		}

	}
	
}
