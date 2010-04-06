// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xdat.om.base;

import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatQcmanualassessordata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

public abstract class BaseXnatQcmanualassessordata extends AutoXnatQcmanualassessordata {
	public BaseXnatQcmanualassessordata(ItemI item) {
		super(item);
	}

	public BaseXnatQcmanualassessordata(UserI user) {
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatQcmanualassessordata(UserI user)
	 */
	public BaseXnatQcmanualassessordata() {
	}

	public BaseXnatQcmanualassessordata(Hashtable properties, UserI user) {
		super(properties, user);
	}
}
