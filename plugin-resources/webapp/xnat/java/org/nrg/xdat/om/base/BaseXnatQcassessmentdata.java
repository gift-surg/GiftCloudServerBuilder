// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Wed Aug 08 13:30:23 CDT 2007
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatQcassessmentdata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatQcassessmentdata extends AutoXnatQcassessmentdata {

	public BaseXnatQcassessmentdata(ItemI item)
	{
		super(item);
	}

	public BaseXnatQcassessmentdata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatQcassessmentdata(UserI user)
	 **/
	public BaseXnatQcassessmentdata()
	{}

	public BaseXnatQcassessmentdata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
