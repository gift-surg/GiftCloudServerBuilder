// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Fri Oct 06 12:11:07 CDT 2006
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatStudyprotocol;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BaseXnatStudyprotocol extends AutoXnatStudyprotocol {

	public BaseXnatStudyprotocol(ItemI item)
	{
		super(item);
	}

	public BaseXnatStudyprotocol(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatStudyprotocol(UserI user)
	 **/
	public BaseXnatStudyprotocol()
	{}

	public BaseXnatStudyprotocol(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
