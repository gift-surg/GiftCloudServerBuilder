// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Oct 07 17:09:42 CDT 2008
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatEegscandata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatEegscandata extends AutoXnatEegscandata {

	public BaseXnatEegscandata(ItemI item)
	{
		super(item);
	}

	public BaseXnatEegscandata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatEcgscandata(UserI user)
	 **/
	public BaseXnatEegscandata()
	{}

	public BaseXnatEegscandata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
