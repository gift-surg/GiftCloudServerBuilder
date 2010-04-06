// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Mon Aug 27 09:45:18 CDT 2007
 *
 */
package org.nrg.xdat.om.base;
import org.nrg.xdat.om.ArcProjectDescendantPipeline;
import org.nrg.xdat.om.base.auto.*;
import org.nrg.xft.*;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.exceptions.PipelineNotFoundException;

import java.util.*;

/**
 * @author XDAT
 *
 */
@SuppressWarnings("serial")
public abstract class BaseArcProjectDescendant extends AutoArcProjectDescendant {

	public BaseArcProjectDescendant(ItemI item)
	{
		super(item);
	}

	public BaseArcProjectDescendant(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseArcProjectDescendant(UserI user)
	 **/
	public BaseArcProjectDescendant()
	{}

	public BaseArcProjectDescendant(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

	
	public int getPipelineIndex(String pipelinePath) throws PipelineNotFoundException {
		int i = -1;
		ArrayList<ArcProjectDescendantPipeline> pipelines = getPipeline();
		for (int j = 0; j <pipelines.size(); j++) {
			if (pipelines.get(j).getLocation().equals(pipelinePath)) {
				i = j;
				break;
			}
		}
		if (i == -1) throw new PipelineNotFoundException("Couldnt find pipelines located at " + pipelinePath + " for " + getXsitype());
		return i;
	}

	public ArcProjectDescendantPipeline getPipeline(String pipelinePath) throws PipelineNotFoundException {
		ArcProjectDescendantPipeline rtn = null;
		ArrayList<ArcProjectDescendantPipeline> pipelines = getPipeline();
		for (int j = 0; j <pipelines.size(); j++) {
			if (pipelines.get(j).getLocation().equals(pipelinePath)) {
				rtn = pipelines.get(j);
				break;
			}
		}
		if (rtn == null) throw new PipelineNotFoundException("Couldnt find pipelines located at " + pipelinePath + " for " + getXsitype());
		return rtn;
	}

}
