/*
 * org.nrg.xnat.turbine.modules.screens.XDATScreen_brief_xnat_imageSessionData
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */

/**
 * 
 */
package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.bean.XnatImagesessiondataBean;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.helpers.prearchive.PrearcTableBuilder;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;

import java.io.File;

/**
 * @author tolsen01
 *
 */
public class XDATScreen_brief_xnat_imageSessionData extends SecureScreen {

	/* (non-Javadoc)
	 * @see org.apache.turbine.modules.screens.VelocitySecureScreen#doBuildTemplate(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
	 */
	@Override
	protected void doBuildTemplate(RunData data, Context context) throws Exception {
		final String folder = (String)TurbineUtils.GetPassedParameter("folder",data);
        final String timestamp = (String)TurbineUtils.GetPassedParameter("timestamp",data);
        final String project = (String)TurbineUtils.GetPassedParameter("project",data);	// can we final this?
        final XDATUser user = TurbineUtils.getUser(data);
        
        final File sessionDir=PrearcUtils.getPrearcSessionDir(user, project, timestamp, folder,false);
        
        final File sessionXML = new File(sessionDir.getPath() + ".xml");
		final XnatImagesessiondataBean sessionBean;
		try {
			sessionBean = PrearcTableBuilder.parseSession(sessionXML);
		} catch (Exception e) {
			error(e, data);
			return;
		}
		
		context.put("session",sessionBean);
		context.put("url", String.format("/prearchive/projects/%s/%s/%s",project,timestamp,folder));
	}

}
