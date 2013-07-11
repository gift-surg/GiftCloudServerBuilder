/*
 * org.nrg.xdat.om.base.BaseXnatAybocsdata
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xdat.om.base;

import org.nrg.xdat.om.base.auto.AutoXnatAybocsdata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

import java.util.Hashtable;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatAybocsdata extends AutoXnatAybocsdata {

	public BaseXnatAybocsdata(ItemI item)
	{
		super(item);
	}

	public BaseXnatAybocsdata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatAybocsdata(UserI user)
	 **/
	public BaseXnatAybocsdata()
	{}

	public BaseXnatAybocsdata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}

