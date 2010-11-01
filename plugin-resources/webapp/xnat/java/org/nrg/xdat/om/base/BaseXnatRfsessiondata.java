// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Oct 07 17:09:43 CDT 2008
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatRfsessiondata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatRfsessiondata extends AutoXnatRfsessiondata {

	public BaseXnatRfsessiondata(ItemI item)
	{
		super(item);
	}

	public BaseXnatRfsessiondata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatRfsessiondata(UserI user)
	 **/
	public BaseXnatRfsessiondata()
	{}

	public BaseXnatRfsessiondata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
