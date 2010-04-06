// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Wed Dec 06 14:45:30 CST 2006
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
public class BaseXnatComputationdata extends AutoXnatComputationdata {

	public BaseXnatComputationdata(ItemI item)
	{
		super(item);
	}

	public BaseXnatComputationdata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatComputationdata(UserI user)
	 **/
	public BaseXnatComputationdata()
	{}

	public BaseXnatComputationdata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
