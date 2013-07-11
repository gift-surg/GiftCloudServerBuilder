/*
 * org.nrg.xnat.turbine.modules.screens.Configuration
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:40 PM
 */
package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.ArcArchivespecification;
import org.nrg.xdat.turbine.modules.screens.AdminScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

public class Configuration extends AdminScreen {
    @Override
    protected void doBuildTemplate(RunData data, Context context) throws Exception {
        ArcArchivespecification arcSpec = ArcSpecManager.GetInstance();
        if (arcSpec == null) {
            arcSpec = ArcSpecManager.initialize(TurbineUtils.getUser(data));
        }
        if (!ArcSpecManager.HasPersisted()) {
            context.put("initialize", true);
        } else {
            context.put("initialize", false);
        }
        context.put("arc", arcSpec);
        setDefaultTabs("siteInfo", "fileSystem", "registration", "notifications", "anonymization", "applet", "dicomReceiver");
        cacheTabs(context, "configuration");
    }
}
