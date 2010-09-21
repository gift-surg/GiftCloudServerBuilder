// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Wed Mar 12 15:37:54 CDT 2008
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
public abstract class BaseXnatAybocsdata extends AutoXnatAybocsdata {

	public BaseXnatAybocsdata(ItemI item)
	{
		super(item);
	}

	public BaseXnatAybocsdata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatAybocsdata(UserI user)
	 **/
	public BaseXnatAybocsdata()
	{}

	public BaseXnatAybocsdata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}

