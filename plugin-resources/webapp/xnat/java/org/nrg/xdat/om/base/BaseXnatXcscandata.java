// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Oct 07 17:09:45 CDT 2008
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatXcscandata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatXcscandata extends AutoXnatXcscandata {

	public BaseXnatXcscandata(ItemI item)
	{
		super(item);
	}

	public BaseXnatXcscandata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatXcscandata(UserI user)
	 **/
	public BaseXnatXcscandata()
	{}

	public BaseXnatXcscandata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
