/*
 * org.nrg.xnat.turbine.modules.actions.ProjectDownloadAction
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.turbine.modules.actions;

import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.DBPoolException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProjectDownloadAction extends SecureAction {
	static org.apache.log4j.Logger logger = Logger.getLogger(ProjectDownloadAction.class);

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
        
        if(!retrieveAllTags(TurbineUtils.getUser(data)).contains(projectId)){
        	Exception e=new Exception("Unknown project: "+ projectId);
        	logger.error("",e);
        	this.error(e, data);
        	return;
        }
        
        String query = "SELECT DISTINCT isd.id FROM xnat_imageSessionData isd LEFT JOIN xnat_experimentData expt ON isd.id=expt.id LEFT JOIN xnat_experimentData_meta_data meta ON expt.experimentData_info=meta.meta_data_id LEFT JOIN xnat_experimentData_share proj ON expt.id=proj.sharing_share_xnat_experimentda_id WHERE (proj.project='" + projectId + "' OR expt.project='" + projectId + "') AND (meta.status='active' OR meta.status='locked');";
        
        XFTTable t = XFTTable.Execute(query, TurbineUtils.getUser(data).getDBName(), TurbineUtils.getUser(data).getUsername());
        ArrayList al =t.convertColumnToArrayList("id");
        for (int i=0;i<al.size();i++){
            data.getParameters().append("sessions", (String)al.get(i));
        }
        
        data.setScreenTemplate("XDATScreen_download_sessions.vm");
    }

	
	public List<String> retrieveAllTags(final XDATUser user){
		try {
			return (List<String>)(XFTTable.Execute("SELECT DISTINCT id from xnat_projectData;", user.getDBName(), user.getLogin()).convertColumnToArrayList("id"));
		} catch (SQLException e) {
			logger.error("",e);
		} catch (DBPoolException e) {
			logger.error("",e);
		}
		
		return Lists.newArrayList();
	}
}
