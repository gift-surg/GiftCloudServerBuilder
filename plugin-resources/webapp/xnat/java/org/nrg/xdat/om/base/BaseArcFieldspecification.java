// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Wed Sep 12 10:22:24 CDT 2007
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
public abstract class BaseArcFieldspecification extends AutoArcFieldspecification {

	public BaseArcFieldspecification(ItemI item)
	{
		super(item);
	}

	public BaseArcFieldspecification(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseArcFieldspecification(UserI user)
	 **/
	public BaseArcFieldspecification()
	{}

	public BaseArcFieldspecification(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
