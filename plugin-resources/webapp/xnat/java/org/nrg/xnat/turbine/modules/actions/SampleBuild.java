/* 
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 * 	
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

*/

package org.nrg.xnat.turbine.modules.actions;

import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.pipeline.XnatPipelineLauncher;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

public class SampleBuild extends SecureAction
{
    static org.apache.log4j.Logger logger = Logger.getLogger(SampleBuild.class);
    
    public void doPerform(RunData data, Context context){
        try {
            ItemI data_item = TurbineUtils.GetItemBySearch(data);
            XnatMrsessiondata mr = new XnatMrsessiondata(data_item);
            String pipelineName = data.getParameters().get("pipeline");
            if (pipelineName != null) {
                if (!pipelineName.endsWith(".xml")) {
                    pipelineName += ".xml";
                }
                String xnat = data.getParameters().get("xnat");
                XnatPipelineLauncher pipelineLauncher = new XnatPipelineLauncher(data,context);
                pipelineLauncher.setAdmin_email(AdminUtils.getAdminEmailId());
                pipelineLauncher.setAlwaysEmailAdmin(ArcSpecManager.GetInstance().getEmailspecifications_pipeline());
                pipelineLauncher.setId(mr.getId());
                pipelineLauncher.setDataType("xnat:mrSessionData");
                pipelineLauncher.setPipelineName(pipelineName);
                pipelineLauncher.setParameter("sessionId",mr.getId());
                pipelineLauncher.setParameter("xnat",xnat);
                String emailsStr = TurbineUtils.getUser(data).getEmail() + "," + data.getParameters().get("emailField");
                String[] emails = emailsStr.trim().split(",");
                for (int i = 0 ; i < emails.length; i++)
                     pipelineLauncher.notify(emails[i]);
                boolean success = pipelineLauncher.launch();
                if (success) {
                    data.setMessage("Build was launched successfully");
                    data.setScreenTemplate("ClosePage.vm");
                }else {
                    data.setMessage("Build process failed");
                    data.setScreenTemplate("Error.vm");
                }
            }
        }catch(Exception e){
            logger.info(e.getMessage(),e);
        }
    }
   

}
