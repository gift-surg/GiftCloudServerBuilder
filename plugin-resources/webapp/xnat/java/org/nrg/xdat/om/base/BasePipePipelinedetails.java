// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Wed Nov 05 15:35:37 CST 2008
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoPipePipelinedetails;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BasePipePipelinedetails extends AutoPipePipelinedetails {

	public BasePipePipelinedetails(ItemI item)
	{
		super(item);
	}

	public BasePipePipelinedetails(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BasePipePipelinedetails(UserI user)
	 **/
	public BasePipePipelinedetails()
	{}

	public BasePipePipelinedetails(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
