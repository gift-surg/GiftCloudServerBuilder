// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Thu Feb 07 12:40:26 CST 2008
 *
 */
package org.nrg.xdat.om.base;
import org.nrg.xdat.om.base.auto.*;
import org.nrg.xft.*;
import org.nrg.xft.security.UserI;

import java.util.*;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatCtscandata extends AutoXnatCtscandata {

	public BaseXnatCtscandata(ItemI item)
	{
		super(item);
	}

	public BaseXnatCtscandata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatCtscandata(UserI user)
	 **/
	public BaseXnatCtscandata()
	{}

	public BaseXnatCtscandata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}

