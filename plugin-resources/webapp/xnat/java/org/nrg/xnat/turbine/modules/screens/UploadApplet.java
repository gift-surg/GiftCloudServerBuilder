// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;

public class UploadApplet extends SecureScreen {
	static Logger logger = LoggerFactory.getLogger(UploadApplet.class);
	
	@Override
	protected void doBuildTemplate(RunData data, Context context) throws Exception {
        Cookie[] cookies = data.getRequest().getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equalsIgnoreCase("jsessionid")) {
                    context.put("jsessionid", cookie.getValue());
                    break;
                }
            }
        }
        storeParameterIfPresent(data, context, "project");
        storeParameterIfPresent(data, context, "subject_id", "part", "part_id");
        storeParameterIfPresent(data, context, "session_id", "expt_id");
        storeParameterIfPresent(data, context, "visit_id");
        storeParameterIfPresent(data, context, "scan_type");
        if (TurbineUtils.HasPassedParameter("session_date", data)) {
            context.put("session_date", TurbineUtils.GetPassedParameter("session_date", data));
        } else if (TurbineUtils.HasPassedParameter("no_session_date", data)) {
            context.put("session_date", "no_session_date");
        }
		context.put("arc", ArcSpecManager.GetInstance());
	}
}
