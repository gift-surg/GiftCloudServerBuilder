/*
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 *
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

*/

package org.nrg.xnat.turbine.modules.screens;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.pipeline.PipelineRepositoryManager;
import org.nrg.pipeline.xmlbeans.ParameterData;
import org.nrg.pipeline.xmlbeans.ParameterData.Values;
import org.nrg.pipeline.xmlbeans.ParametersDocument.Parameters;
import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.model.ArcPipelinedataI;
import org.nrg.xdat.model.ArcPipelineparameterdataI;
import org.nrg.xdat.model.ArcProjectDescendantPipelineI;
import org.nrg.xdat.model.PipePipelinedetailsParameterI;
import org.nrg.xdat.om.ArcPipelineparameterdata;
import org.nrg.xdat.om.ArcProject;
import org.nrg.xdat.om.ArcProjectDescendantPipeline;
import org.nrg.xdat.om.PipePipelinedetails;
import org.nrg.xdat.om.PipePipelinedetailsParameter;
import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatDicomseries;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xdat.om.base.BaseWrkWorkflowdata;
import org.nrg.xdat.turbine.modules.screens.SecureReport;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFT;
import org.nrg.xft.XFTItem;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xnat.exceptions.PipelineNotFoundException;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

public abstract class DefaultPipelineScreen extends SecureReport{

	static Logger logger = Logger.getLogger(DefaultPipelineScreen.class);
	String message = null;
	ArrayList workflows;
	String project = null;

	protected Hashtable<String, ArcPipelineparameterdata > projectParameters ;
	String pipelinePath = null;

	 public DefaultPipelineScreen() {
	        workflows = new ArrayList<WrkWorkflowdata>();
	        projectParameters = new Hashtable<String, ArcPipelineparameterdata >();
	    }

	  public abstract void finalProcessing(RunData data, Context context);

		protected void setHasDicomFiles(XnatMrsessiondata mr, String mprageScanType, Context context) {
			boolean rtn = false;
			String[] types = mprageScanType.split(",");
			for (int j =0; j <types.length; j++) {
			ArrayList<XnatImagescandata> scans = mr.getScansByType(types[j].trim());
			if (scans != null && scans.size() > 0 ) {
				List files = scans.get(0).getFile();
				if (files.size() > 0) {
					for (int i =0; i < files.size(); i++) {
						XnatAbstractresource absFile = (XnatAbstractresource) files.get(i);
						if (absFile instanceof  XnatDicomseries ) {
							rtn = true;
						}else if (absFile instanceof  XnatResourcecatalog){
							XnatResourcecatalog rsccat = (XnatResourcecatalog) absFile;
							if (rsccat.getContent().endsWith("RAW")) {
								if (rsccat.getFormat().equals("DICOM"))
								    rtn = true;
								break;
							}
						}

					}
				}
			}

			}
			context.put("isDicom", rtn?"1":"0");

		}

		protected void setHasFreesurfer(XnatMrsessiondata mr,  Context context) {
			String project = mr.getProject();
			int hasFreesurfer = 0;
			ArcProject arcProject = ArcSpecManager.GetInstance().getProjectArc(project);
			List<ArcProjectDescendantPipelineI> descPipelines = arcProject.getPipelinesForDescendant(mr.SCHEMA_ELEMENT_NAME);
			for (int i = 0; i < descPipelines.size(); i++) {
				ArcProjectDescendantPipeline aPipeline = (ArcProjectDescendantPipeline)descPipelines.get(i);
				if (aPipeline.getLocation().endsWith(File.separator+"StdFreeSurferBuild.xml")); {
					hasFreesurfer = 1;
					break;
				}
			}
			context.put("freesurfer", hasFreesurfer);

		}


	  protected void setWorkflows(RunData data, Context context) {
	        String projectId = (String)context.get("project");
	        try {
	        	org.nrg.xft.search.CriteriaCollection cc = new CriteriaCollection("AND");
	            cc.addClause("wrk:workflowData.ID",item.getProperty("ID"));
	            if (projectId != null) cc.addClause("wrk:workflowData.ExternalID",projectId);
	            org.nrg.xft.collections.ItemCollection items = org.nrg.xft.search.ItemSearch.GetItems(cc,TurbineUtils.getUser(data),false);
	            //Sort by Launch Time
	            ArrayList workitems = items.getItems("wrk:workflowData.launch_time","DESC");
	            Iterator iter = workitems.iterator();
	            while (iter.hasNext())
	            {
	                WrkWorkflowdata vrc = new WrkWorkflowdata((XFTItem)iter.next());
	                workflows.add(vrc);
	            }
	            context.put("workflows",workflows);
	        }catch(Exception e) {
	        	logger.debug(e);
	        }
	    }

	   public void preProcessing(RunData data, Context context)   {
	    }


	  public void doBuildTemplate(RunData data, Context context)	{
	       // preserveVariables(data,context);
		    logger.debug("BEGIN SECURE REPORT :" + this.getClass().getName());
		    preProcessing(data,context);
	        item = TurbineUtils.getDataItem(data);
		    if (item== null)		{
				try {
					ItemI temp = TurbineUtils.GetItemBySearch(data,preLoad());
				    item = temp;
				} catch (IllegalAccessException e1) {
	                logger.error(e1);
				    data.setMessage(e1.getMessage());
					noItemError(data,context);
					return;
				} catch (Exception e1) {
	                logger.error(e1);
	                data.setMessage(e1.getMessage());
	                data.setScreenTemplate("Error.vm");
	                noItemError(data,context);
	                return;
				}
			}
			if (item == null)		{
				data.setMessage("Error: No item found.");
				noItemError(data,context);
			}else{
				try {
					if(XFT.VERBOSE)System.out.println("Creating report: " + getClass());;
				    context.put("item",item.getItem());
				    if(XFT.VERBOSE)System.out.println("Loaded item object (org.nrg.xft.ItemI) as context parameter 'item'.");
				    context.put("user",TurbineUtils.getUser(data));
				    if(XFT.VERBOSE)System.out.println("Loaded user object (org.nrg.xdat.security.XDATUser) as context parameter 'user'.");
	            	context.put("element",org.nrg.xdat.schema.SchemaElement.GetElement(item.getXSIType()));
	            	context.put("search_element",((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("search_element",data)));
	            	context.put("search_field",((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("search_field",data)));
	            	context.put("search_value",((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("search_value",data)));
	            	project = ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("project",data));
	            	pipelinePath = (String)context.get("pipelinePath");

	            	context.put("project",project);

	            	om = BaseElement.GetGeneratedItem(item);
	     
	            	context.put("om",om);
	            	 setWorkflows(data,context);
	            	 setParameters(pipelinePath);
	            	if (message != null) data.setMessage(message);
					 finalProcessing(data,context);
				} catch (Exception e) {
					data.setMessage(e.getMessage());
					logger.error("",e);
				}
			}
		    logger.debug("END SECURE REPORT :" + this.getClass().getName());
		}



	protected boolean isAnyQueuedOrRunning(ArrayList<WrkWorkflowdata> workflows) {
        boolean rtn = false;
        try {
            for (int i = 0; i <workflows.size(); i++) {
                WrkWorkflowdata wrkFlow = workflows.get(i);
                if (wrkFlow.getStatus().toUpperCase().equals(BaseWrkWorkflowdata.QUEUED) ||wrkFlow.getStatus().toUpperCase().equals(BaseWrkWorkflowdata.RUNNING)) {
                    rtn = true;
                    break;
                }
            }
        }catch(IndexOutOfBoundsException aoe){logger.debug(aoe);}
        return rtn;
    }

    protected boolean hasBeenCompletedInThePast(String pipelinePath,ArrayList<WrkWorkflowdata> workflows ) {
    	boolean rtn = false;
             for (int i = 0; i <workflows.size(); i++) {
                 WrkWorkflowdata wrkFlow = workflows.get(i);
                 String matchPipelineName = wrkFlow.getPipelineName();
                 if (matchPipelineName.equals(pipelinePath) || pipelinePath.contains(matchPipelineName)) {
                	 if (wrkFlow.getStatus().equalsIgnoreCase(BaseWrkWorkflowdata.COMPLETE)) {
	                     rtn = true;
	                     message = "This pipeline has been completed in the past. Relaunching the pipeline may result in loss of data. Are you sure you want to proceed?";
	                     break;
                	 }
                 }
             }
         return rtn;

    }

    protected ArcPipelineparameterdata  getProjectPipelineSetting(String parameterName) throws Exception {
    	return projectParameters.get(parameterName);
    }

    protected ArcPipelineparameterdata getParameter(ArcProject arcProject, String parameterName) throws PipelineNotFoundException {
    	ArcPipelineparameterdata rtn = null;
    	ArcPipelinedataI pipelineData = arcProject.getPipelineByPath(pipelinePath);
    	List<ArcPipelineparameterdataI> params = pipelineData.getParameters_parameter();
        for (int i = 0; i < params.size(); i++) {
        	ArcPipelineparameterdata aParam = (ArcPipelineparameterdata)params.get(i);
        	if (aParam.getName().equals(parameterName)) {
        		rtn = aParam;
        		break;
        	}
        }
        return rtn;
    }

    protected void setParameters(String pipeline) throws PipelineNotFoundException {
        ArcProject arcProject = ArcSpecManager.GetInstance().getProjectArc(project);
        ArcPipelinedataI pipelineData = null;
        if (arcProject == null) { //Project pipeline hasnt been set
        	PipePipelinedetails pipelineDetails = PipelineRepositoryManager.GetInstance().getPipeline(pipeline);
        	List<PipePipelinedetailsParameterI> params = pipelineDetails.getParameters_parameter();
	        for (int i = 0; i < params.size(); i++) {
	        	ArcPipelineparameterdata aParam = (ArcPipelineparameterdata)PipelineRepositoryManager.GetInstance().convertToArcPipelineParameter((PipePipelinedetailsParameter)params.get(i));
	            projectParameters.put(aParam.getName(),aParam);
	        }
        }else {
	        if (om.getXSIType().equals(XnatProjectdata.SCHEMA_ELEMENT_NAME)) {
	        	pipelineData = arcProject.getPipelineByPath(pipeline);
	        }else {
	        	 pipelineData = arcProject.getPipelineForDescendantByPath(om.getXSIType(), pipeline);
	        }
	    	List<ArcPipelineparameterdataI> params = pipelineData.getParameters_parameter();
	        for (int i = 0; i < params.size(); i++) {
	        	ArcPipelineparameterdata aParam = (ArcPipelineparameterdata)params.get(i);
	            projectParameters.put(aParam.getName(),aParam);
	        }
        }
    }

    protected void setParameters(ArcPipelinedataI arcPipeline, Context context) throws Exception {
    	List<ArcPipelineparameterdataI> pipelineParameters = arcPipeline.getParameters_parameter();
    	
    	Parameters parameters = Parameters.Factory.newInstance();
		ParameterData param = null;
		
    	for (int i = 0; i < pipelineParameters.size(); i++) {
    		ArcPipelineparameterdataI pipelineParam = pipelineParameters.get(i);
    		String schemaLink = pipelineParam.getSchemalink();
    		if (schemaLink != null) {
    			Object o = om.getItem().getProperty(schemaLink, true);
    			if (o != null ) {
	    			try {
	        			ArrayList<XFTItem>  matches = (ArrayList<XFTItem>) o;
	        			if (matches !=  null) {
	        		    	param = parameters.addNewParameter();
	        		    	param.setName(pipelineParam.getName());
	        		    	Values values = param.addNewValues();
	        				if (matches.size() == 1) {
		        		    	values.setUnique(""+matches.get(0));
		        			}else { 
			    				for (int j = 0; j < matches.size(); j++) {
			    					values.addList(""+matches.get(j));
			        			}
		        			}
	        			}
	    			}catch(ClassCastException  cce) {
        		    	param = parameters.addNewParameter();
        		    	param.setName(pipelineParam.getName());
        		    	Values values = param.addNewValues();
        		    	values.setUnique(""+o);
	    			}
    			}
    		}else {
    			String pValues = pipelineParam.getCsvvalues();
    			String[] pValuesSplit = pValues.split(",");
		    	param = parameters.addNewParameter();
		    	param.setName(pipelineParam.getName());
		    	Values values = param.addNewValues();
		    	if (pValuesSplit.length == 1) {
		    		values.setUnique(pValuesSplit[0]);
		    	}else 
	    			for (int j = 0; j < pValuesSplit.length; j++) {
	    				values.addList(pValuesSplit[j]);
	    			}
    		}
    	}
    	context.put("parameters",parameters );
    }


}
