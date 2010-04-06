// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;

public class ReportIssue extends SecureScreen {

	@Override
	protected void doBuildTemplate(RunData data, Context context) throws Exception {
		// doesn't currently need any context, just needed to subclass SecureScreen
	}

}
