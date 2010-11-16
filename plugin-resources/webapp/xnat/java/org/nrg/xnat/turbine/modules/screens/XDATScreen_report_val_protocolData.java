/*
 * GENERATED FILE
 * Created on Tue Nov 16 22:57:48 IST 2010
 *
 */
package org.nrg.xnat.turbine.modules.screens;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.ValProtocoldata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.turbine.modules.screens.SecureReport;

/**
 * @author XDAT
 *
 */
public class XDATScreen_report_val_protocolData extends SecureReport {
	public static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(XDATScreen_report_val_protocolData.class);
	/* (non-Javadoc)
	 * @see org.nrg.xdat.turbine.modules.screens.SecureReport#finalProcessing(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
	 */
	public void finalProcessing(RunData data, Context context) {
        XnatImagesessiondata mr = ((ValProtocoldata)om).getImageSessionData();
        context.put("mr",mr);
        if(context.get("project")==null){
        	context.put("project", mr.getProject());
        }

	
	}}
