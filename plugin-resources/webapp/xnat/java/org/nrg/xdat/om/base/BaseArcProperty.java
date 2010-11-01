// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Aug 07 11:23:27 CDT 2007
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoArcProperty;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseArcProperty extends AutoArcProperty {

	public BaseArcProperty(ItemI item)
	{
		super(item);
	}

	public BaseArcProperty(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseArcProperty(UserI user)
	 **/
	public BaseArcProperty()
	{}

	public BaseArcProperty(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
