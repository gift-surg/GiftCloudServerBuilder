// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Wed Dec 17 11:45:12 CST 2008
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatAbstractresourceTag;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatAbstractresourceTag extends AutoXnatAbstractresourceTag {

	public BaseXnatAbstractresourceTag(ItemI item)
	{
		super(item);
	}

	public BaseXnatAbstractresourceTag(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatAbstractresourceTag(UserI user)
	 **/
	public BaseXnatAbstractresourceTag()
	{}

	public BaseXnatAbstractresourceTag(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
