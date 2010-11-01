//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Aug 7, 2007
 *
 */
package org.nrg.xnat.turbine.modules.screens;

import java.util.Date;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.ArcArchivespecification;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.db.PoolDBUtils;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.nrg.xnat.turbine.utils.ProjectAccessRequest;

public class Index extends SecureScreen {

    @Override
    protected void doBuildTemplate(RunData data, Context context) throws Exception {
        ArcArchivespecification arc= ArcSpecManager.GetInstance();
        if (arc==null || !arc.isComplete()){
            this.doRedirect(data, "EditArcSpecs.vm");
            return;
        }else{
            context.put("arc", arc);
        }
        
        XDATUser user = TurbineUtils.getUser(data);
        
        if(data.getParameters().get("node")!=null){
        	context.put("node", data.getParameters().get("node"));
        }
        
        ProjectAccessRequest.CreatePARTable(user);
        
        if (user.getEmail()==null)
        {
        	data.setMessage("WARNING: A valid email account is required for many features.  Please use the (edit) link at the top of the page to add a valid email address to your user account.");
        }else{
            Integer parcount=(Integer)PoolDBUtils.ReturnStatisticQuery("SELECT COUNT(par_id)::int4 AS count FROM xs_par_table WHERE approval_date IS NULL AND LOWER(email)='"+ user.getEmail().toLowerCase() + "'", "count", user.getDBName(), user.getLogin());
            context.put("par_count", parcount);
        }
        
        Date lastLogin = user.getLastLogin();
        if (lastLogin!=null)
        {
            context.put("last_login",lastLogin);
        }
        
        context.put("proj_count", user.getTotalCounts().get("xnat:projectData"));
		
		context.put("sub_count", user.getTotalCounts().get("xnat:subjectData"));
		
		Long isd_count=(Long)PoolDBUtils.ReturnStatisticQuery("SELECT COUNT(*) FROM xnat_imageSessionData", "count", TurbineUtils.getUser(data).getDBName(), TurbineUtils.getUser(data).getUsername());
		context.put("isd_count", isd_count);
    }
}
