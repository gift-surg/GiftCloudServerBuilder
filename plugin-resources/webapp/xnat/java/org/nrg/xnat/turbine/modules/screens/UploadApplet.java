// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.nrg.xnat.utils.XnatHttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UploadApplet extends SecureScreen {
	static Logger logger = LoggerFactory.getLogger(UploadApplet.class);
	
	@Override
	protected void doBuildTemplate(RunData data, Context context) throws Exception {
		context.put("jsessionid", XnatHttpUtils.getJSESSIONID(data));
        storeParameterIfPresent(data, context, "project");
        storeParameterIfPresent(data, context, "subject_id", "part", "part_id");
        storeParameterIfPresent(data, context, "subject_label");
        storeParameterIfPresent(data, context, "session_id", "expt_id");
        storeParameterIfPresent(data, context, "visit_id");
        storeParameterIfPresent(data, context, "visit");
        storeParameterIfPresent(data, context, "protocol");
        storeParameterIfPresent(data, context, "expectedModality");
        storeParameterIfPresent(data, context, "scan_type");
        if (TurbineUtils.HasPassedParameter("session_date", data)) {
            context.put("session_date", ((String)TurbineUtils.GetPassedParameter("session_date", data)).replace('.', '/'));
        } else if (TurbineUtils.HasPassedParameter("no_session_date", data)) {
            context.put("session_date", "no_session_date");
        }
		context.put("arc", ArcSpecManager.GetInstance());
	}
}
