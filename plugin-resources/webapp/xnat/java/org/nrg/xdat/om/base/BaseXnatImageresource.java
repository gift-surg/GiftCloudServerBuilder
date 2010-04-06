// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Sep 26 09:10:46 CDT 2006
 *
 */
package org.nrg.xdat.om.base;
import org.nrg.xdat.om.base.auto.*;
import org.nrg.xft.*;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xft.utils.StringUtils;

import java.util.*;

/**
 * @author XDAT
 *
 */
@SuppressWarnings("serial")
public class BaseXnatImageresource extends AutoXnatImageresource {

	public BaseXnatImageresource(ItemI item)
	{
		super(item);
	}

	public BaseXnatImageresource(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatImageresource(UserI user)
	 **/
	public BaseXnatImageresource()
	{}

	public BaseXnatImageresource(Hashtable properties, UserI user)
	{
		super(properties,user);
	}


}
