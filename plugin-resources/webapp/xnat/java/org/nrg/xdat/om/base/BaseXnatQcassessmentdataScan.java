// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Wed Aug 08 13:30:23 CDT 2007
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
public abstract class BaseXnatQcassessmentdataScan extends AutoXnatQcassessmentdataScan {

	public BaseXnatQcassessmentdataScan(ItemI item)
	{
		super(item);
	}

	public BaseXnatQcassessmentdataScan(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatQcassessmentdataScan(UserI user)
	 **/
	public BaseXnatQcassessmentdataScan()
	{}

	public BaseXnatQcassessmentdataScan(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
