/*
 * org.nrg.xnat.restlet.rundata.RestletRunData
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:40 PM
 */
package org.nrg.xnat.restlet.rundata;

import org.apache.turbine.services.rundata.DefaultTurbineRunData;

import java.io.PrintWriter;

public class RestletRunData extends DefaultTurbineRunData {
	public void hijackOutput(PrintWriter os){
		this.setOut(os);
	}
}
