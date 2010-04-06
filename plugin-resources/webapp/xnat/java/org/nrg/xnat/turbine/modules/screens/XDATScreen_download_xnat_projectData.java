//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Jun 20, 2007
 *
 */
package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.screens.SecureReport;
import org.nrg.xdat.turbine.utils.TurbineUtils;

public class XDATScreen_download_xnat_projectData extends SecureReport {

    @Override
    public void finalProcessing(RunData data, Context context) {

        context.put("appletPath",TurbineUtils.GetRelativeServerPath(data) + "/applet");
        context.put("serverRoot",TurbineUtils.GetRelativeServerPath(data));
    }

}
