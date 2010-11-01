// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Oct 07 17:09:44 CDT 2008
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatGmvsessiondata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatGmvsessiondata extends AutoXnatGmvsessiondata {

	public BaseXnatGmvsessiondata(ItemI item)
	{
		super(item);
	}

	public BaseXnatGmvsessiondata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatGmvsessiondata(UserI user)
	 **/
	public BaseXnatGmvsessiondata()
	{}

	public BaseXnatGmvsessiondata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
