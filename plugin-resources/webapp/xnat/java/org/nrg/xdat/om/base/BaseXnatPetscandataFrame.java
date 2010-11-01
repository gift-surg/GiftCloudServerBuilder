// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Feb 06 10:09:20 CST 2007
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatPetscandataFrame;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BaseXnatPetscandataFrame extends AutoXnatPetscandataFrame {

	public BaseXnatPetscandataFrame(ItemI item)
	{
		super(item);
	}

	public BaseXnatPetscandataFrame(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatPetscandataFrame(UserI user)
	 **/
	public BaseXnatPetscandataFrame()
	{}

	public BaseXnatPetscandataFrame(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

}
