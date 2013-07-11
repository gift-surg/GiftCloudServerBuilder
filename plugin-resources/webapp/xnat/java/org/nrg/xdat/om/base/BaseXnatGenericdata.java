/*
 * org.nrg.xdat.om.base.BaseXnatGenericdata
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xdat.om.base;

import org.nrg.xdat.om.base.auto.AutoXnatGenericdata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

import java.util.Hashtable;


public class BaseXnatGenericdata extends AutoXnatGenericdata{
	public BaseXnatGenericdata(ItemI item)
	{
		super(item);
	}

	public BaseXnatGenericdata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatHdscandata(UserI user)
	 **/
	public BaseXnatGenericdata()
	{}

	public BaseXnatGenericdata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}
}
