/*
 * org.nrg.xnat.turbine.modules.screens.UploadApplet
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.turbine.modules.screens;

import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LaunchGiftCloudUploader extends SecureScreen {
	private static final Logger logger = LoggerFactory
			.getLogger(LaunchGiftCloudUploader.class);
	
	@Override
	public void doBuildTemplate(RunData data, Context context) {
		// nop
	}
}
