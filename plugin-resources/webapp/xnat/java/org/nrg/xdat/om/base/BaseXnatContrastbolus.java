// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Oct 07 17:09:40 CDT 2008
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatContrastbolus;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatContrastbolus extends AutoXnatContrastbolus {

	public BaseXnatContrastbolus(ItemI item)
	{
		super(item);
	}

	public BaseXnatContrastbolus(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatContrastbolus(UserI user)
	 **/
	public BaseXnatContrastbolus()
	{}

	public BaseXnatContrastbolus(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
