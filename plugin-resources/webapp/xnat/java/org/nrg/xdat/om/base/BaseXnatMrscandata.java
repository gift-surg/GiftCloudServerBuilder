//Copyright 2005 Harvard University / Howard Hughes Medical Institute (HHMI) All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Aug 16 15:08:17 CDT 2005
 *
 */
package org.nrg.xdat.om.base;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;

import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatMrqcscandataI;
import org.nrg.xdat.om.XnatQcscandataI;
import org.nrg.xdat.om.base.auto.AutoXnatMrscandata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileTracker;
import org.nrg.xft.utils.StringUtils;

/**
 * @author XDAT
 * 
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BaseXnatMrscandata extends AutoXnatMrscandata {

	public BaseXnatMrscandata(ItemI item) {
		super(item);
	}

	public BaseXnatMrscandata(UserI user) {
		super(user);
	}

	public BaseXnatMrscandata() {
	}

	public BaseXnatMrscandata(Hashtable properties, UserI user) {
		super(properties, user);
	}

	public XnatMrqcscandataI getManualQC() {
		return (XnatMrqcscandataI) super.getManualQC();
	}
}
