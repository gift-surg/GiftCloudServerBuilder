/* 
 * org.nrg.xnat.turbine.modules.screens.PipelineScreen_protocolcheck_add
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
import org.apache.velocity.context.Context;
import org.nrg.xdat.security.ElementSecurity;

import java.util.ArrayList;

public class PipelineScreen_protocolcheck_add extends PipelineScreen_add_project_pipeline{

	static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PipelineScreen_protocolcheck_add.class);
	

	
	 public void finalProcessing(RunData data, Context context){
		 try {
			 ArrayList elementsAll = ElementSecurity.GetElementNames();
			 ArrayList elements = new ArrayList();
			 for (int i=0; i< elementsAll.size(); i++) {
				 if (!((String)elementsAll.get(i)).startsWith("xdat:") && !((String)elementsAll.get(i)).startsWith("prov:") && !((String)elementsAll.get(i)).startsWith("wrk:") && !((String)elementsAll.get(i)).startsWith("val:")) {
					elements.add(elementsAll.get(i)); 
				 }
			 }
			 context.put("elements",elements);

		 }catch(Exception e) {
			 logger.error("Encountered error", e);
			 e.printStackTrace();
		 }
	 }
		
}
