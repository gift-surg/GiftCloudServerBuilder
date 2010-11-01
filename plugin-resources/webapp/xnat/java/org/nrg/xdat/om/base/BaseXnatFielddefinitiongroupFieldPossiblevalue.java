// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Wed Nov 28 15:02:06 CST 2007
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatFielddefinitiongroupFieldPossiblevalue;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatFielddefinitiongroupFieldPossiblevalue extends AutoXnatFielddefinitiongroupFieldPossiblevalue {

	public BaseXnatFielddefinitiongroupFieldPossiblevalue(ItemI item)
	{
		super(item);
	}

	public BaseXnatFielddefinitiongroupFieldPossiblevalue(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatFielddefinitiongroupFieldPossiblevalue(UserI user)
	 **/
	public BaseXnatFielddefinitiongroupFieldPossiblevalue()
	{}

	public BaseXnatFielddefinitiongroupFieldPossiblevalue(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
