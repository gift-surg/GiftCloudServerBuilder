/*
 * org.nrg.xdat.om.base.BaseArcFieldspecification
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xdat.om.base;

import org.nrg.xdat.om.base.auto.AutoArcFieldspecification;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

import java.util.Hashtable;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseArcFieldspecification extends AutoArcFieldspecification {

	public BaseArcFieldspecification(ItemI item)
	{
		super(item);
	}

	public BaseArcFieldspecification(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseArcFieldspecification(UserI user)
	 **/
	public BaseArcFieldspecification()
	{}

	public BaseArcFieldspecification(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
