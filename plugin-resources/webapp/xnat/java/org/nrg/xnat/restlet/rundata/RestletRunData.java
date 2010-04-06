// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.rundata;

import java.io.PrintWriter;

import org.apache.turbine.services.rundata.DefaultTurbineRunData;

public class RestletRunData extends DefaultTurbineRunData {
	public void hijackOutput(PrintWriter os){
		this.setOut(os);
	}
}
