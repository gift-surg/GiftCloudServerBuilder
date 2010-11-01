// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Wed Dec 06 14:45:30 CST 2006
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatRegionresource;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BaseXnatRegionresource extends AutoXnatRegionresource {

	public BaseXnatRegionresource(ItemI item)
	{
		super(item);
	}

	public BaseXnatRegionresource(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatRegionresource(UserI user)
	 **/
	public BaseXnatRegionresource()
	{}

	public BaseXnatRegionresource(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
