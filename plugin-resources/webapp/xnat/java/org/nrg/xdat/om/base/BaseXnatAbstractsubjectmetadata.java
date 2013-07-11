/*
 * org.nrg.xdat.om.base.BaseXnatAbstractsubjectmetadata
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xdat.om.base;

import org.nrg.xdat.om.base.auto.AutoXnatAbstractsubjectmetadata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

import java.util.Hashtable;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BaseXnatAbstractsubjectmetadata extends AutoXnatAbstractsubjectmetadata {

	public BaseXnatAbstractsubjectmetadata(ItemI item)
	{
		super(item);
	}

	public BaseXnatAbstractsubjectmetadata(UserI user)
	{
		super(user);
	}

	public BaseXnatAbstractsubjectmetadata()
	{}

	public BaseXnatAbstractsubjectmetadata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
