// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Oct 07 17:09:43 CDT 2008
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatXa3dscandata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatXa3dscandata extends AutoXnatXa3dscandata {

	public BaseXnatXa3dscandata(ItemI item)
	{
		super(item);
	}

	public BaseXnatXa3dscandata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatXa3dscandata(UserI user)
	 **/
	public BaseXnatXa3dscandata()
	{}

	public BaseXnatXa3dscandata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
