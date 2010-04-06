// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xdat.om.base;

import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatMegscandata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

public class BaseXnatMegscandata extends AutoXnatMegscandata {

	public BaseXnatMegscandata(ItemI item) {
		super(item);
		// TODO Auto-generated constructor stub
	}

	public BaseXnatMegscandata(UserI user) {
		super(user);
		// TODO Auto-generated constructor stub
	}

	public BaseXnatMegscandata() {
		// TODO Auto-generated constructor stub
	}

	public BaseXnatMegscandata(Hashtable properties, UserI user) {
		super(properties, user);
		// TODO Auto-generated constructor stub
	}

}
