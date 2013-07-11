/*
 * org.nrg.xdat.om.base.BaseXnatProjectdataField
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xdat.om.base;

import org.nrg.xdat.om.base.auto.AutoXnatProjectdataField;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

import java.util.Hashtable;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatProjectdataField extends AutoXnatProjectdataField {

	public BaseXnatProjectdataField(ItemI item)
	{
		super(item);
	}

	public BaseXnatProjectdataField(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatProjectdataField(UserI user)
	 **/
	public BaseXnatProjectdataField()
	{}

	public BaseXnatProjectdataField(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
