// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Aug 07 11:23:27 CDT 2007
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
public abstract class BaseArcPipelineparameterdata extends AutoArcPipelineparameterdata {

	public BaseArcPipelineparameterdata(ItemI item)
	{
		super(item);
	}

	public BaseArcPipelineparameterdata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseArcPipelineparameterdata(UserI user)
	 **/
	public BaseArcPipelineparameterdata()
	{}

	public BaseArcPipelineparameterdata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
