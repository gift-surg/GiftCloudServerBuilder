// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UploadApplet extends SecureScreen {
	static Logger logger = LoggerFactory.getLogger(UploadApplet.class);
	
	@Override
	protected void doBuildTemplate(RunData data, Context context) throws Exception {
        storeParameterIfPresent(data, context, "project");
        storeParameterIfPresent(data, context, "part", "part_id", "subject_id");
        storeParameterIfPresent(data, context, "expt_id", "session_id");
        storeParameterIfPresent(data, context, "scan_date");
        storeParameterIfPresent(data, context, "visit_id");
        storeParameterIfPresent(data, context, "scan_type");
		context.put("arc", ArcSpecManager.GetInstance());
	}
}
