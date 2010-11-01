// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Thu May 17 10:21:30 CDT 2007
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoWrkAbstractexecutionenvironment;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseWrkAbstractexecutionenvironment extends AutoWrkAbstractexecutionenvironment {

	public BaseWrkAbstractexecutionenvironment(ItemI item)
	{
		super(item);
	}

	public BaseWrkAbstractexecutionenvironment(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseWrkAbstractexecutionenvironment(UserI user)
	 **/
	public BaseWrkAbstractexecutionenvironment()
	{}

	public BaseWrkAbstractexecutionenvironment(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
