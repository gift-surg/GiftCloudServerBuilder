// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Oct 07 17:09:44 CDT 2008
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatEsvscandata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatEsvscandata extends AutoXnatEsvscandata {

	public BaseXnatEsvscandata(ItemI item)
	{
		super(item);
	}

	public BaseXnatEsvscandata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatEsvscandata(UserI user)
	 **/
	public BaseXnatEsvscandata()
	{}

	public BaseXnatEsvscandata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
