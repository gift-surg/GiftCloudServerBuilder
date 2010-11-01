//Copyright 2005 Harvard University / Howard Hughes Medical Institute (HHMI) All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Aug 16 15:08:18 CDT 2005
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoProvProcessstepLibrary;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BaseProvProcessstepLibrary extends AutoProvProcessstepLibrary {

	public BaseProvProcessstepLibrary(ItemI item)
	{
		super(item);
	}

	public BaseProvProcessstepLibrary(UserI user)
	{
		super(user);
	}

	public BaseProvProcessstepLibrary()
	{}

	public BaseProvProcessstepLibrary(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
