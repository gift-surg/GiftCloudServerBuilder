/*
 * org.nrg.xnat.turbine.modules.screens.XDATScreen_prearchives
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.turbine.utils.XNATUtils;

import java.util.Hashtable;

public class XDATScreen_prearchives extends SecureScreen {
    /* (non-Javadoc)
     * @see org.apache.turbine.modules.screens.VelocityScreen#doBuildTemplate(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    protected void doBuildTemplate(final RunData data, final Context context) {
	try {
	    context.put("user", TurbineUtils.getUser(data).getUsername());
	    final Hashtable hash = XNATUtils.getInvestigatorsForRead("xnat:mrSessionData",data);
	    context.put("investigators", hash);

	    if (data.getParameters().containsKey("project")) {
		context.put("project", org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("project",data));
	    }
	} catch (Exception e) {
	    log.error(e);
	    e.printStackTrace();
	}
    }

}
