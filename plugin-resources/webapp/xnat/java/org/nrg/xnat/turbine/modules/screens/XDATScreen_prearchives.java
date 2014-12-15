/*
 * org.nrg.xnat.turbine.modules.screens.XDATScreen_prearchives
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/26/13 11:58 AM
 */
package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.turbine.utils.XNATUtils;

import java.util.Hashtable;

public class XDATScreen_prearchives extends SecureScreen {
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.turbine.modules.screens.VelocityScreen#doBuildTemplate(org
	 * .apache.turbine.util.RunData, org.apache.velocity.context.Context)
	 */
	protected void doBuildTemplate(final RunData data, final Context context) {
		try {
			context.put("user", TurbineUtils.getUser(data).getUsername());
			final Hashtable hash = XNATUtils.getInvestigatorsForRead(
					"xnat:mrSessionData", data);
			context.put("investigators", hash);

			if (data.getParameters().containsKey("project")) {
				context.put("project", org.nrg.xdat.turbine.utils.TurbineUtils
						.GetPassedParameter("project", data));
			}
			if (TurbineUtils.getUser(data).checkRole("Administrator")) {
				context.put("role", "admin");
			}
		} catch (Exception e) {
			log.error(e);
			e.printStackTrace();
		}
	}

}
