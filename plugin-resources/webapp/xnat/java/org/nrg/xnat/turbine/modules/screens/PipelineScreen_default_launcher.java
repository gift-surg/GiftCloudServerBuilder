// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.turbine.modules.screens;

import java.util.ArrayList;
import java.util.List;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.pipeline.xmlbeans.ParameterData;
import org.nrg.pipeline.xmlbeans.ParameterData.Values;
import org.nrg.pipeline.xmlbeans.ParametersDocument.Parameters;
import org.nrg.xdat.model.ArcPipelinedataI;
import org.nrg.xdat.model.ArcPipelineparameterdataI;
import org.nrg.xdat.om.ArcPipelineparameterdata;
import org.nrg.xdat.om.ArcProject;
import org.nrg.xdat.om.ArcProjectPipeline;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.turbine.modules.screens.SecureReport;
import org.nrg.xft.XFTItem;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

public class PipelineScreen_default_launcher extends DefaultPipelineScreen {
    public static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PipelineScreen_launch_pipeline.class);

    public void finalProcessing(RunData data, Context context) {
    	try {
	        String projectId = ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("project",data));
	        String pipelinePath = ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("pipeline",data));
	        String schemaType = ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("schema_type",data));
	        ArcProject arcProject = ArcSpecManager.GetInstance().getProjectArc(projectId);
	        if (schemaType.equals(XnatProjectdata.SCHEMA_ELEMENT_NAME)) {
	        	ArcProjectPipeline pipelineData = (ArcProjectPipeline)arcProject.getPipelineByPath(pipelinePath);
	        	context.put("pipeline", pipelineData);
	        	setParameters(pipelineData, context);
	        }else {
	        	ArcPipelinedataI pipelineData = arcProject.getPipelineForDescendantByPath(schemaType, pipelinePath);
	        	context.put("pipeline", pipelineData);
	        	setParameters(pipelineData, context);
	        }
    	}catch(Exception e) {
    		e.printStackTrace();
    		logger.debug(e);
    	}
    }
    
}
