// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Mon Oct 13 13:15:44 CDT 2008
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
public abstract class BaseXnatImagescandataShare extends AutoXnatImagescandataShare {

	public BaseXnatImagescandataShare(ItemI item)
	{
		super(item);
	}

	public BaseXnatImagescandataShare(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatImagescandataShare(UserI user)
	 **/
	public BaseXnatImagescandataShare()
	{}

	public BaseXnatImagescandataShare(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
