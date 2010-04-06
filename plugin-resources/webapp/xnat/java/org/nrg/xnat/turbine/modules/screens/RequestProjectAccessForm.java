//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on May 21, 2007
 *
 */
package org.nrg.xnat.turbine.modules.screens;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.XdatUser;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;

public class RequestProjectAccessForm extends SecureScreen {
    private XnatProjectdata project = null;
    @Override
    protected void doBuildTemplate(RunData data, Context context) throws Exception {
        String p = data.getParameters().getString("project");
        XDATUser user = TurbineUtils.getUser(data);
        if (project==null){
            project = XnatProjectdata.getXnatProjectdatasById(p, user, false);
        }
        
        if (!user.canDelete(project)){
            data.setMessage("Permission Denied.  The current user is not authorized to view this data.");
            data.setScreenTemplate("UnauthorizedAccess.vm");
            return;
        }
        
        String access_level = data.getParameters().getString("access_level");
        Integer id = data.getParameters().getInteger("id");
        XdatUser other =(XdatUser) XdatUser.getXdatUsersByXdatUserId(id,TurbineUtils.getUser(data), false);
        
        context.put("user", other);
        context.put("projectOM", project);
        context.put("access_level", access_level);
    }
    
    @Override
    public boolean allowGuestAccess() {
        return false;
    }

    
    
}
