// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Oct 07 17:09:43 CDT 2008
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatEssessiondata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatEssessiondata extends AutoXnatEssessiondata {

	public BaseXnatEssessiondata(ItemI item)
	{
		super(item);
	}

	public BaseXnatEssessiondata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatEssessiondata(UserI user)
	 **/
	public BaseXnatEssessiondata()
	{}

	public BaseXnatEssessiondata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
