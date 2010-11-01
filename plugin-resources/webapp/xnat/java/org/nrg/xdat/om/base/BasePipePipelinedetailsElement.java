// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Wed Nov 05 15:35:37 CST 2008
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoPipePipelinedetailsElement;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BasePipePipelinedetailsElement extends AutoPipePipelinedetailsElement {

	public BasePipePipelinedetailsElement(ItemI item)
	{
		super(item);
	}

	public BasePipePipelinedetailsElement(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BasePipePipelinedetailsElement(UserI user)
	 **/
	public BasePipePipelinedetailsElement()
	{}

	public BasePipePipelinedetailsElement(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
