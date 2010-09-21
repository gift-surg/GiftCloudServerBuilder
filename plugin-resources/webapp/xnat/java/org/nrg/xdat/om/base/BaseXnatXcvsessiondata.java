// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Oct 07 17:09:45 CDT 2008
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
public abstract class BaseXnatXcvsessiondata extends AutoXnatXcvsessiondata {

	public BaseXnatXcvsessiondata(ItemI item)
	{
		super(item);
	}

	public BaseXnatXcvsessiondata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatXcvsessiondata(UserI user)
	 **/
	public BaseXnatXcvsessiondata()
	{}

	public BaseXnatXcvsessiondata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
