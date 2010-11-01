// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Wed Dec 06 09:45:34 CST 2006
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.XnatPetsessiondata;
import org.nrg.xdat.om.base.auto.AutoXnatPetassessordata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BaseXnatPetassessordata extends AutoXnatPetassessordata {

	public BaseXnatPetassessordata(ItemI item)
	{
		super(item);
	}

	public BaseXnatPetassessordata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatPetassessordata(UserI user)
	 **/
	public BaseXnatPetassessordata()
	{}

	public BaseXnatPetassessordata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}



    public XnatPetsessiondata getPetSessionData()
    {
        return (XnatPetsessiondata)this.getImageSessionData();
    }
}
