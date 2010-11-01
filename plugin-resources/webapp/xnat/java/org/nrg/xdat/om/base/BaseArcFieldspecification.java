// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Wed Sep 12 10:22:24 CDT 2007
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoArcFieldspecification;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseArcFieldspecification extends AutoArcFieldspecification {

	public BaseArcFieldspecification(ItemI item)
	{
		super(item);
	}

	public BaseArcFieldspecification(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseArcFieldspecification(UserI user)
	 **/
	public BaseArcFieldspecification()
	{}

	public BaseArcFieldspecification(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
