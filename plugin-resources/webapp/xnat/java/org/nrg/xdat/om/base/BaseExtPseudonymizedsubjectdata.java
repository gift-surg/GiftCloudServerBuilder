/*
 * GENERATED FILE
 * Created on Fri Nov 21 09:46:40 GMT 2014
 *
 */
package org.nrg.xdat.om.base;
import org.nrg.xdat.om.base.auto.*;
import org.nrg.xft.*;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.turbine.utils.ArchivableItem;

import java.util.*;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseExtPseudonymizedsubjectdata extends AutoExtPseudonymizedsubjectdata {

	public BaseExtPseudonymizedsubjectdata(ItemI item)
	{
		super(item);
	}

	public BaseExtPseudonymizedsubjectdata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseExtPseudonymizedsubjectdata(UserI user)
	 **/
	public BaseExtPseudonymizedsubjectdata()
	{}

	public BaseExtPseudonymizedsubjectdata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
