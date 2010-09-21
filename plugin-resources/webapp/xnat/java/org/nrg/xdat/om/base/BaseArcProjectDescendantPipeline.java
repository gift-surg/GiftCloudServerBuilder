// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Thu Aug 23 15:17:36 CDT 2007
 *
 */
package org.nrg.xdat.om.base;
import org.nrg.xdat.om.base.auto.*;
import org.nrg.xft.*;
import org.nrg.xft.security.UserI;

import java.io.File;
import java.util.*;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseArcProjectDescendantPipeline extends AutoArcProjectDescendantPipeline {

	public BaseArcProjectDescendantPipeline(ItemI item)
	{
		super(item);
	}

	public BaseArcProjectDescendantPipeline(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseArcProjectDescendantPipeline(UserI user)
	 **/
	public BaseArcProjectDescendantPipeline()
	{}

	public BaseArcProjectDescendantPipeline(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

    public boolean hasCustomwebpage() {
        boolean rtn = false;
        if (getCustomwebpage() !=null)
            rtn = true;
        return rtn;
    }

}
