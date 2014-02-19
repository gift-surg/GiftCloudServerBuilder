/*
 * org.nrg.xnat.turbine.modules.screens.PublicProjectView
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.modules.screens.VelocityScreen;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.SecurityManager;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.utils.TurbineUtils;

import java.util.ArrayList;

public class PublicProjectView extends VelocityScreen {

    /* (non-Javadoc)
     * @see org.apache.turbine.modules.screens.VelocityScreen#doBuildTemplate(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    @Override
    protected void doBuildTemplate(RunData data, Context context) throws Exception {
        XDATUser user = TurbineUtils.getUser(data);
        
        if (user==null){
            user = new XDATUser("guest");
            TurbineUtils.setUser(data, user);
        } 
        ArrayList allProjects = new ArrayList();
        
        for(XnatProjectdata p :XnatProjectdata.getAllXnatProjectdatas(user, false)){
            if (user.can(p.getItem(), SecurityManager.ACTIVATE)){
                allProjects.add(p);
            }
        }
        
        context.put("projects", allProjects);
        
    }


}
