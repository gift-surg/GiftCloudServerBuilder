// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Oct 07 17:09:45 CDT 2008
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatOtherdicomsessiondata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatOtherdicomsessiondata extends AutoXnatOtherdicomsessiondata {

	public BaseXnatOtherdicomsessiondata(ItemI item)
	{
		super(item);
	}

	public BaseXnatOtherdicomsessiondata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatOtherdicomsessiondata(UserI user)
	 **/
	public BaseXnatOtherdicomsessiondata()
	{}

	public BaseXnatOtherdicomsessiondata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
