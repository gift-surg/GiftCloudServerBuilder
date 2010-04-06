//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on May 3, 2007
 *
 */
package org.nrg.xnat.turbine.modules.screens;

import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.screens.EditScreenA;
import org.nrg.xdat.turbine.utils.TurbineUtils;

public abstract class DataManagementEditScreenA extends EditScreenA {
    static Logger logger = Logger.getLogger(DataManagementEditScreenA.class);
    /* (non-Javadoc)
     * @see org.nrg.xdat.turbine.modules.screens.SecureScreen#isAuthorized(org.apache.turbine.util.RunData)
     */
    @Override
    protected boolean isAuthorized(RunData data) throws Exception {
        boolean authorized= super.isAuthorized(data);
        if (authorized)
        {
            if (!TurbineUtils.getUser(data).checkRole("DataManager"))
            {
                authorized=false;
                data.setMessage("Unauthorized access.  Please login to gain access to this page.");
                logAccess(data,"Unauthorized access.");
                logger.error("Unauthorized Access to an DataManager Report (prevented).");
            }
        }
        
        return authorized;
    }

}
