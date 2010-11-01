// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Oct 07 17:09:45 CDT 2008
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatOpscandata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatOpscandata extends AutoXnatOpscandata {

	public BaseXnatOpscandata(ItemI item)
	{
		super(item);
	}

	public BaseXnatOpscandata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatOpscandata(UserI user)
	 **/
	public BaseXnatOpscandata()
	{}

	public BaseXnatOpscandata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
