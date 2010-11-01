// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Oct 07 17:09:42 CDT 2008
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatEcgscandata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatEcgscandata extends AutoXnatEcgscandata {

	public BaseXnatEcgscandata(ItemI item)
	{
		super(item);
	}

	public BaseXnatEcgscandata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatEcgscandata(UserI user)
	 **/
	public BaseXnatEcgscandata()
	{}

	public BaseXnatEcgscandata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
