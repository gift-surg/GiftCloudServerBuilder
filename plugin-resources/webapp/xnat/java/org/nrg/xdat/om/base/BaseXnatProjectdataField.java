// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Mon May 21 12:22:29 CDT 2007
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatProjectdataField;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatProjectdataField extends AutoXnatProjectdataField {

	public BaseXnatProjectdataField(ItemI item)
	{
		super(item);
	}

	public BaseXnatProjectdataField(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatProjectdataField(UserI user)
	 **/
	public BaseXnatProjectdataField()
	{}

	public BaseXnatProjectdataField(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
