/* 
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 * 	
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

*/

package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.pipeline.PipelineRepositoryManager;
import org.nrg.xdat.om.PipePipelinerepository;
import org.nrg.xdat.turbine.modules.screens.AdminScreen;

public class XDATScreen_manage_pipeline extends AdminScreen {
	 
	protected void doBuildTemplate(RunData data, Context context)     throws Exception {
			PipePipelinerepository pipelineRepository = PipelineRepositoryManager.GetInstance();
			context.put("repository", pipelineRepository);
	}
}
