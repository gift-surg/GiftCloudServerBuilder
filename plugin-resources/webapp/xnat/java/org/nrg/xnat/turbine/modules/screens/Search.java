// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;

public class Search extends SecureScreen {

	@Override
	protected void doBuildTemplate(RunData data, Context context)
			throws Exception {
		 if(data.getParameters().get("node")!=null){
        	context.put("node", data.getParameters().get("node"));
         }
		 if(data.getParameters().get("new_search")!=null){
	        	context.put("newSearch", "true");
	         }
	}

}
