/*
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 *
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

*/

package org.nrg.pipeline.utils;

import java.util.ArrayList;
import java.util.Collections;

import org.nrg.xdat.om.ArcPipelineparameterdata;
import org.nrg.xdat.om.ArcProject;
import org.nrg.xdat.om.ArcProjectDescendant;
import org.nrg.xdat.om.ArcProjectDescendantPipeline;
import org.nrg.xdat.om.ArcProjectPipeline;
import org.nrg.xft.XFTItem;


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
	        ArrayList<ArcProjectDescendantPipeline> pipelines = arcProjectDesc.getPipeline();
	  	  boolean projectHasAutoArchive = false;

	        for (int i =0; i < pipelines.size(); i++) {
	      	  ArcProjectDescendantPipeline pipeline = pipelines.get(i);
	      	  String currentId = pipeline.getStepid();
	      	  if (currentId.startsWith(PipelineUtils.AUTO_ARCHIVE)) {
	      		  projectHasAutoArchive = true;
	      		  String[] parts = currentId.split("_");
	      		  if (parts.length > 2) {
	      			  String currentIndex = parts[2];
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

	    public static String getNextAutoArchiveStepId(ArcProject arcProject) {
	        String rtn = PipelineUtils.AUTO_ARCHIVE;
	        ArrayList<Integer> bins = new ArrayList<Integer>();
	        ArrayList<ArcProjectPipeline> pipelines = arcProject.getPipelines_pipeline();
	  	  boolean projectHasAutoArchive = false;

	        for (int i =0; i < pipelines.size(); i++) {
	      	  ArcProjectPipeline pipeline = pipelines.get(i);
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
