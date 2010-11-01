// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Oct 07 17:09:43 CDT 2008
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatNmscandata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatNmscandata extends AutoXnatNmscandata {

	public BaseXnatNmscandata(ItemI item)
	{
		super(item);
	}

	public BaseXnatNmscandata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatNmscandata(UserI user)
	 **/
	public BaseXnatNmscandata()
	{}

	public BaseXnatNmscandata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
