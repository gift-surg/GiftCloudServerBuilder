// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Fri Oct 06 12:11:07 CDT 2006
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatStudyprotocolVariable;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BaseXnatStudyprotocolVariable extends AutoXnatStudyprotocolVariable {

	public BaseXnatStudyprotocolVariable(ItemI item)
	{
		super(item);
	}

	public BaseXnatStudyprotocolVariable(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatStudyprotocolVariable(UserI user)
	 **/
	public BaseXnatStudyprotocolVariable()
	{}

	public BaseXnatStudyprotocolVariable(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
