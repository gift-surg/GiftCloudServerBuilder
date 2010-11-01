// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Oct 07 17:09:41 CDT 2008
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatIoscandata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatIoscandata extends AutoXnatIoscandata {

	public BaseXnatIoscandata(ItemI item)
	{
		super(item);
	}

	public BaseXnatIoscandata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatIoscandata(UserI user)
	 **/
	public BaseXnatIoscandata()
	{}

	public BaseXnatIoscandata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
