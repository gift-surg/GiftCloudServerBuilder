// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Fri Oct 06 12:11:07 CDT 2006
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
