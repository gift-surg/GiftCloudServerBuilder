/*
 * GENERATED FILE
 * Created on Wed Oct 20 22:44:43 IST 2010
 *
 */
package org.nrg.xdat.om.base;
import java.util.Hashtable;
import java.util.List;

import org.nrg.xdat.model.ValProtocoldataScanCheckI;
import org.nrg.xdat.om.ValProtocoldataScanCheck;
import org.nrg.xdat.om.base.auto.AutoValProtocoldata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseValProtocoldata extends AutoValProtocoldata {

	public BaseValProtocoldata(ItemI item)
	{
		super(item);
	}

	public BaseValProtocoldata(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseValProtocolvalidationdata(UserI user)
	 **/
	public BaseValProtocoldata()
	{}

	public BaseValProtocoldata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

	public ValProtocoldataScanCheck getScanProtocolValidation(String scanId) throws NullPointerException {
		ValProtocoldataScanCheck rtn = null;
		if (scanId == null) throw new NullPointerException("Expected a non-null value for the scan id input parameter");
		List<ValProtocoldataScanCheckI> scans =  super.getScans_scanCheck();
		if (scans != null && scans.size() > 0) {
			for (int i=0; i< scans.size(); i++) {
				ValProtocoldataScanCheck aScan = (ValProtocoldataScanCheck)scans.get(i);
				if (aScan.getScanId().equals(scanId)) {
					rtn = aScan;
					break;
				}
			}
		}
		if (rtn == null) throw new NullPointerException("Couldnt find scan protocol validation for scan id " + scanId);
		return rtn;
	}

}
