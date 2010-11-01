// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Fri Oct 06 12:11:06 CDT 2006
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatSubjectvariablesdata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BaseXnatSubjectvariablesdata extends AutoXnatSubjectvariablesdata {

	public BaseXnatSubjectvariablesdata(ItemI item)
	{
		super(item);
	}

	public BaseXnatSubjectvariablesdata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatSubjectvariablesdata(UserI user)
	 **/
	public BaseXnatSubjectvariablesdata()
	{}

	public BaseXnatSubjectvariablesdata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
