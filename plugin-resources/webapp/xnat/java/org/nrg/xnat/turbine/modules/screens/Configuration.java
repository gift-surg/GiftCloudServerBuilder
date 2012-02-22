// Copyright 2010 Washington University School of Medicine All Rights Reserved
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
            context.put("initialize", true);
        }
        context.put("arc", arcSpec);
        cacheTabs(context, "configuration");
    }
}
