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
        	//build up the full date/time expected by the upload applet. If the upload applet recieves a date/time in the format
        	// mm/dd/yyyy HH:MM it will verify the scan time is within 61 minutes of HH:MM (24 hour time). If it does not receive
        	// HH:MM or if HH:MM == 00:00 the applet will only verify the scan is on the same day as mm/dd/yyyy. If it receives no
        	// session_date, it will prompt the user for one.  With that said, we want to build up the mm/dd/yyyy string if we can.
        	// we'll do it here.
        	String hhmm = " 00:00";
        	if (TurbineUtils.HasPassedParameter("session_time_h", data) && TurbineUtils.HasPassedParameter("session_time_m", data)) {
        		// parameters are set with drop-downs so no need to validate here.
        		String hr = (String)TurbineUtils.GetPassedParameter("session_time_h", data);
        		String mm =(String)TurbineUtils.GetPassedParameter("session_time_m", data);
        		try{
        			Integer.parseInt(hr);  //poor man's validation
        			Integer.parseInt(mm);  
        			hhmm =  hr + ":" + mm;
        		} catch (Exception e){
        			//if one or both aren't an integer, we'll get here, which is fine. it just means they didn't select a time. 
        		}
        	}
            context.put("session_date", ((String)TurbineUtils.GetPassedParameter("session_date", data)).replace('.', '/') + " " + hhmm);
        } else if (TurbineUtils.HasPassedParameter("no_session_date", data)) {
            context.put("session_date", "no_session_date");
        }
		context.put("arc", ArcSpecManager.GetInstance());
	}
}
