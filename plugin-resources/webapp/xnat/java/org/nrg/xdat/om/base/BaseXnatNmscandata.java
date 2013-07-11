/*
 * org.nrg.xdat.om.base.BaseXnatNmscandata
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xdat.om.base;

import org.nrg.xdat.om.base.auto.AutoXnatNmscandata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

import java.util.Hashtable;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatNmscandata extends AutoXnatNmscandata {

	public BaseXnatNmscandata(ItemI item)
	{
		super(item);
	}

	public BaseXnatNmscandata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatNmscandata(UserI user)
	 **/
	public BaseXnatNmscandata()
	{}

	public BaseXnatNmscandata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
