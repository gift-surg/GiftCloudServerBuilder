//Copyright 2011 Washington University School of Medicine All Rights Reserved
/*
 * Created on Aug 22, 2007
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import java.sql.SQLException;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.om.XnatSubjectassessordata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.schema.SchemaElement;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.DisplayItemAction;
import org.nrg.xdat.turbine.modules.actions.ModifyItem;
import org.nrg.xdat.turbine.utils.PopulateItem;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFT;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTable;
import org.nrg.xft.collections.ItemCollection;
import org.nrg.xft.db.DBAction;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.search.ItemSearch;
import org.nrg.xft.utils.ValidationUtils.ValidationResults;
import org.nrg.xnat.helpers.scanType.ScanTypeMapping;
import org.nrg.xnat.helpers.scanType.ScanTypeMappingI;

public class ModifyScanTypes extends ModifyItem{
    static Logger logger = Logger.getLogger(ModifyScanTypes.class);

    /* (non-Javadoc)
     * @see org.apache.turbine.modules.actions.VelocityAction#doPerform(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    public void doPerform(RunData data, Context context) throws Exception {
        XFTItem found = null;
        try {
			String project = data.getParameters().getString("project");
			XDATUser user = TurbineUtils.getUser(data);
			String dbName = user.getDBName();
			ScanTypeMapping mapping = new ScanTypeMapping(project,dbName);
			
			XnatProjectdata p = XnatProjectdata.getProjectByIDorAlias(project, user, false);
			int numRows = Integer.parseInt(data.getParameters().getString("numRows"));
			
			for(XnatExperimentdata exp : p.getExperiments()){
				if(exp instanceof XnatImagesessiondata){
					XnatImagesessiondata imageSession = (XnatImagesessiondata)(exp);
					if(user.canEdit(imageSession)){
					
						List scans = imageSession.getScans_scan();
						if (scans != null) {
							
				        	int count = 0;
				        	while(count<=numRows){
				        		int frames = Integer.parseInt(data.getParameters().getString("frames_"+count));
				        		String series_description = standardizeFormat(data.getParameters().getString("series_description_"+count));
				        		String imagetype = standardizeFormat(data.getParameters().getString("parameters_imagetype_"+count));
				        		
				        		if((data.getParameters().getString("change_"+count)!=null) && data.getParameters().getString("change_"+count).equals("doChange")){
							
						            for (int i = 0; i < scans.size(); i++) {
						                XnatImagescandata scan = (XnatImagescandata) scans.get(i);
						                
						                if((scan.getFrames().equals(frames)) && (standardizeFormat(scan.getSeriesDescription()).equals(series_description)) && (standardizeFormat((String)scan.getProperty("parameters_imagetype")).equals(imagetype)))
						                {
						                	scan.setType(data.getParameters().getString("type_"+count));
						                	scan.save(user, false, false);
						                }
						            }
				        		}
				        	count++;
				        	}
				        }	
					}
				}		
			}
        	
			this.redirectToReportScreen("XDATScreen_report_xnat_projectData.vm",(ItemI) p, data);
			
        } catch (Exception e) {
            logger.error("",e);
            data.setMessage("Error: Item save failed.  See log for details.");
            handleException(data,(XFTItem)found,null);
        }
    }

    /* (non-Javadoc)
     * @see org.nrg.xdat.turbine.modules.actions.ModifyItem#preProcess(org.nrg.xft.XFTItem, org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    @Override
    public void preProcess(XFTItem item, RunData data, Context context) {
        super.preProcess(item, data, context);
    }

    /* (non-Javadoc)
     * @see org.nrg.xdat.turbine.modules.actions.ModifyItem#handleException(org.apache.turbine.util.RunData, org.nrg.xft.XFTItem, java.lang.Throwable)
     */
    @Override
    public void handleException(RunData data, XFTItem first, Throwable error) {
    	super.handleException(data, first, error);
    }

	private static String standardizeFormat(String originalString){
		originalString = StringUtils.replace(originalString, " ", "");
		originalString = StringUtils.replace(originalString, "_", "");
		originalString = StringUtils.replace(originalString, "-", "");
		originalString = StringUtils.replace(originalString, "*", "");
		return originalString.toUpperCase();
	}
    
}
