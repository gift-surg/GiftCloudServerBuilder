// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Oct 07 17:09:44 CDT 2008
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
public abstract class BaseXnatEsscandata extends AutoXnatEsscandata {

	public BaseXnatEsscandata(ItemI item)
	{
		super(item);
	}

	public BaseXnatEsscandata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatEsscandata(UserI user)
	 **/
	public BaseXnatEsscandata()
	{}

	public BaseXnatEsscandata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
