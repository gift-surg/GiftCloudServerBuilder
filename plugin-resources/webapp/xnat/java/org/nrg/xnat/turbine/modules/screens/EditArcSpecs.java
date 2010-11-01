//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Aug 7, 2007
 *
 */
package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.ArcArchivespecification;
import org.nrg.xdat.turbine.modules.screens.AdminScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFT;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

public class EditArcSpecs extends AdminScreen {

    /* (non-Javadoc)
     * @see org.apache.turbine.modules.screens.VelocitySecureScreen#doBuildTemplate(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    @Override
    protected void doBuildTemplate(RunData data, Context context) throws Exception {
        ArcArchivespecification arcSpec = ArcSpecManager.GetInstance();
        if (arcSpec==null){
        	arcSpec = new ArcArchivespecification((UserI)TurbineUtils.getUser(data));
        	if (XFT.GetAdminEmail()!=null && !XFT.GetAdminEmail().equals(""))
            	arcSpec.setSiteAdminEmail(XFT.GetAdminEmail());
                
        	if (XFT.GetSiteURL()!=null && !XFT.GetSiteURL().equals(""))
                	arcSpec.setSiteUrl(XFT.GetSiteURL());
                
        	if (XFT.GetAdminEmailHost()!=null && !XFT.GetAdminEmailHost().equals(""))
                	arcSpec.setSmtpHost(XFT.GetAdminEmailHost());
                
        	arcSpec.setEnableNewRegistrations(XFT.GetUserRegistration());
            
            arcSpec.setRequireLogin(XFT.GetRequireLogin());
            if (XFT.GetPipelinePath()!=null && !XFT.GetPipelinePath().equals(""))
            	arcSpec.setProperty("globalPaths/pipelinePath", XFT.GetPipelinePath());
            
            if (XFT.GetArchiveRootPath()!=null && !XFT.GetArchiveRootPath().equals(""))
            	arcSpec.setProperty("globalPaths/archivePath", XFT.GetArchiveRootPath());
            
            if (XFT.GetPrearchivePath()!=null && !XFT.GetPrearchivePath().equals(""))
            	arcSpec.setProperty("globalPaths/prearchivePath", XFT.GetPrearchivePath());
            
            if (XFT.GetCachePath()!=null && !XFT.GetCachePath().equals(""))
            	arcSpec.setProperty("globalPaths/cachePath", XFT.GetCachePath());
            
        }
        context.put("arc", arcSpec);
    }

}
