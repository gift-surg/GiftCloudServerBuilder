// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Oct 07 17:09:42 CDT 2008
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatHdscandata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatHdscandata extends AutoXnatHdscandata {

	public BaseXnatHdscandata(ItemI item)
	{
		super(item);
	}

	public BaseXnatHdscandata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatHdscandata(UserI user)
	 **/
	public BaseXnatHdscandata()
	{}

	public BaseXnatHdscandata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
