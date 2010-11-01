// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Wed Aug 08 13:30:23 CDT 2007
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatAbstractstatistics;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatAbstractstatistics extends AutoXnatAbstractstatistics {

	public BaseXnatAbstractstatistics(ItemI item)
	{
		super(item);
	}

	public BaseXnatAbstractstatistics(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatAbstractstatistics(UserI user)
	 **/
	public BaseXnatAbstractstatistics()
	{}

	public BaseXnatAbstractstatistics(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
