/*
 * org.nrg.xnat.helpers.prearchive.ProjectPrearchiveI
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.helpers.prearchive;

import org.nrg.xft.XFTTable;

import java.util.Date;

public interface ProjectPrearchiveI {

	public abstract Date getLastMod();

	public abstract XFTTable getContent();

}