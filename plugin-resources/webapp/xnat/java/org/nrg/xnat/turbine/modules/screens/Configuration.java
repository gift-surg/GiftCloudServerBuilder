// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;

public class Configuration extends SecureScreen {
	@Override
	protected void doBuildTemplate(RunData data, Context context) throws Exception {
        cacheTabs(context, "configuration");
	}
}
