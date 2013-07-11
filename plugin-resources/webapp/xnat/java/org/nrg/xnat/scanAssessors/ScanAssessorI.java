/*
 * org.nrg.xnat.scanAssessors.ScanAssessorI
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.scanAssessors;

public interface ScanAssessorI {
	public ScanAssessorScanI getScanById(String id);
	public String getHeader();
	public int getPrecedence();
}
