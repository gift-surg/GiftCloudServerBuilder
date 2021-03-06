/*
 * org.nrg.xdat.om.base.BaseXnatValidationdata
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xdat.om.base;

import org.nrg.xdat.om.base.auto.AutoXnatValidationdata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

import java.util.Hashtable;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class BaseXnatValidationdata extends AutoXnatValidationdata {

	public BaseXnatValidationdata(ItemI item) {
		super(item);
	}

	public BaseXnatValidationdata(UserI user) {
		super(user);
	}

	public BaseXnatValidationdata() {
	}

	public BaseXnatValidationdata(Hashtable properties, UserI user) {
		super(properties, user);
	}

}
