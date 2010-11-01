// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Fri Jan 26 09:36:20 CST 2007
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatExperimentdataField;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BaseXnatExperimentdataField extends AutoXnatExperimentdataField {

	public BaseXnatExperimentdataField(ItemI item)
	{
		super(item);
	}

	public BaseXnatExperimentdataField(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatExperimentdataField(UserI user)
	 **/
	public BaseXnatExperimentdataField()
	{}

	public BaseXnatExperimentdataField(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
