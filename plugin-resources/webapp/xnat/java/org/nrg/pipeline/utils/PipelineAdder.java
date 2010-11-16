package org.nrg.pipeline.utils;

import org.apache.turbine.util.RunData;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.nrg.pipeline.PipelineRepositoryManager;
import org.nrg.xdat.om.ArcProject;
import org.nrg.xdat.om.ArcProjectDescendantPipeline;
import org.nrg.xdat.om.ArcProjectPipeline;
import org.nrg.xdat.om.PipePipelinedetails;
import org.nrg.xdat.om.PipePipelinerepository;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

public class PipelineAdder {
	
    public static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PipelineAdder.class);

	
    public void prepareScreen(RunData data, Context context) {
	    logger.debug("BEGIN SECURE REPORT :" + this.getClass().getName());
	    String projectId = data.getParameters().get("project");
	    String pipelinePath = data.getParameters().get("pipeline_path");
	    String dataType = data.getParameters().get("dataType");
	    boolean edit = data.getParameters().getBoolean("edit");
	    String templateFile = null;
	    try {
	    	if (edit) {
				ArcProject arcProject = ArcSpecManager.GetInstance().getProjectArc(projectId);
	    		if (dataType.equals(XnatProjectdata.SCHEMA_ELEMENT_NAME)) { //Its a project level pipeline
					ArcProjectPipeline newPipeline = arcProject.getPipelineEltByPath(pipelinePath);
					templateFile = newPipeline.getCustomwebpage();
	    		}else {
					ArcProjectDescendantPipeline newPipeline = arcProject.getPipelineForDescendantEltByPath(dataType, pipelinePath);
					templateFile = newPipeline.getCustomwebpage();
	    		}
	    	}else {
				    //Get the pipeline identified by the pipeline_path
				    //Get the ArcProject element identified by the projectId
				    //Set the pipeline for the data-type and send it to the screen for the user to add parameters
				    PipePipelinerepository pipelineRepository = PipelineRepositoryManager.GetInstance();
				    PipePipelinedetails pipeline = pipelineRepository.getPipeline(pipelinePath);
					if (dataType.equals(XnatProjectdata.SCHEMA_ELEMENT_NAME)) { //Its a project level pipeline
						if (pipeline.getCustomwebpage() != null) {
							templateFile = pipeline.getCustomwebpage();
						}
					}else {
						ArcProjectDescendantPipeline newPipeline = new ArcProjectDescendantPipeline();
						if (pipeline.getCustomwebpage() != null) {
							templateFile = pipeline.getCustomwebpage();
						}
					}
	    	}
	    	redirect(data, templateFile);
	    }catch(Exception e) {
	    	logger.error("",e);
	    }
    	
    }

    private void redirect(RunData data, String templateFile) throws Exception {
		if (templateFile != null) {
			String screenName = "";
			int index = templateFile.indexOf(".vm");
			if (index != -1) {
				String prefix = templateFile.substring(0, index); 
				screenName += prefix +"_add"; 
			}else {
				screenName += templateFile +"_add"; 
			}
			if (!screenName.endsWith(".vm")) screenName+=".vm";
			logger.debug("PipelineAdder looking for: " + screenName);
			if (Velocity.templateExists("/screens/" + screenName)) {
				data.setScreenTemplate(screenName);
			}
		}else {
            String template = data.getParameters().get("template");
            data.setScreenTemplate(template);
		}
    }
    
    

}
