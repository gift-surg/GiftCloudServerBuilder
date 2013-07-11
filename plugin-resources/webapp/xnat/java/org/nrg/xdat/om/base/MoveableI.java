/*
 * org.nrg.xdat.om.base.MoveableI
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xdat.om.base;

import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xft.XFTItem;


public interface MoveableI {
	public String getProject();
	public XnatProjectdata getProjectData();
	public String getLabel();
	public String getId();
	public XFTItem getCurrentDBVersion(boolean withChildren);
	public void setProject(String v);
	public void setLabel(String v);
}
