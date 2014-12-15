/*
 * org.nrg.xdat.om.base.BaseXnatPetassessordata
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xdat.om.base;

import org.nrg.xdat.om.XnatPetsessiondata;
import org.nrg.xdat.om.base.auto.AutoXnatPetassessordata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

import java.util.Hashtable;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class BaseXnatPetassessordata extends AutoXnatPetassessordata {

	public BaseXnatPetassessordata(ItemI item) {
		super(item);
	}

	public BaseXnatPetassessordata(UserI user) {
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatPetassessordata(UserI user)
	 */
	public BaseXnatPetassessordata() {
	}

	public BaseXnatPetassessordata(Hashtable properties, UserI user) {
		super(properties, user);
	}

	public XnatPetsessiondata getPetSessionData() {
		return (XnatPetsessiondata) this.getImageSessionData();
	}
}
