// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Wed Mar 12 15:37:54 CDT 2008
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
public abstract class BaseXnatAupdrs3data extends AutoXnatAupdrs3data {

	public BaseXnatAupdrs3data(ItemI item)
	{
		super(item);
	}

	public BaseXnatAupdrs3data(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatAupdrs3data(UserI user)
	 **/
	public BaseXnatAupdrs3data()
	{}

	public BaseXnatAupdrs3data(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}

