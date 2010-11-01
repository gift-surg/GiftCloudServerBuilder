// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Oct 07 17:09:43 CDT 2008
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatNmsessiondata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatNmsessiondata extends AutoXnatNmsessiondata {

	public BaseXnatNmsessiondata(ItemI item)
	{
		super(item);
	}

	public BaseXnatNmsessiondata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatNmsessiondata(UserI user)
	 **/
	public BaseXnatNmsessiondata()
	{}

	public BaseXnatNmsessiondata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
