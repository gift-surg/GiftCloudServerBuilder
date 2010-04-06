// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Oct 07 17:09:43 CDT 2008
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
public abstract class BaseXnatRfsessiondata extends AutoXnatRfsessiondata {

	public BaseXnatRfsessiondata(ItemI item)
	{
		super(item);
	}

	public BaseXnatRfsessiondata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatRfsessiondata(UserI user)
	 **/
	public BaseXnatRfsessiondata()
	{}

	public BaseXnatRfsessiondata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
