// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Aug 07 11:23:27 CDT 2007
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoArcProjectPipeline;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseArcProjectPipeline extends AutoArcProjectPipeline {

	public BaseArcProjectPipeline(ItemI item)
	{
		super(item);
	}

	public BaseArcProjectPipeline(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseArcProjectPipeline(UserI user)
	 **/
	public BaseArcProjectPipeline()
	{}

	public BaseArcProjectPipeline(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

    public boolean hasCustomwebpage() {
        boolean rtn = false;
        if (getCustomwebpage()!= null) {
            rtn = true;
        }
        return rtn;
    }


}
