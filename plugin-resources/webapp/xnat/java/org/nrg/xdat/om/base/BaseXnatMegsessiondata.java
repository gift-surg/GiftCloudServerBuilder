// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xdat.om.base;

import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatMegsessiondata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

@SuppressWarnings({"unchecked","rawtypes"})
public class BaseXnatMegsessiondata extends AutoXnatMegsessiondata {

	public BaseXnatMegsessiondata(ItemI item) {
		super(item);
	}

	public BaseXnatMegsessiondata(UserI user) {
		super(user);
	}

	public BaseXnatMegsessiondata() {
	}

	public BaseXnatMegsessiondata(Hashtable properties, UserI user) {
		super(properties, user);
	}

}
