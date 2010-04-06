// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xdat.om.base;

import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatPetqcscandata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

public abstract class BaseXnatPetqcscandata extends AutoXnatPetqcscandata {
	public BaseXnatPetqcscandata(ItemI item) {
		super(item);
	}

	public BaseXnatPetqcscandata(UserI user) {
		super(user);
	}

	public BaseXnatPetqcscandata() {
	}

	public BaseXnatPetqcscandata(Hashtable properties, UserI user) {
		super(properties, user);
	}
}