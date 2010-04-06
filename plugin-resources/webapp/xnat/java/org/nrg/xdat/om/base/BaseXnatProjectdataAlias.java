// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Thu Apr 03 13:48:01 CDT 2008
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
public abstract class BaseXnatProjectdataAlias extends AutoXnatProjectdataAlias {

	public BaseXnatProjectdataAlias(ItemI item)
	{
		super(item);
	}

	public BaseXnatProjectdataAlias(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatProjectdataAlias(UserI user)
	 **/
	public BaseXnatProjectdataAlias()
	{}

	public BaseXnatProjectdataAlias(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
