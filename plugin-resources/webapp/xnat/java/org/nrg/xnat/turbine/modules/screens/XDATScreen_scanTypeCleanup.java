package org.nrg.xnat.turbine.modules.screens;

import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.XnatMrscandata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.screens.SecureReport;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xnat.helpers.scanType.ScanTypeMapping.ScanTypeHistory;
import org.nrg.xnat.helpers.scanType.ScanTypeMapping;
import org.nrg.xnat.helpers.scanType.ScanTypeMappingI;

public class XDATScreen_scanTypeCleanup extends SecureReport {
     static org.apache.log4j.Logger logger = Logger.getLogger(XDATScreen_scanTypeCleanup.class);
     
	@Override
	public void finalProcessing(RunData data, Context context) {
		try{
			String proj = (String)om.getProperty("id");
			String dbName = TurbineUtils.getUser(data).getDBName();
			try {
				String query = "SELECT DISTINCT REPLACE(REPLACE(REPLACE(REPLACE(UPPER(scan.series_description),' ',''),'_',''),'-',''),'*','') AS series_description,scan.type,UPPER(parameters_imagetype) AS parameters_imagetype,frames FROM xnat_imagescandata scan LEFT JOIN xnat_mrscandata mr ON scan.xnat_imagescandata_id=mr.xnat_imagescandata_id LEFT JOIN xnat_experimentData isd ON scan.image_session_id=isd.id WHERE scan.series_description IS NOT NULL AND isd.project='" + proj + "' ORDER BY series_description;";
				XFTTable t = XFTTable.Execute(query, dbName, "system");
				context.put("queryResults", t);
			} catch (SQLException e) {
				logger.error("",e);
			} catch (DBPoolException e) {
				logger.error("",e);
			}
		}
		catch(Exception e){
			
		}	
	}
 }
