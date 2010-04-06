/* 
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 * 	
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

*/

package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.pipeline.PipelineRepositoryManager;
import org.nrg.pipeline.utils.FileUtils;
import org.nrg.pipeline.xmlbeans.PipelineDocument;
import org.nrg.pipeline.xmlbeans.PipelineData.Documentation.InputParameters;
import org.nrg.pipeline.xmlbeans.PipelineData.Documentation.InputParameters.Parameter;
import org.nrg.pipeline.xmlbeans.PipelineData.Documentation.InputParameters.Parameter.Values;
import org.nrg.xdat.om.PipePipelinedetails;
import org.nrg.xdat.om.PipePipelinedetailsParameter;
import org.nrg.xdat.om.PipePipelinerepository;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.screens.AdminEditScreenA;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFT;

public class PipelineScreen_set_site_parameters extends AdminEditScreenA{

	static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PipelineScreen_set_site_parameters.class);
	
	public String getElementName() {
		return PipePipelinedetails.SCHEMA_ELEMENT_NAME;
	}
	
	public void finalProcessing(RunData data, Context context) {
		
	}
	
	public void doBuildTemplate(RunData data, Context context)      {
		try {
			//A partially filled pipeline element
			PipePipelinedetails pipelineDetails = (PipePipelinedetails)context.get("pipeline");
			context.remove("pipeline");
			String pathToPipeline = pipelineDetails.getPath();
			PipelineDocument pipelineDoc = FileUtils.GetDocument(pathToPipeline);
			pipelineDetails.setDescription(pipelineDoc.getPipeline().getDescription());
			if (pipelineDoc.getPipeline().isSetDocumentation()) {
				if (pipelineDoc.getPipeline().getDocumentation().isSetInputParameters()) {
					InputParameters parameters = pipelineDoc.getPipeline().getDocumentation().getInputParameters();
					Parameter[] params = parameters.getParameterArray();
					for (int i = 0; i < params.length; i++) {
						Parameter aParam = params[i];
						if (aParam.isSetValues()) {
							Values values = aParam.getValues();
							PipePipelinedetailsParameter pipelineParameter = new PipePipelinedetailsParameter();
							pipelineParameter.setName(aParam.getName());
							pipelineParameter.setDescription(aParam.getDescription());
							if (values.isSetCsv()) {
								pipelineParameter.setValues_csvvalues(values.getCsv());
							}else {
								pipelineParameter.setValues_schemalink(values.getSchemalink());
							}
							pipelineDetails.setParameters_parameter(pipelineParameter);
						}
					}
				}
				context.put("item", pipelineDetails);
			}else {
				XDATUser user = TurbineUtils.getUser(data);
				try {
            		PipePipelinerepository pipelineRepository = PipelineRepositoryManager.GetInstance();
            		pipelineRepository.setPipeline(pipelineDetails);
            		pipelineRepository.save(user, false, true);
            		PipelineRepositoryManager.Reset();
    				data.setMessage("The pipeline has been added to the repository");
    				data.setScreenTemplate("ClosePage.vm");
				} catch (Exception e) {
					data.setMessage("The pipeline could not be added to the repository. Please contact " + XFT.GetAdminEmail() );
					data.setScreenTemplate("Error.vm");
            	}
			}
		}catch(Exception e) {
			logger.error(e);
			e.printStackTrace();
			data.setMessage("The pipeline could not be added to the repository. Please contact " + XFT.GetAdminEmail() );
			data.setScreenTemplate("Error.vm");
		}
	}
	
		
}
