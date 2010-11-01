// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Wed Aug 08 13:30:23 CDT 2007
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatStatisticsdataAddfield;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatStatisticsdataAddfield extends AutoXnatStatisticsdataAddfield {

	public BaseXnatStatisticsdataAddfield(ItemI item)
	{
		super(item);
	}

	public BaseXnatStatisticsdataAddfield(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatStatisticsdataAddfield(UserI user)
	 **/
	public BaseXnatStatisticsdataAddfield()
	{}

	public BaseXnatStatisticsdataAddfield(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
