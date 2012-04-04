/* 
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 * 	
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

*/

package org.nrg.pipeline.launchers;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.pipeline.XnatPipelineLauncher;
import org.nrg.pipeline.utils.PipelineFileUtils;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.turbine.utils.TurbineUtils;

public class FreesurferLauncher extends PipelineLauncher{
	ArrayList<String> mprageScans = null;
	static org.apache.log4j.Logger logger = Logger.getLogger(FreesurferLauncher.class);
	
	public FreesurferLauncher(ArrayList<String> mprs) {
		mprageScans = mprs;
	}
	
	public FreesurferLauncher(RunData data, XnatMrsessiondata mr) {
		mprageScans = getCheckBoxSelections(data,mr,"MPRAGE");
	}

	public boolean launch(RunData data, Context context) {
		return false;
	}
	

	
	public boolean launch(RunData data, Context context, XnatMrsessiondata mr) {
		try {
			XnatPipelineLauncher xnatPipelineLauncher = XnatPipelineLauncher.GetLauncher(data, context, mr);
		    String pipelineName = ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("freesurfer_pipelinename",data));
		    String cmdPrefix = ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("cmdprefix",data));
		    xnatPipelineLauncher.setPipelineName(pipelineName);
		    xnatPipelineLauncher.setSupressNotification(true);
		    String buildDir = PipelineFileUtils.getBuildDir(mr.getProject(), true);
		    buildDir +=  "fsrfer"  ;
		    xnatPipelineLauncher.setBuildDir(buildDir);
		    xnatPipelineLauncher.setNeedsBuildDir(false);
		    //Parameters for Freesurfer Launch
		    if (TurbineUtils.HasPassedParameter("custom_command", data)) {
	    		xnatPipelineLauncher.setParameter("custom_command",((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("custom_command",data)));
			}else {
			    xnatPipelineLauncher.setParameter("sessionId", mr.getLabel());
			    xnatPipelineLauncher.setParameter("isDicom", ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("isDicom",data)));
			    xnatPipelineLauncher.setParameter("mprs",mprageScans);
			}
		    boolean rtn = xnatPipelineLauncher.launch(cmdPrefix);
		    return rtn;
		}catch(Exception e) {
			logger.error(e.getCause() + " " + e.getLocalizedMessage());
			return false;
		}
	}
	
}
