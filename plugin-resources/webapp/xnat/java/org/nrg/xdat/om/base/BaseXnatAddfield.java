//Copyright 2005 Harvard University / Howard Hughes Medical Institute (HHMI) All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Aug 16 15:08:17 CDT 2005
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatAddfield;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BaseXnatAddfield extends AutoXnatAddfield {

	public BaseXnatAddfield(ItemI item)
	{
		super(item);
	}

	public BaseXnatAddfield(UserI user)
	{
		super(user);
	}

	public BaseXnatAddfield()
	{}

	public BaseXnatAddfield(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
