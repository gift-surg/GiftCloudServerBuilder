// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Wed Nov 05 15:35:37 CST 2008
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoPipePipelinedetailsParameter;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BasePipePipelinedetailsParameter extends AutoPipePipelinedetailsParameter {

	public BasePipePipelinedetailsParameter(ItemI item)
	{
		super(item);
	}

	public BasePipePipelinedetailsParameter(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BasePipePipelinedetailsParameter(UserI user)
	 **/
	public BasePipePipelinedetailsParameter()
	{}

	public BasePipePipelinedetailsParameter(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
