//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Feb 5, 2008
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xnat.utils.WorkflowUtils;

public class AdminProjectAccess extends SecureAction {

    /* (non-Javadoc)
     * @see org.apache.turbine.modules.actions.VelocitySecureAction#doPerform(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    @Override
    public void doPerform(RunData data, Context context) throws Exception {
        XDATUser user = TurbineUtils.getUser(data);
        int counter=0;
        while(TurbineUtils.HasPassedParameter("project" + counter, data)){
            String pId=(String)TurbineUtils.GetPassedParameter("project" + counter, data);
            String access=(String)TurbineUtils.GetPassedParameter("access" + counter, data);
            if (access!=null && !access.equals("")){
                XnatProjectdata p = XnatProjectdata.getXnatProjectdatasById(pId, user, false);
                
                String currentAccess = p.getPublicAccessibility();
                
                
                if (!currentAccess.equals(access)){
                    PersistentWorkflowI wrk=WorkflowUtils.buildProjectWorkflow(user, p,newEventInstance(data, EventUtils.CATEGORY.PROJECT_ACCESS, EventUtils.MODIFY_PROJECT_ACCESS));
                    EventMetaI c=wrk.buildEvent();
                    if(p.initAccessibility(access, true,c)){
                    	WorkflowUtils.complete(wrk, c);
                    }
                }
            }
            
            counter++;
        }
    }

}
