// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Oct 07 17:09:43 CDT 2008
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
public abstract class BaseXnatXa3dsessiondata extends AutoXnatXa3dsessiondata {

	public BaseXnatXa3dsessiondata(ItemI item)
	{
		super(item);
	}

	public BaseXnatXa3dsessiondata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatXa3dsessiondata(UserI user)
	 **/
	public BaseXnatXa3dsessiondata()
	{}

	public BaseXnatXa3dsessiondata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
