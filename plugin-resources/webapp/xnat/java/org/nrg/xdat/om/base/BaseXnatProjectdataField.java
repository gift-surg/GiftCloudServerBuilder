// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Mon May 21 12:22:29 CDT 2007
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
