/*
 * $Id: XDATScreen_prearchives.java,v 1.4 2007/09/10 18:16:23 karchie Exp $
 * Copyright (c) 2005 Harvard University / Howard Hughes Medical Institute (HHMI),
 *               2007 Washington University
 *
 */
package org.nrg.xnat.turbine.modules.screens;

import java.util.Hashtable;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.turbine.utils.XNATUtils;

/**
 * @author timo
 * @author Kevin A. Archie <karchie@npg.wustl.edu>
 * Adapted from XDATScreen_data_management
 *
 */
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
