// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xdat.om.base;

import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatQcscandata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.scanAssessors.ScanAssessorScanI;


@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatQcscandata extends AutoXnatQcscandata implements ScanAssessorScanI{
	public BaseXnatQcscandata(ItemI item) {
		super(item);
	}

	public BaseXnatQcscandata(UserI user) {
		super(user);
	}

	public BaseXnatQcscandata() {
	}

	public BaseXnatQcscandata(Hashtable properties, UserI user) {
		super(properties, user);
	}
	
	public String getSummary(){
		String status = getPass();
		String summary = "<span>Unknown</span>";
		if(status!=null){
			if(status.equals("1")||status.equalsIgnoreCase("pass")){
				summary = "<span style=\"color:green\">Passed</span>";
			}
			else if(status.equals("0")||status.equalsIgnoreCase("fail")){
				summary = "<span style=\"color:red\">Failed</span>";
			}
		}
		return summary;
	}

}
