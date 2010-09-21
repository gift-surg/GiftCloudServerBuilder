// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Wed Aug 08 13:30:23 CDT 2007
 *
 */
package org.nrg.xdat.om.base;
import java.util.ArrayList;
import java.util.Hashtable;

import org.nrg.xdat.om.XnatStatisticsdataAddfield;
import org.nrg.xdat.om.XnatStatisticsdataAdditionalstatistics;
import org.nrg.xdat.om.base.auto.AutoXnatStatisticsdata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatStatisticsdata extends AutoXnatStatisticsdata {

	public BaseXnatStatisticsdata(ItemI item)
	{
		super(item);
	}

	public BaseXnatStatisticsdata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatStatisticsdata(UserI user)
	 **/
	public BaseXnatStatisticsdata()
	{}

	public BaseXnatStatisticsdata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}
	
	public XnatStatisticsdataAdditionalstatistics getAdditionalstatistics(String name) {
		XnatStatisticsdataAdditionalstatistics rtn = null;
		ArrayList<XnatStatisticsdataAdditionalstatistics> additionalStats = getAdditionalstatistics() ;
		for (int i = 0; i < additionalStats.size(); i++) {
			if (additionalStats.get(i).getName().equals(name)) {
				rtn = additionalStats.get(i);
				break;
			}
		}
		return rtn;
	}	

	public XnatStatisticsdataAddfield getAddfield(String name) {
		XnatStatisticsdataAddfield rtn = null;
		ArrayList<XnatStatisticsdataAddfield> additionalFields = getAddfield() ;
		for (int i = 0; i < additionalFields.size(); i++) {
			if (additionalFields.get(i).getName().equals(name)) {
				rtn = additionalFields.get(i);
				break;
			}
		}
		return rtn;
	}


}
