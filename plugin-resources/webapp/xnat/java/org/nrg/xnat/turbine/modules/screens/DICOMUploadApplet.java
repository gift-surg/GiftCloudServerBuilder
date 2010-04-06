// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.turbine.modules.screens;

import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFTItem;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

public class DICOMUploadApplet extends SecureScreen {
	static Logger logger = Logger.getLogger(DICOMUploadApplet.class);
	
	@Override
	protected void doBuildTemplate(RunData data, Context context) throws Exception {
		if(TurbineUtils.HasPassedParameter("project", data)){
			context.put("project", TurbineUtils.GetPassedParameter("project", data));
		}

		if(TurbineUtils.HasPassedParameter("subject_id", data)){
			context.put("subject_id", TurbineUtils.GetPassedParameter("subject_id", data));
		}

		if(TurbineUtils.HasPassedParameter("part_id", data)){
			context.put("subject_id", TurbineUtils.GetPassedParameter("part_id", data));
		}

		if(TurbineUtils.HasPassedParameter("part", data)){
			context.put("subject_id", TurbineUtils.GetPassedParameter("part", data));
		}
		
		if(TurbineUtils.HasPassedParameter("session_id", data)){
			context.put("expt_id", TurbineUtils.GetPassedParameter("session_id", data));
		}
		
		if(TurbineUtils.HasPassedParameter("expt_id", data)){
			context.put("expt_id", TurbineUtils.GetPassedParameter("expt_id", data));
		}
		
		context.put("arc",ArcSpecManager.GetInstance());
	}
}