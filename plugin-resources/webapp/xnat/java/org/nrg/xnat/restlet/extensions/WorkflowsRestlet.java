package org.nrg.xnat.restlet.extensions;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatSubjectassessordata;
import org.nrg.xdat.turbine.utils.PopulateItem;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTable;
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
/**
 * Uses:
 * /data/services/workflows/pipeline_name?project=PROJECT_ID&experiment=PROJECT_LABEL&tatus=Running&display=Latest
 * /data/services/workflows/pipeline_name?experiment=XNAT_ID
 * /data/services/workflows/pipeline_name?experiment=XNAT_ID&param_NAME=VALUE_STR
 * To get a list of all pipelines which have completed for a project with a given parameter
* /data/services/workflows/pipeline_name?project=PROJECT_ID&status=Complete&param_functionalseries=BOLD_MOTOR1&format=csv

 * TODO: /data/services/workflows?subject=XNAT_ID
 * TODO: /data/services/workflows/pipeline_name upload a list file of XNAT session ids
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
	String[] pipelineList = null;
	String latest_by_param = null;
	public WorkflowsRestlet(Context context, Request request, Response response) {
        super(context, request, response);
		pipeline_name= (String)getParameter(request,"PIPELINE_NAME");
		project_id = this.getQueryVariable("project");
		status = this.getQueryVariable("status");
		display = this.getQueryVariable("display");
		form_params = this.getQueryVariablesAsMap();
		xnat_id = this.getQueryVariable("experiment");
		latest_by_param = this.getQueryVariable("latest_by_param");
		if (pipeline_name != null)
			pipelineList = pipeline_name.split(",");
		if (display == null) 
			display = DISPLAY_LATEST;
		this.getVariants().add(new Variant(MediaType.APPLICATION_EXCEL));
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
			   if (hasWorkFlowParamsFilter()) {
				   String query = "select s.label,e.label,w.id,w.externalid,w.pipeline_name, w.launch_time,w.jobid,w.status,w.current_step_launch_time, w.current_step_id from wrk_workflowdata w ";
				   query +=	" LEFT JOIN xnat_experimentdata e ON e.id=w.id";
				   query +=	" LEFT JOIN xnat_subjectassessordata sa ON sa.id=e.id";
				   query +=	" LEFT JOIN xnat_subjectdata s ON sa.subject_id=s.id";
				   query +=	" LEFT JOIN wrk_abstractexecutionenvironment wa ON wa.wrk_abstractexecutionenvironment_id = w.executionenvironment_wrk_abstractexecutionenvironment_id ";
				   query += " LEFT JOIN wrk_xnatexecutionenvironment x ON wa.wrk_abstractexecutionenvironment_id = x.wrk_abstractexecutionenvironment_id";
				   query += " LEFT JOIN wrk_xnatexecutionenvironment_parameter xp ON xp.parameters_parameter_wrk_xnatex_wrk_abstractexecutionenvironmen = x.wrk_abstractexecutionenvironment_id";
				   query += " where w.id='" +expt.getId() +"'"; 
				   query += "and " + addPipelineFilter("w");
				   if (status != null) {
					   query += " and status = '" + status + "'"; 
				   }
				   query += addWorkflowParamsConstraints();
				   query += " ORDER BY e.label, launch_time DESC ";
				   if (display.equalsIgnoreCase(DISPLAY_LATEST)) {
					   query += " LIMIT 1 ";
				   }
			       System.out.println(query);
			       
			       
				   mt = overrideVariant(variant);
			       
				   XFTTable table=XFTTable.Execute(query, user.getDBName(), userName);
				   if (table.size()==1 && table.hasMoreRows()) {
					   ArrayList<Hashtable> tableToList = table.toArrayListOfHashtables();
					   Hashtable rowHash = tableToList.get(0);
					   PopulateItem populator = PopulateItem.Populate(rowHash, user, "wrk:workflowdata", true);
					   return representItem(populator.getItem(), mt);
				   }else {
					   Hashtable params = new Hashtable();
					   return representTable(table, mt, params);
				   }
			   }else {
				   org.nrg.xft.search.CriteriaCollection cc = new CriteriaCollection("AND");
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
			   }
	       }catch(Exception e) {
			   _log.error(e);
				this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,
				"Unable to find workflow entry for pipeline " + pipeline_name + " and experiment." + expt.getId());
				return null;
		   }
		}else {
			   String query = "select s.label,e.label,w.id,w.externalid,w.pipeline_name, w.launch_time,w.jobid,w.status,w.current_step_launch_time, w.current_step_id ";
			   boolean hasWorkFlowParamsFilter = hasWorkFlowParamsFilter();
			   boolean hasParamColumns = hasParamColumns();
			   if (hasWorkFlowParamsFilter) {
				   query += ", xp.name, xp.parameter ";
				   query +=	" from wrk_workflowdata w ";
				   query +=	" LEFT JOIN xnat_experimentdata e ON e.id=w.id";
				   query +=	" LEFT JOIN xnat_subjectassessordata sa ON sa.id=e.id";
				   query +=	" LEFT JOIN xnat_subjectdata s ON sa.subject_id=s.id";
				   query +=	" LEFT JOIN wrk_abstractexecutionenvironment wa ON wa.wrk_abstractexecutionenvironment_id = w.executionenvironment_wrk_abstractexecutionenvironment_id ";
				   query += " LEFT JOIN wrk_xnatexecutionenvironment x ON wa.wrk_abstractexecutionenvironment_id = x.wrk_abstractexecutionenvironment_id";
				   query += " LEFT JOIN wrk_xnatexecutionenvironment_parameter xp ON xp.parameters_parameter_wrk_xnatex_wrk_abstractexecutionenvironmen = x.wrk_abstractexecutionenvironment_id";
				   query += " where " +addPipelineFilter("w");
				   if (status != null)
				      query += " and status = '" + status + "'";
				   query += addWorkflowParamsConstraints();
			   }else if (hasParamColumns) {
				   String columns = (String)form_params.get("columns");
				   String[] columnlist = null;
				   if (columns != null) columnlist = columns.split(",");
				   String latest_by_param_index = "";
				   if (columnlist != null) {
					   for (int i=0; i<columnlist.length; i++) {
						   query += ", xp"+i+".parameter as " + columnlist[i].trim() ;
						   if (latest_by_param != null && latest_by_param.equals(columnlist[i])) {
							   latest_by_param_index = ""+i;
						   }
					   }
				   }
				   query +=	" from wrk_workflowdata w ";
				   query +=	" LEFT JOIN xnat_experimentdata e ON e.id=w.id";
				   query +=	" LEFT JOIN xnat_subjectassessordata sa ON sa.id=e.id";
				   query +=	" LEFT JOIN xnat_subjectdata s ON sa.subject_id=s.id";
				   query +=	" LEFT JOIN wrk_abstractexecutionenvironment wa ON wa.wrk_abstractexecutionenvironment_id = w.executionenvironment_wrk_abstractexecutionenvironment_id ";
				   query += " LEFT JOIN wrk_xnatexecutionenvironment x ON wa.wrk_abstractexecutionenvironment_id = x.wrk_abstractexecutionenvironment_id";
				   if (columnlist != null) {
					   for (int i=0; i<columnlist.length; i++) {
						   query += " LEFT JOIN wrk_xnatexecutionenvironment_parameter xp"+i+" ON xp"+i+".parameters_parameter_wrk_xnatex_wrk_abstractexecutionenvironmen = x.wrk_abstractexecutionenvironment_id";
					   }
				   }
				   query += " where " +addPipelineFilter("w");
				   if (status != null)
				      query += " and status = '" + status + "'";
				   if (columnlist != null) {
					   for (int i=0; i<columnlist.length; i++) {
						   query += " and xp"+i+".name='"+columnlist[i].trim()+"'";
					   }
				   }
				   if (latest_by_param != null) {
					   query += " and launch_time = ( " ;
					   query += "select max(launch_time) from wrk_workflowdata w1 ";
					   query += " LEFT JOIN wrk_abstractexecutionenvironment wa1 ON wa1.wrk_abstractexecutionenvironment_id = w1.executionenvironment_wrk_abstractexecutionenvironment_id";
					   query += " LEFT JOIN wrk_xnatexecutionenvironment x1 ON wa1.wrk_abstractexecutionenvironment_id = x1.wrk_abstractexecutionenvironment_id";
					   query += " LEFT JOIN wrk_xnatexecutionenvironment_parameter xp100 ON xp100.parameters_parameter_wrk_xnatex_wrk_abstractexecutionenvironmen = x1.wrk_abstractexecutionenvironment_id";
					   query += " where w.id=w1.id and " + addPipelineFilter("w1") +" and xp100.name=xp"+ latest_by_param_index +".name and xp100.parameter=xp" + latest_by_param_index + ".parameter group by w1.id, xp100.name, xp100.parameter";
					   query += " ) ";
				   }else if (display.equals(DISPLAY_LATEST)) {
					   query += " and launch_time = ( " ;
					   query += "select max(launch_time) from wrk_workflowdata w1 ";
					   query += " where w.id=w1.id and " + addPipelineFilter("w1") + " group by w1.id";
					   query += " ) ";
				   }
			   }else  {
				   query +=	" from wrk_workflowdata w ";
				   query +=	" LEFT JOIN xnat_experimentdata e ON e.id=w.id";
				   query +=	" LEFT JOIN xnat_subjectassessordata sa ON sa.id=e.id";
				   query +=	" LEFT JOIN xnat_subjectdata s ON sa.subject_id=s.id";
				   query += " where " +addPipelineFilter("w");
				   query += " and launch_time = ( " ;
				   query += "select max(launch_time) from wrk_workflowdata w1 ";
				   query += " where w.id=w1.id " + addPipelineFilter("w1") +"  group by w1.id";
				   query += " ) ";

				   if (status != null)
				      query += " and status = '" + status + "'";
			   }
			   if (project_id!=null) {
				   query += " and externalid = '" + project_id + "'";
			   }
			   query += " ORDER BY w.id, launch_time DESC ";

			   System.out.println(query);
		       
		       
			   mt = overrideVariant(variant);
	        try {
				XFTTable table=XFTTable.Execute(query, user.getDBName(), userName);
        		Hashtable<String,Object> params=new Hashtable<String,Object>();
            	params.put("title", "All " + status + " workflows");
            	return representTable(table, mt, params);
	        }catch(Exception e) {
				   logger.error(e);
					this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,
					"Unable to find workflow entry with pipeline_name " + pipeline_name);
					return null;
	        }
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
				cc.addClause("wrk:workflowData.executionEnvironment.parameters.parameter.name", wrkflow_paramName);
				cc.addClause("wrk:workflowData.executionEnvironment.parameters.parameter",wrkflow_paramValue);
			}
		}
		}

	}

	private String addWorkflowParamsConstraints() {
		String subQuery = " ";
		if (form_params.size()>0) {
		for(String s:form_params.keySet()){
			if (s.startsWith("param_")) {
				String wrkflow_paramName = s.substring(6);
				String wrkflow_paramValue = (String)form_params.get(s);
				subQuery += " and xp.name='" + wrkflow_paramName +"' ";
				if (wrkflow_paramValue != null)
					subQuery += " and xp.parameter='" + wrkflow_paramValue +"' ";
			}
		}
		}
		return subQuery;
	}

	
	private boolean hasWorkFlowParamsFilter() {
		boolean has = false;
		if (form_params.size()>0) {
			for(String s:form_params.keySet()){
				if (s.startsWith("param_")) {
					has = true;
					break;
				}
			}
			}
		  
		return has;
	}

	private boolean hasParamColumns() {
		boolean has = false;
		if (form_params.size()>0) {
			if (form_params.get("columns")!=null) {
				has = true;
			}
		}
		return has;
	}

	private String addPipelineFilter(String prefix) {
		String query ="";
		if (pipelineList != null && pipelineList.length > 0) {
			   query += " (";
			   for (int i=0; i< pipelineList.length; i++) {
			     query += " " + prefix + ".pipeline_name like '%"+ pipelineList[i]+"%' ";
			   if (pipelineList.length > 1 && i < (pipelineList.length -1)) query += " or ";
		     }
			   query += " ) ";
		   }
		return query;
	}
	
}
