// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Wed Nov 05 15:35:37 CST 2008
 *
 */
package org.nrg.xdat.om.base;
import org.nrg.xdat.om.base.auto.*;
import org.nrg.xft.*;
import org.nrg.xft.security.UserI;

import java.util.*;

/**
 * @author XDAT
 *
 */
@SuppressWarnings("serial")
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
