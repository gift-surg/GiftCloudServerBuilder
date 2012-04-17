package org.nrg.xdat.om.base;

import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatGenericdata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;


public class BaseXnatGenericdata extends AutoXnatGenericdata{
	public BaseXnatGenericdata(ItemI item)
	{
		super(item);
	}

	public BaseXnatGenericdata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatHdscandata(UserI user)
	 **/
	public BaseXnatGenericdata()
	{}

	public BaseXnatGenericdata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}
}
