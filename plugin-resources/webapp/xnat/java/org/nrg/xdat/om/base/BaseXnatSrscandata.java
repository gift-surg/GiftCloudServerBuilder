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
public abstract class BaseXnatSrscandata extends AutoXnatSrscandata {

	public BaseXnatSrscandata(ItemI item)
	{
		super(item);
	}

	public BaseXnatSrscandata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatSrscandata(UserI user)
	 **/
	public BaseXnatSrscandata()
	{}

	public BaseXnatSrscandata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
