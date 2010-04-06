// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Wed Dec 17 11:45:12 CST 2008
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
@SuppressWarnings("serial")
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
