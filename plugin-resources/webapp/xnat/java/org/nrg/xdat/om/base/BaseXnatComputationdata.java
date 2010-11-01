// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Wed Dec 06 14:45:30 CST 2006
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatComputationdata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BaseXnatComputationdata extends AutoXnatComputationdata {

	public BaseXnatComputationdata(ItemI item)
	{
		super(item);
	}

	public BaseXnatComputationdata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatComputationdata(UserI user)
	 **/
	public BaseXnatComputationdata()
	{}

	public BaseXnatComputationdata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
