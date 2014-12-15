/*
 * org.nrg.xnat.turbine.modules.screens.BatchUploadSummary
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;

public class BatchUploadSummary extends SecureScreen {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.turbine.modules.screens.VelocitySecureScreen#doBuildTemplate
	 * (org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
	 */
	@Override
	protected void doBuildTemplate(RunData data, Context context)
			throws Exception {
		XDATUser user = TurbineUtils.getUser(data);
		String cachepath = ArcSpecManager.GetInstance().getGlobalCachePath();

		cachepath += "uploads" + File.separator + user.getLogin()
				+ File.separator;

		java.text.NumberFormat nf = NumberFormat.getInstance();
		ArrayList uploadedFiles = new ArrayList();
		java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat(
				"MM/dd/yyyy HH:mm:ss");
		File dir = new File(cachepath);
		File[] listFiles = dir.listFiles();
		for (int i = 0; i < listFiles.length; i++) {
			if (listFiles[i].isDirectory()) {
				// root
				File[] children = listFiles[i].listFiles();

				for (int j = 0; j < children.length; j++) {
					ArrayList file = new ArrayList();
					file.add(children[j].getName());
					file.add(formatter.format(children[j].lastModified()));
					file.add((children[j].length() / 1024) + " KB");
					uploadedFiles.add(file);
				}
			}
		}

		context.put("uploadedFiles", uploadedFiles);
	}

}