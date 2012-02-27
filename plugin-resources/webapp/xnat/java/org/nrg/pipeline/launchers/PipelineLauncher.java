/* 
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 * 	
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

*/

package org.nrg.pipeline.launchers;

import java.io.File;
import java.util.ArrayList;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.apache.xmlbeans.XmlOptions;
import org.nrg.pipeline.xmlbeans.ParametersDocument;
import org.nrg.pipeline.xmlbeans.ParametersDocument.Parameters;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.turbine.utils.TurbineUtils;

public abstract class PipelineLauncher {
	public abstract boolean launch(RunData data, Context context);
	
	protected ArrayList<String> getCheckBoxSelections(RunData data, XnatImagesessiondata imageSession, String type) {
		ArrayList<String> rtn = new ArrayList<String>();
		int totalCount = ((Integer)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedInteger(type+"_rowcount",data));
		for (int i = 0; i < totalCount; i++) {
			if (TurbineUtils.HasPassedParameter(type+"_"+i, data)) {
				rtn.add(((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter(type+"_"+i,data)));
			}			
		}
		return rtn;
	}
	
	protected ArrayList<String> getCheckBoxSelections(RunData data, XnatImagesessiondata imageSession, String type, int totalCount) {
		ArrayList<String> rtn = new ArrayList<String>();
		for (int i = 0; i < totalCount; i++) {
			if (TurbineUtils.HasPassedParameter(type+"_"+i, data)) {
				rtn.add(((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter(type+"_"+i,data)));
			}			
		}
		return rtn;
	}


	  protected String saveParameters(String rootpath, String fileName, Parameters parameters) throws Exception{
	        File dir = new File(rootpath);
	        if (!dir.exists()) dir.mkdirs();
	        File paramFile = new File(rootpath + File.separator + fileName);
	        ParametersDocument paramDoc = ParametersDocument.Factory.newInstance();
	        paramDoc.addNewParameters().set(parameters);
	        paramDoc.save(paramFile,new XmlOptions().setSavePrettyPrint().setSaveAggressiveNamespaces());
	        return paramFile.getAbsolutePath();
	    }
	  
	  protected String getName(String name) {
		  String rtn = name;
		  int i = name.lastIndexOf(File.separator);
		  if (i != -1) 
			  rtn = name.substring(i+1);
		  i = rtn.lastIndexOf(".");
		  if (i != -1)
			  rtn = rtn.substring(0, i);
		  return rtn;
	  }
	
}
