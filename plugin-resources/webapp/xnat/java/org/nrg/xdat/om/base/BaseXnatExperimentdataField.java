// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Fri Jan 26 09:36:20 CST 2007
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
