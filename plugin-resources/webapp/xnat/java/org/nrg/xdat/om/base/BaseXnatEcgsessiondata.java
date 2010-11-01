// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Oct 07 17:09:42 CDT 2008
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatEcgsessiondata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatEcgsessiondata extends AutoXnatEcgsessiondata {

	public BaseXnatEcgsessiondata(ItemI item)
	{
		super(item);
	}

	public BaseXnatEcgsessiondata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatEcgsessiondata(UserI user)
	 **/
	public BaseXnatEcgsessiondata()
	{}

	public BaseXnatEcgsessiondata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
