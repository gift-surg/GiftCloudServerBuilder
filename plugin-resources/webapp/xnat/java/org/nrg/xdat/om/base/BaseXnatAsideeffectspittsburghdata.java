// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Wed Mar 12 15:37:54 CDT 2008
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatAsideeffectspittsburghdata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatAsideeffectspittsburghdata extends AutoXnatAsideeffectspittsburghdata {

	public BaseXnatAsideeffectspittsburghdata(ItemI item)
	{
		super(item);
	}

	public BaseXnatAsideeffectspittsburghdata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatAsideeffectspittsburghdata(UserI user)
	 **/
	public BaseXnatAsideeffectspittsburghdata()
	{}

	public BaseXnatAsideeffectspittsburghdata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}

