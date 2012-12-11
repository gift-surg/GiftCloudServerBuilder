package org.nrg.xnat.restlet.extensions;

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatSubjectassessordata;
import org.nrg.xft.XFTItem;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xnat.restlet.XnatRestlet;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.restlet.Context;
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

@XnatRestlet({"/archive/workflows", "/archive/workflows/{PIPELINE_NAME}"})

/**
 * Uses:
 * /data/archive/workflows/pipeline_name?project=PROJECT_ID&experiment=PROJECT_LABEL
 * /data/archive/workflows/pipeline_name?experiment=XNAT_ID
 * TODO: /data/archive/workflows?subject=XNAT_ID
 * TODO: /data/archive/workflows/pipeline_name upload a list file of XNAT session ids
 */

public class WorkflowsRestlet extends SecureResource {
    private static final Log _log = LogFactory.getLog(WorkflowsRestlet.class);

	String pipeline_name = null;
	String project_id = null;
	String xnat_id = null;

	public WorkflowsRestlet(Context context, Request request, Response response) {
        super(context, request, response);
		String pipeline_name= (String)getParameter(request,"PIPELINE_NAME");

		if(pipeline_name != null){
			project_id = this.getQueryVariable("project");
			xnat_id = this.getQueryVariable("experiment");
			if (project_id !=null && xnat_id != null) {
				this.getVariants().add(new Variant(MediaType.TEXT_XML));
				this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
//				this.getVariants().add(new Variant(MediaType.APPLICATION_EXCEL));
			}else if (project_id == null && xnat_id != null) {
				this.getVariants().add(new Variant(MediaType.TEXT_XML));
				this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
//				this.getVariants().add(new Variant(MediaType.APPLICATION_EXCEL));
//			}else if (project_id == null && xnat_id == null && ) {
//				this.getVariants().add(new Variant(MediaType.APPLICATION_EXCEL));
//				this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
			}
		}else{
		    response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
	    }
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
		        cc.addClause("wrk:workflowData.ID",expt.getId());
		        //cc.addClause("wrk:workflowData.ExternalID",expt.getProject());
		        cc.addClause("wrk:workflowData.pipeline_name","LIKE","%/"+pipeline_name+"%");
		        org.nrg.xft.collections.ItemCollection items = org.nrg.xft.search.ItemSearch.GetItems(cc,user,false);
	            ArrayList workitems = items.getItems("wrk:workflowData.launch_time","DESC");
	            XFTItem latestWrkFlow = (XFTItem)workitems.get(0);
				return representItem(latestWrkFlow.getItem(), mt);
		   }catch(Exception e) {
			   logger.error(e);
				this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,
				"Unable to find workflow entry for pipeline " + pipeline_name + " and experiment." + expt.getId());
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
	public boolean allowDelete() {
		return false;
	}


}
