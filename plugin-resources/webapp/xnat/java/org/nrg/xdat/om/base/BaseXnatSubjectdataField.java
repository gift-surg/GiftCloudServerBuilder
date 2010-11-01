// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Wed May 16 11:09:02 CDT 2007
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatSubjectdataField;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatSubjectdataField extends AutoXnatSubjectdataField {

	public BaseXnatSubjectdataField(ItemI item)
	{
		super(item);
	}

	public BaseXnatSubjectdataField(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatSubjectdataField(UserI user)
	 **/
	public BaseXnatSubjectdataField()
	{}

	public BaseXnatSubjectdataField(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
