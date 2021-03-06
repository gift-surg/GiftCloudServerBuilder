/*
 * org.nrg.xdat.om.base.BaseXnatImageresourceseries
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xdat.om.base;

import org.nrg.xdat.om.base.auto.AutoXnatImageresourceseries;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

import java.util.Hashtable;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class BaseXnatImageresourceseries extends AutoXnatImageresourceseries {

	public BaseXnatImageresourceseries(ItemI item) {
		super(item);
	}

	public BaseXnatImageresourceseries(UserI user) {
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatImageresourceseries(UserI user)
	 */
	public BaseXnatImageresourceseries() {
	}

	public BaseXnatImageresourceseries(Hashtable properties, UserI user) {
		super(properties, user);
	}

}
