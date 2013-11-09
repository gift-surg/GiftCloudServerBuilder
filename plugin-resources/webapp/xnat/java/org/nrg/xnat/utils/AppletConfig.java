/*
 * org.nrg.xnat.utils.AppletConfig
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.utils;

import java.util.Map;

/*
 * This is a bean whose sole purpose is to deserialize the applet settings json.
 */

public class AppletConfig{
	
	public static String toolName = "applet";
	public static String path = "settings";
	
	public Map<String, String> launch;
	public Map<String, String> parameters;
	public Map<String, String> getLaunch() {
		return launch;
	}
	public void setLaunch(Map<String, String> launch) {
		this.launch = launch;
	}
	public Map<String, String> getParameters() {
		return parameters;
	}
	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

}