/*
 * GENERATED FILE
 * Created on Mon Nov 22 10:20:49 CST 2010
 *
 */
package org.nrg.xdat.om.base;
import org.nrg.xdat.om.base.auto.*;
import org.nrg.xft.*;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.scanAssessors.ScanAssessorScanI;

import java.util.*;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseValProtocoldataScanCheck extends AutoValProtocoldataScanCheck implements ScanAssessorScanI{

	public BaseValProtocoldataScanCheck(ItemI item)
	{
		super(item);
	}

	public BaseValProtocoldataScanCheck(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseValProtocoldataScanCheck(UserI user)
	 **/
	public BaseValProtocoldataScanCheck()
	{}

	public BaseValProtocoldataScanCheck(Hashtable properties, UserI user)
	{
		super(properties,user);
	}
	
	public String getSummary(){
		String status = getStatus();
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
