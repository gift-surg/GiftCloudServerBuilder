// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xdat.om.base;

import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatPetqcscandataProcessingerror;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

public class BaseXnatPetqcscandataProcessingerror extends AutoXnatPetqcscandataProcessingerror {
	public BaseXnatPetqcscandataProcessingerror(ItemI item) {
		super(item);
	}

	public BaseXnatPetqcscandataProcessingerror(UserI user) {
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatPetqcscandataProcessingerror(UserI user)
	 */
	public BaseXnatPetqcscandataProcessingerror() {
	}

	public BaseXnatPetqcscandataProcessingerror(Hashtable properties, UserI user) {
		super(properties, user);
	}

}
