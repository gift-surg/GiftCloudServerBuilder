package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;

public class CSVScreen extends org.nrg.xdat.turbine.modules.screens.CSVScreen {

	@Override
	public String getContentType(RunData data) {
		return "text/csv";
	}

}
