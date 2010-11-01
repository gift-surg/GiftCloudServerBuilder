// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Oct 07 17:09:44 CDT 2008
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatGmsessiondata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatGmsessiondata extends AutoXnatGmsessiondata {

	public BaseXnatGmsessiondata(ItemI item)
	{
		super(item);
	}

	public BaseXnatGmsessiondata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatGmsessiondata(UserI user)
	 **/
	public BaseXnatGmsessiondata()
	{}

	public BaseXnatGmsessiondata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
