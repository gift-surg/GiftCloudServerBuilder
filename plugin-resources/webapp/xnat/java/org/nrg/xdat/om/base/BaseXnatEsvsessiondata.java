// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Oct 07 17:09:44 CDT 2008
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatEsvsessiondata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatEsvsessiondata extends AutoXnatEsvsessiondata {

	public BaseXnatEsvsessiondata(ItemI item)
	{
		super(item);
	}

	public BaseXnatEsvsessiondata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatEsvsessiondata(UserI user)
	 **/
	public BaseXnatEsvsessiondata()
	{}

	public BaseXnatEsvsessiondata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
