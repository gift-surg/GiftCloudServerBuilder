// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Thu Dec 14 10:09:32 CST 2006
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatRegionresourceLabel;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BaseXnatRegionresourceLabel extends AutoXnatRegionresourceLabel {

	public BaseXnatRegionresourceLabel(ItemI item)
	{
		super(item);
	}

	public BaseXnatRegionresourceLabel(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatRegionresourceLabel(UserI user)
	 **/
	public BaseXnatRegionresourceLabel()
	{}

	public BaseXnatRegionresourceLabel(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
