// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Sep 26 09:51:05 CDT 2006
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
public class BaseXnatImageresourceseries extends AutoXnatImageresourceseries {

	public BaseXnatImageresourceseries(ItemI item)
	{
		super(item);
	}

	public BaseXnatImageresourceseries(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatImageresourceseries(UserI user)
	 **/
	public BaseXnatImageresourceseries()
	{}

	public BaseXnatImageresourceseries(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
