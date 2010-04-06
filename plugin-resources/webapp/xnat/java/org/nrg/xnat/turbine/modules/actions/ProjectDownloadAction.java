//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Nov 12, 2007
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import java.util.ArrayList;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFTTable;

public class ProjectDownloadAction extends SecureAction {

    /* (non-Javadoc)
     * @see org.apache.turbine.modules.actions.VelocitySecureAction#doPerform(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    @Override
    public void doPerform(RunData data, Context context) throws Exception {
        String projectId = (String) TurbineUtils.GetPassedParameter("project", data);
        
        if(projectId.contains("\\")|| projectId.contains("'"))
        {
        	data.setMessage("Illegal Character");
        	data.setScreenTemplate("Index.vm");
        	return;
        }
        String query = "SELECT DISTINCT isd.id FROM xnat_imageSessionData isd LEFT JOIN xnat_experimentData expt ON isd.id=expt.id LEFT JOIN xnat_experimentData_share proj ON expt.id=proj.sharing_share_xnat_experimentda_id WHERE proj.project='" + projectId + "' OR expt.project='" + projectId + "';";
        
        XFTTable t = XFTTable.Execute(query, TurbineUtils.getUser(data).getDBName(), TurbineUtils.getUser(data).getUsername());
        ArrayList al =t.convertColumnToArrayList("id");
        for (int i=0;i<al.size();i++){
            data.getParameters().append("sessions", (String)al.get(i));
        }
        
        data.setScreenTemplate("XDATScreen_download_sessions.vm");
    }

}
