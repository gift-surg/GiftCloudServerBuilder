package org.nrg.xnat.scanAssessors;

import java.util.Comparator;


public class AssessorComparator implements Comparator<ScanAssessorI>{

	public int compare(ScanAssessorI o1, ScanAssessorI o2) {
		return ((Integer)o1.getPrecedence()).compareTo((Integer)o2.getPrecedence());
	}
	
}
