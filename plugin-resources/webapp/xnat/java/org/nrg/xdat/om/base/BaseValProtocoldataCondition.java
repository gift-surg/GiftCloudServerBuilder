/*
 * GENERATED FILE
 * Created on Mon Nov 15 15:41:19 IST 2010
 *
 */
package org.nrg.xdat.om.base;
import org.nrg.xdat.om.base.auto.*;
import org.nrg.xft.*;
import org.nrg.xft.security.UserI;

import java.util.*;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseValProtocoldataCondition extends AutoValProtocoldataCondition {

	public BaseValProtocoldataCondition(ItemI item)
	{
		super(item);
	}

	public BaseValProtocoldataCondition(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseValProtocoldataCondition(UserI user)
	 **/
	public BaseValProtocoldataCondition()
	{}

	public BaseValProtocoldataCondition(Hashtable properties, UserI user)
	{
		super(properties,user);
	}
	
	public String getExpectedValue() {
		String rtn ="";
		String diagnosis = getDiagnosis();
		int expectedIndex = diagnosis.indexOf("Expected:");
		if (expectedIndex != -1) {
			int foundIndex = diagnosis.indexOf("Found:");
			if (foundIndex != -1) {
			   	rtn = diagnosis.substring(expectedIndex + 9 ,foundIndex);
			}else {
				rtn = diagnosis.substring(expectedIndex + 9);
			}
		}
		return rtn.trim();
	}

	public String getFoundValue() {
		String rtn ="";
		String diagnosis = getDiagnosis();
		int foundIndex = diagnosis.indexOf("Found:");
		if (foundIndex != -1) {
		   	rtn = diagnosis.substring(foundIndex + 6);
		}
		return rtn.trim();
	}

}
