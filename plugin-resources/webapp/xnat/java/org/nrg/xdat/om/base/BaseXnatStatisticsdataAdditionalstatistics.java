// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Wed Aug 08 13:30:23 CDT 2007
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatStatisticsdataAdditionalstatistics;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatStatisticsdataAdditionalstatistics extends AutoXnatStatisticsdataAdditionalstatistics {

	public BaseXnatStatisticsdataAdditionalstatistics(ItemI item)
	{
		super(item);
	}

	public BaseXnatStatisticsdataAdditionalstatistics(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatStatisticsdataAdditionalstatistics(UserI user)
	 **/
	public BaseXnatStatisticsdataAdditionalstatistics()
	{}

	public BaseXnatStatisticsdataAdditionalstatistics(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
