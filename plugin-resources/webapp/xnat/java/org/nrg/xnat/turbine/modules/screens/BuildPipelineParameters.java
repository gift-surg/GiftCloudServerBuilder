/* 
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 * 	
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

*/

package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.turbine.modules.screens.SecureReport;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFT;

public class BuildPipelineParameters extends SecureReport
{

    public void preProcessing(RunData data, Context context)
    {
        TurbineUtils.InstanciatePassedItemForScreenUse(data,context);
    }
    
    
    /**
     * Place all the data object in the context
     * for use in the template.
     */
    public void finalProcessing(RunData data, Context context) 
    {
        if (context.get("om")==null) {
            data.setScreenTemplate("Error.vm");
            return; 
        }
        String pipelineName = data.getParameters().get("pipelineName");
        if (pipelineName == null) {
            data.setScreenTemplate("Error.vm");
            return;
        }
        
        XnatMrsessiondata mr = (XnatMrsessiondata)context.get("om");
        
        try {
            /*LinkedHashMap parametersHash = BuildSpecification.GetInstance().getResolvedParametersForPipeline(pipelineName,mr);
            context.put("parametersHash",parametersHash);
            context.put("pipelineName",pipelineName);
            context.put("search_element",mr.getSchemaElementName());
            context.put("search_field",mr.getSchemaElementName()+".ID");
            context.put("search_value",mr.getId());*/
            return;
        }catch(Exception e) {
            String errorString = "<img src=\"/cnda1/images/error.gif\"> Error in the Build Spec file document for the pipeline " + context.get("pipelineName") ;
            errorString += "<p>Please contact the <a href=\"mailto:"+XFT.GetAdminEmail()+"?subject=Error in Build Spec file for " + mr.getSessionType() + " pipeline " + pipelineName + "\">CNL techdesk</a> to resolve the error.</p>";
            data.setMessage(errorString);
            data.getParameters().add("exception",e.getMessage());
            data.setScreenTemplate("Error.vm");
        }
    }
    
    
    
 
    

}
