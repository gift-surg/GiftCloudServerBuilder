package org.nrg.xnat.scanAssessors;

public interface ScanAssessorI {
	public ScanAssessorScanI getScanById(String id);
	public String getHeader();
	public int getPrecedence();
}
