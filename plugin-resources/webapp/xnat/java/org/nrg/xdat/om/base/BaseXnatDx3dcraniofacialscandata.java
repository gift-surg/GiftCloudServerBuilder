// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Oct 07 17:09:43 CDT 2008
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatDx3dcraniofacialscandata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatDx3dcraniofacialscandata extends AutoXnatDx3dcraniofacialscandata {

	public BaseXnatDx3dcraniofacialscandata(ItemI item)
	{
		super(item);
	}

	public BaseXnatDx3dcraniofacialscandata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatDx3dcraniofacialscandata(UserI user)
	 **/
	public BaseXnatDx3dcraniofacialscandata()
	{}

	public BaseXnatDx3dcraniofacialscandata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
