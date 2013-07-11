/*
 * org.nrg.xdat.om.base.BaseXnatStudyprotocolCondition
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xdat.om.base;

import org.nrg.xdat.om.base.auto.AutoXnatStudyprotocolCondition;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

import java.util.Hashtable;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BaseXnatStudyprotocolCondition extends AutoXnatStudyprotocolCondition {

	public BaseXnatStudyprotocolCondition(ItemI item)
	{
		super(item);
	}

	public BaseXnatStudyprotocolCondition(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatStudyprotocolCondition(UserI user)
	 **/
	public BaseXnatStudyprotocolCondition()
	{}

	public BaseXnatStudyprotocolCondition(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
