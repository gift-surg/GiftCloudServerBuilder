/*
 * org.nrg.xdat.om.base.BaseXnatStudyprotocol
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xdat.om.base;

import org.nrg.xdat.om.base.auto.AutoXnatStudyprotocol;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

import java.util.Hashtable;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BaseXnatStudyprotocol extends AutoXnatStudyprotocol {

	public BaseXnatStudyprotocol(ItemI item)
	{
		super(item);
	}

	public BaseXnatStudyprotocol(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatStudyprotocol(UserI user)
	 **/
	public BaseXnatStudyprotocol()
	{}

	public BaseXnatStudyprotocol(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
