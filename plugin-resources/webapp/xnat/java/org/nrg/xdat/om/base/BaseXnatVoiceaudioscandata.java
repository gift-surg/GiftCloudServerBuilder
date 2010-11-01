// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Oct 07 17:09:46 CDT 2008
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatVoiceaudioscandata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatVoiceaudioscandata extends AutoXnatVoiceaudioscandata {

	public BaseXnatVoiceaudioscandata(ItemI item)
	{
		super(item);
	}

	public BaseXnatVoiceaudioscandata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatVoiceaudioscandata(UserI user)
	 **/
	public BaseXnatVoiceaudioscandata()
	{}

	public BaseXnatVoiceaudioscandata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
