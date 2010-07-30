// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Aug 07 11:23:27 CDT 2007
 *
 */
package org.nrg.xdat.om.base;
import org.nrg.pipeline.PipelineRepositoryManager;
import org.nrg.xdat.om.ArcPipelinedataI;
import org.nrg.xdat.om.ArcProjectDescendant;
import org.nrg.xdat.om.ArcProjectDescendantPipeline;
import org.nrg.xdat.om.ArcProjectPipeline;
import org.nrg.xdat.om.base.auto.*;
import org.nrg.xft.*;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.exceptions.PipelineNotFoundException;
import org.nrg.pipeline.PipelineRepositoryManager;

import java.util.*;

/**
 * @author XDAT
 *
 */
@SuppressWarnings("serial")
public abstract class BaseArcProject extends AutoArcProject {

	public BaseArcProject(ItemI item)
	{
		super(item);
	}

	public BaseArcProject(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseArcProject(UserI user)
	 **/
	public BaseArcProject()
	{}

	public BaseArcProject(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

	@Override
	public Integer getPrearchiveCode() {
		Integer i= super.getPrearchiveCode();
		if(i==null){
			return new Integer(0);
		}else{
			return i;
		}
	}

	@Override
	public Integer getQuarantineCode() {
		Integer i= super.getQuarantineCode();
		if(i==null){
			return new Integer(0);
		}else{
			return i;
		}
	}

		public ArrayList<ArcProjectDescendantPipeline> getPipelinesForDescendant(String xsiType) {
			ArrayList<ArcProjectDescendantPipeline>rtn = new ArrayList<ArcProjectDescendantPipeline>();
			ArrayList<ArcProjectDescendant> descendants = getPipelines_descendants_descendant();
			if (xsiType == null || descendants == null) return rtn;
			for (int i = 0; i < descendants.size(); i++) {
if (descendants.get(i).getXsitype().equals(xsiType) || descendants.get(i).getXsitype().equals(PipelineRepositoryManager.ALL_DATA_TYPES)) {
					rtn = descendants.get(i).getPipeline();
					break;
				}
			}
			return rtn;
		}




	public ArcPipelinedataI getPipelineForDescendant(String xsiType, String pipelineStep) throws PipelineNotFoundException {
		ArcPipelinedataI rtn = null;
		ArrayList<ArcProjectDescendantPipeline> descendantPipelines = getPipelinesForDescendant(xsiType);
		for (int i = 0; i < descendantPipelines.size(); i++) {
			if (descendantPipelines.get(i).getStepid().equals(pipelineStep)) {
				rtn = descendantPipelines.get(i).getPipelinedata();
				break;
			}
		}
		if (rtn == null) throw new PipelineNotFoundException("A Pipeline identified by " + pipelineStep + " could not be found for " + xsiType + " for project " + getId());
		return rtn;
	}

	public ArcPipelinedataI getPipelineForDescendantByPath(String xsiType, String pipelinePath) throws PipelineNotFoundException {
		ArcPipelinedataI rtn = null;
		ArrayList<ArcProjectDescendantPipeline> descendantPipelines = getPipelinesForDescendant(xsiType);
		for (int i = 0; i < descendantPipelines.size(); i++) {
			if (descendantPipelines.get(i).getLocation().equals(pipelinePath)) {
				rtn = descendantPipelines.get(i).getPipelinedata();
				break;
			}
		}
		if (rtn == null) throw new PipelineNotFoundException("A Pipeline identified by " + pipelinePath + " could not be found for " + xsiType + " for project " + getId());
		return rtn;
	}

	public ArcProjectDescendantPipeline getPipelineForDescendantEltByPath(String xsiType, String pipelinePath) throws PipelineNotFoundException {
		ArcProjectDescendantPipeline rtn = null;
		ArrayList<ArcProjectDescendantPipeline> descendantPipelines = getPipelinesForDescendant(xsiType);
		for (int i = 0; i < descendantPipelines.size(); i++) {
			if (descendantPipelines.get(i).getLocation().equals(pipelinePath)) {
				rtn = descendantPipelines.get(i);
				break;
			}
		}
		if (rtn == null) throw new PipelineNotFoundException("A Pipeline identified by " + pipelinePath + " could not be found for " + xsiType + " for project " + getId());
		return rtn;
	}

	public int getPipelineForDescendantIndexByPath(String xsiType, String pipelinePath) throws PipelineNotFoundException {
		int rtn = -1;
		ArrayList<ArcProjectDescendantPipeline> descendantPipelines = getPipelinesForDescendant(xsiType);
		for (int i = 0; i < descendantPipelines.size(); i++) {
			if (descendantPipelines.get(i).getLocation().equals(pipelinePath)) {
				rtn = i;
				break;
			}
		}
		if (rtn == -1) throw new PipelineNotFoundException("A Pipeline identified by " + pipelinePath + " could not be found for " + xsiType + " for project " + getId());
		return rtn;
	}


	public ArcPipelinedataI getPipeline(String pipelineStep) throws PipelineNotFoundException {
		ArcPipelinedataI rtn = null;
		ArrayList<ArcProjectPipeline> pipelines =getPipelines_pipeline();
		for (int i = 0; i < pipelines.size(); i++) {
			if (pipelines.get(i).getStepid().equals(pipelineStep)) {
				rtn = pipelines.get(i).getPipelinedata();
				break;
			}
		}
		if (rtn == null) throw new PipelineNotFoundException("A Pipeline identified by " + pipelineStep + " could not be found for  project " + getId());
		return rtn;
	}



	public ArcPipelinedataI getPipelineByPath(String pipelinePath) throws PipelineNotFoundException {
		ArcPipelinedataI rtn = null;
		ArrayList<ArcProjectPipeline> pipelines =getPipelines_pipeline();
		for (int i = 0; i < pipelines.size(); i++) {
			if (pipelines.get(i).getLocation().equals(pipelinePath)) {
				rtn = pipelines.get(i).getPipelinedata();
				break;
			}
		}
		if (rtn == null) throw new PipelineNotFoundException("A Pipeline identified by " + pipelinePath + " could not be found for  project " + getId());
		return rtn;
	}

	public ArcProjectPipeline getPipelineEltByPath(String pipelinePath) throws PipelineNotFoundException {
		ArcProjectPipeline rtn = null;
		ArrayList<ArcProjectPipeline> pipelines =getPipelines_pipeline();
		for (int i = 0; i < pipelines.size(); i++) {
			if (pipelines.get(i).getLocation().equals(pipelinePath)) {
				rtn = pipelines.get(i);
				break;
			}
		}
		if (rtn == null) throw new PipelineNotFoundException("A Pipeline identified by " + pipelinePath + " could not be found for  project " + getId());
		return rtn;
	}

	public int getPipelineIndexByPath(String pipelinePath) throws PipelineNotFoundException {
		int rtn = -1;
		ArrayList<ArcProjectPipeline> pipelines =getPipelines_pipeline();
		for (int i = 0; i < pipelines.size(); i++) {
			if (pipelines.get(i).getLocation().equals(pipelinePath)) {
				rtn = i;
				break;
			}
		}
		if (rtn == -1) throw new PipelineNotFoundException("A Pipeline identified by " + pipelinePath + " could not be found for  project " + getId());
		return rtn;
	}


	public ArcProjectDescendant getDescendant(String xsiType) {
		ArcProjectDescendant rtn = null;
		ArrayList<ArcProjectDescendant> descendants = getPipelines_descendants_descendant();
		if (xsiType == null || descendants == null) return rtn;
		for (int i = 0; i < descendants.size(); i++) {
			if (descendants.get(i).getXsitype().equals(xsiType)) {
				rtn = descendants.get(i);
				break;
			}
		}
		return rtn;
	}

	public void removeAllDescendantPipelines() {
		ArrayList<ArcProjectDescendant> descendants = getPipelines_descendants_descendant();
		if (descendants.size() > 0) {
			int i = 0;
			Iterator iter = descendants.iterator();
			while (iter.hasNext()) {
				removePipelines_descendants_descendant(i);
				descendants = getPipelines_descendants_descendant();
				iter = descendants.iterator();
			}
		}
	}

	public void removeAllPipelines() {
		removeAllDescendantPipelines();
		removeAllProjectPipelines();
	}

	public void removeAllProjectPipelines() {
		ArrayList<ArcProjectPipeline> projPipeline = getPipelines_pipeline();
		if (projPipeline.size() > 0) {
			int i = 0;
			Iterator iter = projPipeline.iterator();
			while (iter.hasNext()) {
				this.removePipelines_pipeline(i);
				projPipeline = getPipelines_pipeline();
				iter = projPipeline.iterator();
			}
		}
	}

}
