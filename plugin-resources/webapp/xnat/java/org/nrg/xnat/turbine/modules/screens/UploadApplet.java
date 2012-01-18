// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UploadApplet extends SecureScreen {
	static Logger logger = LoggerFactory.getLogger(UploadApplet.class);
	
	@Override
	protected void doBuildTemplate(RunData data, Context context) throws Exception {
		if(TurbineUtils.HasPassedParameter("project", data)){
			context.put("project", TurbineUtils.GetPassedParameter("project", data));
		}

		if(TurbineUtils.HasPassedParameter("part", data)){
			context.put("subject_id", TurbineUtils.GetPassedParameter("part", data));
        } else if (TurbineUtils.HasPassedParameter("part_id", data)) {
            context.put("subject_id", TurbineUtils.GetPassedParameter("part_id", data));
        } else if (TurbineUtils.HasPassedParameter("subject_id", data)) {
            context.put("subject_id", TurbineUtils.GetPassedParameter("subject_id", data));
		}
		
		if(TurbineUtils.HasPassedParameter("expt_id", data)){
			context.put("expt_id", TurbineUtils.GetPassedParameter("expt_id", data));
        } else if (TurbineUtils.HasPassedParameter("session_id", data)) {
            context.put("expt_id", TurbineUtils.GetPassedParameter("session_id", data));
		}
		
		context.put("arc",ArcSpecManager.GetInstance());
	}
}