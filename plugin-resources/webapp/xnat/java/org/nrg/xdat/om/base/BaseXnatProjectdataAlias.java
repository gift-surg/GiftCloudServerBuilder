// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Thu Apr 03 13:48:01 CDT 2008
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatProjectdataAlias;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatProjectdataAlias extends AutoXnatProjectdataAlias {

	public BaseXnatProjectdataAlias(ItemI item)
	{
		super(item);
	}

	public BaseXnatProjectdataAlias(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatProjectdataAlias(UserI user)
	 **/
	public BaseXnatProjectdataAlias()
	{}

	public BaseXnatProjectdataAlias(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
