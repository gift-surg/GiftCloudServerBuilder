/*
 * org.nrg.xdat.om.base.BaseXnatInvestigatordata
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xdat.om.base;

import org.nrg.xdat.om.base.auto.AutoXnatInvestigatordata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

import java.util.Hashtable;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BaseXnatInvestigatordata extends AutoXnatInvestigatordata {

	public BaseXnatInvestigatordata(ItemI item)
	{
		super(item);
	}

	public BaseXnatInvestigatordata(UserI user)
	{
		super(user);
	}

	public BaseXnatInvestigatordata()
	{}

	public BaseXnatInvestigatordata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
