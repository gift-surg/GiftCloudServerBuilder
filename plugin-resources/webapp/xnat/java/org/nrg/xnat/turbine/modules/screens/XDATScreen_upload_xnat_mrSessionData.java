//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Sep 26, 2007
 *
 */
package org.nrg.xnat.turbine.modules.screens;

import java.util.Calendar;
import java.util.Date;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.screens.SecureReport;

public class XDATScreen_upload_xnat_mrSessionData extends SecureReport {

    @Override
    public void finalProcessing(RunData data, Context context) {

        Date d = Calendar.getInstance().getTime();
        
        StringBuffer sb = new StringBuffer();
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat ("yyyyMMdd_hhmmss");
        sb.append(formatter.format(d));
        context.put("uploadID", sb.toString());
    }

}
