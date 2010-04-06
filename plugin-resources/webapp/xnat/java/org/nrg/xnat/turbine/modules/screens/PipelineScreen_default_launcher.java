// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.turbine.modules.screens;

import java.util.ArrayList;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.pipeline.xmlbeans.ParameterData;
import org.nrg.pipeline.xmlbeans.ParameterData.Values;
import org.nrg.pipeline.xmlbeans.ParametersDocument.Parameters;
import org.nrg.xdat.om.ArcPipelinedataI;
import org.nrg.xdat.om.ArcPipelineparameterdata;
import org.nrg.xdat.om.ArcProject;
import org.nrg.xdat.om.ArcProjectPipeline;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.turbine.modules.screens.SecureReport;
import org.nrg.xft.XFTItem;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

public class PipelineScreen_default_launcher extends SecureReport {
    public static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PipelineScreen_launch_pipeline.class);

    public void finalProcessing(RunData data, Context context) {
    	try {
	        String projectId = data.getParameters().get("project");
	        String pipelinePath = data.getParameters().get("pipeline");
	        String schemaType = data.getParameters().get("schema_type");
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
    
    private void setParameters(ArcPipelinedataI arcPipeline, Context context) throws Exception {
    	ArrayList<ArcPipelineparameterdata> pipelineParameters = arcPipeline.getParameters_parameter();
    	
    	Parameters parameters = Parameters.Factory.newInstance();
		ParameterData param = null;
		
    	for (int i = 0; i < pipelineParameters.size(); i++) {
    		ArcPipelineparameterdata pipelineParam = pipelineParameters.get(i);
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
