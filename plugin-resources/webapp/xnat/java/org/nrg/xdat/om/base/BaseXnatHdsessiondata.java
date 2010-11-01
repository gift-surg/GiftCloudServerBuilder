// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Oct 07 17:09:42 CDT 2008
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatHdsessiondata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatHdsessiondata extends AutoXnatHdsessiondata {

	public BaseXnatHdsessiondata(ItemI item)
	{
		super(item);
	}

	public BaseXnatHdsessiondata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatHdsessiondata(UserI user)
	 **/
	public BaseXnatHdsessiondata()
	{}

	public BaseXnatHdsessiondata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
