/*
 * GENERATED FILE
 * Created on Thu Jan 31 10:55:11 CST 2013
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
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatSrsessiondata extends AutoXnatSrsessiondata {

	public BaseXnatSrsessiondata(ItemI item)
	{
		super(item);
	}

	public BaseXnatSrsessiondata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatSrsessiondata(UserI user)
	 **/
	public BaseXnatSrsessiondata()
	{}

	public BaseXnatSrsessiondata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
