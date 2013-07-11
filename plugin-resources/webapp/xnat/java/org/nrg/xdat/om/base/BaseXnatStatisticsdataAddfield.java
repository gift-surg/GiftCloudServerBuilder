/*
 * org.nrg.xdat.om.base.BaseXnatStatisticsdataAddfield
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xdat.om.base;

import org.nrg.xdat.om.base.auto.AutoXnatStatisticsdataAddfield;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

import java.util.Hashtable;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatStatisticsdataAddfield extends AutoXnatStatisticsdataAddfield {

	public BaseXnatStatisticsdataAddfield(ItemI item)
	{
		super(item);
	}

	public BaseXnatStatisticsdataAddfield(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatStatisticsdataAddfield(UserI user)
	 **/
	public BaseXnatStatisticsdataAddfield()
	{}

	public BaseXnatStatisticsdataAddfield(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
