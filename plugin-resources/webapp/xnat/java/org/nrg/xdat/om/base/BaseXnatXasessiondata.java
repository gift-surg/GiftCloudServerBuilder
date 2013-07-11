/*
 * org.nrg.xdat.om.base.BaseXnatXasessiondata
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xdat.om.base;

import org.nrg.xdat.om.base.auto.AutoXnatXasessiondata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

import java.util.Hashtable;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatXasessiondata extends AutoXnatXasessiondata {

	public BaseXnatXasessiondata(ItemI item)
	{
		super(item);
	}

	public BaseXnatXasessiondata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatXasessiondata(UserI user)
	 **/
	public BaseXnatXasessiondata()
	{}

	public BaseXnatXasessiondata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
