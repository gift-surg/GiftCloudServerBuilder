// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Wed Dec 06 09:45:34 CST 2006
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;
import java.util.Iterator;

import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.base.auto.AutoXnatPetscandata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BaseXnatPetscandata extends AutoXnatPetscandata {
	public BaseXnatPetscandata(ItemI item)
	{
		super(item);
	}

	public BaseXnatPetscandata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatPetscandata(UserI user)
	 **/
	public BaseXnatPetscandata()
	{}

	public BaseXnatPetscandata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}


    public boolean isInRAWDirectory(){
        boolean hasRAW=false;
        Iterator files = getFile().iterator();
        while (files.hasNext()){
            XnatAbstractresource file = (XnatAbstractresource)files.next();
            if (file.isInRAWDirectory())
            {
                hasRAW=true;
                break;
            }
        }
        return hasRAW;
    }
}
