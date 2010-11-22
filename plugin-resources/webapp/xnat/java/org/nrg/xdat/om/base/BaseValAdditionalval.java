/*
 * GENERATED FILE
 * Created on Mon Nov 22 10:20:49 CST 2010
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
public abstract class BaseValAdditionalval extends AutoValAdditionalval {

	public BaseValAdditionalval(ItemI item)
	{
		super(item);
	}

	public BaseValAdditionalval(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseValAdditionalval(UserI user)
	 **/
	public BaseValAdditionalval()
	{}

	public BaseValAdditionalval(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
