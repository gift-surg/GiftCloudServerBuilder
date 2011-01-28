// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xdat.om.base;

import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatMegscandata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

@SuppressWarnings({"unchecked","rawtypes"})
public class BaseXnatMegscandata extends AutoXnatMegscandata {

	public BaseXnatMegscandata(ItemI item) {
		super(item);
	}

	public BaseXnatMegscandata(UserI user) {
		super(user);
	}

	public BaseXnatMegscandata() {
	}

	public BaseXnatMegscandata(Hashtable properties, UserI user) {
		super(properties, user);
	}

}
