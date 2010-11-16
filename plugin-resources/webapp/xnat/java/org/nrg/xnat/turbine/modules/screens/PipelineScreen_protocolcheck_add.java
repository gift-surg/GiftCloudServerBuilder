/* 
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 * 	
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

*/

package org.nrg.xnat.turbine.modules.screens;

import java.util.ArrayList;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.security.ElementSecurity;

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
