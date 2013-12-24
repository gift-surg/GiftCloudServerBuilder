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

import java.io.PrintWriter;
import java.util.Map;

import org.apache.turbine.services.rundata.DefaultTurbineRunData;

import com.google.common.collect.Maps;

public class RestletRunData extends DefaultTurbineRunData {
	Map<String,Object> passedObjects=Maps.newHashMap();
	
	public void passObject(String key, Object o){
		passedObjects.put(key,o);
	}
	
	public Object retrieveObject(String key){
		return passedObjects.remove(key);
	}
	
	public void hijackOutput(PrintWriter os){
		this.setOut(os);
	}
}
