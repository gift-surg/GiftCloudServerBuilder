/*
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 *
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

*/

package org.nrg.pipeline.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.nrg.xdat.model.ArcProjectDescendantPipelineI;
import org.nrg.xdat.model.ArcProjectPipelineI;
import org.nrg.xdat.om.ArcProject;
import org.nrg.xdat.om.ArcProjectDescendant;
import org.nrg.xdat.om.ArcProjectPipeline;


public class PipelineUtils {

	public static final String GRAD_UNWARP = "grad_unwarp";
	public static final String TARGET = "target";
	public static final String FREESURFER = "freesurfer";
	public static final String CROSS_DAY_REGISTER = "cross_day_register";
	public static final String COLLATE = "collate";
	public static final String AUTO_ARCHIVE = "AUTO_ARCHIVE";

	   public static String getNextAutoArchiveStepId(ArcProjectDescendant arcProjectDesc) {
	        String rtn = PipelineUtils.AUTO_ARCHIVE;
	        if (arcProjectDesc == null) {
	        	rtn += "_1";
	        	return rtn;
	        }
	        ArrayList<Integer> bins = new ArrayList<Integer>();
	        List<ArcProjectDescendantPipelineI> pipelines = arcProjectDesc.getPipeline();
	  	  boolean projectHasAutoArchive = false;
	  	  if (pipelines == null || pipelines.size() ==0) {
	  		  rtn += "_1";
	  		  return rtn;
	  	  }
	        for (int i =0; i < pipelines.size(); i++) {
	      	  ArcProjectDescendantPipelineI pipeline = pipelines.get(i);
	      	  String currentId = pipeline.getStepid();
	      	  if (currentId.startsWith(PipelineUtils.AUTO_ARCHIVE)) {
	      		  projectHasAutoArchive = true;
	      		  String[] parts = currentId.split("_");
	      		  if (parts.length > 2) {
	      			  String currentIndex = parts[2];
	      			  try {
	      			    Integer binNo = new Integer(currentIndex);
		      			bins.add(binNo);
	      			  }catch(NumberFormatException ne) {

	      			  }
	      		  }
	      	  }
	        }
	        //now sort the existing and add new.
	        if (projectHasAutoArchive) {
	        	if (bins != null && bins.size()>0) { // More than 1 AutoArchive have been defined
	            	  Collections.sort(bins);
	              	  rtn = rtn + "_" + (bins.get(bins.size() -1 ).intValue() + 1);
	        	}else {
	        		rtn += "_1";
	        	}
	        }else {
	      		rtn += "_1";
	        }
	        return rtn;
	      }

	    public static String getNextAutoArchiveStepId(ArcProject arcProject) {
	        String rtn = PipelineUtils.AUTO_ARCHIVE;
	        ArrayList<Integer> bins = new ArrayList<Integer>();
	        List<ArcProjectPipelineI> pipelines = arcProject.getPipelines_pipeline();
	  	  boolean projectHasAutoArchive = false;

	        for (int i =0; i < pipelines.size(); i++) {
	      	  ArcProjectPipelineI pipeline = pipelines.get(i);
	      	  String currentId = pipeline.getStepid();
	      	  if (currentId.startsWith(PipelineUtils.AUTO_ARCHIVE)) {
	      		  projectHasAutoArchive = true;
	      		  String[] parts = currentId.split("_");
	      		  if (parts.length > 2) {
	      			  String currentIndex = parts[3];
	      			  bins.add(new Integer(currentIndex));
	      		  }
	      	  }
	        }
	        //now sort the existing and add new.
	        if (projectHasAutoArchive) {
	        	if (bins.size()>0) { // More than 1 AutoArchive have been defined
	            	  Collections.sort(bins);
	              	  rtn = rtn + "_" + bins.get(bins.size() -1 ).intValue();
	        	}else {
	        		rtn += "_1";
	        	}
	        }
	       return rtn;
	      }


}
