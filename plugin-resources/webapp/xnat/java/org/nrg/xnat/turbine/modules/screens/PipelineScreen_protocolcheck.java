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
import org.nrg.xdat.turbine.modules.screens.SecureReport;

public class PipelineScreen_protocolcheck extends DefaultPipelineScreen{

	static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PipelineScreen_protocolcheck_add.class);
	
	 public void preProcessing(RunData data, Context context)   {
	     super.preProcessing(data, context);
  	 }

	
	 public void finalProcessing(RunData data, Context context){
	     	context.put("projectSettings", projectParameters);
	 	   
	 }
		
}
