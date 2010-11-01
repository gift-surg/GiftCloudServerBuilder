// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Oct 07 17:09:41 CDT 2008
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatUsscandata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatUsscandata extends AutoXnatUsscandata {

	public BaseXnatUsscandata(ItemI item)
	{
		super(item);
	}

	public BaseXnatUsscandata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatUsscandata(UserI user)
	 **/
	public BaseXnatUsscandata()
	{}

	public BaseXnatUsscandata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
