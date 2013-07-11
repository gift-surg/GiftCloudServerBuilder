/*
 * org.nrg.xnat.turbine.modules.screens.CSVScreen
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;

public class CSVScreen extends org.nrg.xdat.turbine.modules.screens.CSVScreen {

	@Override
	public String getContentType(RunData data) {
		return "text/csv";
	}

}
