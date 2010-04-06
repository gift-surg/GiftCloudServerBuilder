// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xdat.om.base;

import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatQcscandata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

public abstract class BaseXnatQcscandata extends AutoXnatQcscandata {
	public BaseXnatQcscandata(ItemI item) {
		super(item);
	}

	public BaseXnatQcscandata(UserI user) {
		super(user);
	}

	public BaseXnatQcscandata() {
	}

	public BaseXnatQcscandata(Hashtable properties, UserI user) {
		super(properties, user);
	}

}
