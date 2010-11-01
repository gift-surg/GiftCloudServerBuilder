// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Thu May 17 10:21:31 CDT 2007
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoWrkXnatexecutionenvironmentNotify;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseWrkXnatexecutionenvironmentNotify extends AutoWrkXnatexecutionenvironmentNotify {

	public BaseWrkXnatexecutionenvironmentNotify(ItemI item)
	{
		super(item);
	}

	public BaseWrkXnatexecutionenvironmentNotify(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseWrkXnatexecutionenvironmentNotify(UserI user)
	 **/
	public BaseWrkXnatexecutionenvironmentNotify()
	{}

	public BaseWrkXnatexecutionenvironmentNotify(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
