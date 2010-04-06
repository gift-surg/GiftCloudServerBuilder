/* 
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 * 	
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

*/

package org.nrg.xnat.turbine.modules.screens;

import java.io.File;
import java.util.ArrayList;

import org.apache.turbine.util.RunData;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.nrg.pipeline.PipelineRepositoryManager;
import org.nrg.pipeline.utils.PipelineUtils;
import org.nrg.xdat.om.ArcPipelineparameterdata;
import org.nrg.xdat.om.ArcProject;
import org.nrg.xdat.om.ArcProjectDescendantPipeline;
import org.nrg.xdat.om.ArcProjectPipeline;
import org.nrg.xdat.om.PipePipelinedetails;
import org.nrg.xdat.om.PipePipelinedetailsParameter;
import org.nrg.xdat.om.PipePipelinerepository;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.turbine.modules.screens.SecureReport;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

public class PipelineScreen_add_project_pipeline extends SecureReport {
    public static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PipelineScreen_add_project_pipeline.class);

    public void finalProcessing(RunData data, Context context) {
    	
    }
    
    public void doBuildTemplate(RunData data, Context context) 	{
	    logger.debug("BEGIN SECURE REPORT :" + this.getClass().getName());
	    String projectId = data.getParameters().get("project");
	    String pipelinePath = data.getParameters().get("pipeline_path");
	    String dataType = data.getParameters().get("dataType");
	    boolean edit = data.getParameters().getBoolean("edit");
	    String templateFile = null;
	    context.put("pipeline_path", pipelinePath);
	    context.put("dataType", dataType);
	    try {
	    	context.put("edit", edit);
	    	if (edit) {
				ArcProject arcProject = ArcSpecManager.GetInstance().getProjectArc(projectId);
	    		if (dataType.equals(XnatProjectdata.SCHEMA_ELEMENT_NAME)) { //Its a project level pipeline
					ArcProjectPipeline newPipeline = arcProject.getPipelineEltByPath(pipelinePath);
					templateFile = newPipeline.getCustomwebpage();
					context.put("newpipeline", newPipeline);
					context.put("isAutoArchive",isAutoArchive(newPipeline));
					redirect(data, templateFile);
	    		}else {
					ArcProjectDescendantPipeline newPipeline = arcProject.getPipelineForDescendantEltByPath(dataType, pipelinePath);
					templateFile = newPipeline.getCustomwebpage();
					context.put("newpipeline", newPipeline);
					context.put("isAutoArchive",isAutoArchive(newPipeline));
					redirect(data, templateFile);
	    		}
	    	}else {
				    //Get the pipeline identified by the pipeline_path
				    //Get the ArcProject element identified by the projectId
				    //Set the pipeline for the data-type and send it to the screen for the user to add parameters
				    PipePipelinerepository pipelineRepository = PipelineRepositoryManager.GetInstance();
				    PipePipelinedetails pipeline = pipelineRepository.getPipeline(pipelinePath);
					if (dataType.equals(XnatProjectdata.SCHEMA_ELEMENT_NAME)) { //Its a project level pipeline
						ArcProjectPipeline newPipeline = new ArcProjectPipeline();
						String pipelineName = getName(pipeline.getPath());
						newPipeline.setStepid(pipelineName);
						newPipeline.setDisplaytext(pipelineName);
						newPipeline.setLocation(pipeline.getPath());
						newPipeline.setName(pipelineName);
						newPipeline.setDescription(pipeline.getDescription());
						ArrayList<PipePipelinedetailsParameter> parameter = pipeline.getParameters_parameter();
						for (int i = 0; i < parameter.size(); i++) {
							ArcPipelineparameterdata arcParam = extractArcPipelineParameter(parameter.get(i));
							newPipeline.setParameters_parameter(arcParam.getItem());
						}
						if (pipeline.getCustomwebpage() != null) {
							newPipeline.setCustomwebpage(pipeline.getCustomwebpage());
							templateFile = newPipeline.getCustomwebpage();
						}
						context.put("newpipeline", newPipeline);
						context.put("isAutoArchive",isAutoArchive(newPipeline));
						redirect(data, templateFile);
					}else {
						ArcProjectDescendantPipeline newPipeline = new ArcProjectDescendantPipeline();
						String pipelineName = getName(pipeline.getPath());
						newPipeline.setStepid(pipelineName);
						newPipeline.setDisplaytext(pipelineName);
						newPipeline.setLocation(pipeline.getPath());
						newPipeline.setName(pipelineName);
						newPipeline.setDescription(pipeline.getDescription());
						ArrayList<PipePipelinedetailsParameter> parameter = pipeline.getParameters_parameter();
						for (int i = 0; i < parameter.size(); i++) {
							ArcPipelineparameterdata arcParam = extractArcPipelineParameter(parameter.get(i));
							newPipeline.setParameters_parameter(arcParam.getItem());
						}
						if (pipeline.getCustomwebpage() != null) {
							newPipeline.setCustomwebpage(pipeline.getCustomwebpage());
							templateFile = newPipeline.getCustomwebpage();
						}
						context.put("newpipeline", newPipeline);
						context.put("isAutoArchive",isAutoArchive(newPipeline));
						redirect(data, templateFile);
					}
	    	}
	    }catch(Exception e) {
	    	logger.error("",e);
	    }
    }
    
    private boolean isAutoArchive(ArcProjectDescendantPipeline newPipeline) {
    	boolean rtn = false;
    	if (newPipeline == null ) return rtn;
    	if (newPipeline.getStepid()!=null && newPipeline.getStepid().equals(PipelineUtils.AUTO_ARCHIVE))
    		rtn = true;
    	return rtn;
    }
    
    private boolean isAutoArchive(ArcProjectPipeline newPipeline) {
    	boolean rtn = false;
    	if (newPipeline == null ) return rtn;
    	if (newPipeline.getStepid()!=null && newPipeline.getStepid().equals(PipelineUtils.AUTO_ARCHIVE))
    		rtn = true;
    	return rtn;
    }

    
    private String getName(String path) {
    	String rtn = path;
    	//int index = path.lastIndexOf(File.separator);
    	int index = path.lastIndexOf("/");
    	if (index != -1) {
    		rtn = path.substring(index + 1);
        	index = rtn.lastIndexOf(".xml");
        	if (index != -1) {
        		rtn = rtn.substring(0, index);
        	}
    	}
    	return rtn;
    }

    private void redirect(RunData data, String templateFile) {
		if (templateFile != null) {
			String templateName = "";
			int index = templateFile.indexOf(".vm");
			if (index != -1) {
				String prefix = templateFile.substring(0, index); 
				templateName += prefix +"_add.vm"; 
			}
			logger.debug("PipelineScreen_add_pipeline looking for: " + templateName);
			if (Velocity.templateExists("/screens/" + templateName)) {
				data.setScreenTemplate(templateName );
			}
		}
    }
    
    
    private ArcPipelineparameterdata extractArcPipelineParameter(PipePipelinedetailsParameter pipeParameter) {
    	ArcPipelineparameterdata rtn = new ArcPipelineparameterdata();
    	rtn.setName(pipeParameter.getName());
    	rtn.setDescription(pipeParameter.getDescription());
    	String schemaLink = pipeParameter.getValues_schemalink();
    	String csvValue = pipeParameter.getValues_csvvalues();
    	if (schemaLink != null) {
    		rtn.setSchemalink(schemaLink);
    	}else {
    		rtn.setCsvvalues(csvValue);
    	}
    	return rtn;
    }
    
}
