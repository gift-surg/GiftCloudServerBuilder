// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xdat.om.base;

import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatMrqcscandata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatMrqcscandata extends AutoXnatMrqcscandata {
	public BaseXnatMrqcscandata(ItemI item) {
		super(item);
	}

	public BaseXnatMrqcscandata(UserI user) {
		super(user);
	}

	public BaseXnatMrqcscandata() {
	}

	public BaseXnatMrqcscandata(Hashtable properties, UserI user) {
		super(properties, user);
	}
}
