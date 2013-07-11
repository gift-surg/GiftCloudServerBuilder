/*
 * org.nrg.xnat.turbine.modules.actions.ModifyScanTypes
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.turbine.modules.actions;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.ModifyItem;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xnat.utils.WorkflowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.nrg.xnat.helpers.scanType.AbstractScanTypeMapping.standardizeFormat;


public class ModifyScanTypes extends ModifyItem{
    private final Logger logger = LoggerFactory.getLogger(ModifyScanTypes.class);

    /* (non-Javadoc)
     * @see org.apache.turbine.modules.actions.VelocityAction#doPerform(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    public void doPerform(final RunData data, final Context context) throws Exception {
        try {
			final String project = (String)TurbineUtils.GetPassedParameter("project", data);
			final XDATUser user = TurbineUtils.getUser(data);
			
			final XnatProjectdata p = XnatProjectdata.getProjectByIDorAlias(project, user, false);
			final int numRows = Integer.parseInt((String)TurbineUtils.GetPassedParameter("numRows", data));
            
			for(final XnatExperimentdata exp : p.getExperiments()){
				if(exp instanceof XnatImagesessiondata){
					XnatImagesessiondata imageSession = (XnatImagesessiondata)(exp);
					if(user.canEdit(imageSession)){					
						final List<XnatImagescandata> scans = imageSession.getScans_scan();
						if (scans != null) {
				            final PersistentWorkflowI wrk = WorkflowUtils.getOrCreateWorkflowData(null, user, exp.getXSIType(), exp.getId(), exp.getProject(), newEventInstance(data, EventUtils.CATEGORY.DATA, EventUtils.getAddModifyAction(exp.getXSIType(), false)));
				            final EventMetaI ci=wrk.buildEvent();
							
				        	int count = 0;
				        	while(count<=numRows){
				        		final int frames = Integer.parseInt((String)TurbineUtils.GetPassedParameter("frames_"+count, data));
				        		final String series_description = standardizeFormat((String)TurbineUtils.GetPassedParameter("series_description_"+count, data));
				        		final String imagetype = standardizeFormat((String)TurbineUtils.GetPassedParameter("parameters_imagetype_"+count, data));
				        		
				        		if((data.getParameters().getString("change_"+count)!=null) && ((String)TurbineUtils.GetPassedParameter("change_"+count, data)).equals("doChange")){
				        		    for (final XnatImagescandata scan : scans) {
						                if(Integer.valueOf(frames).equals(scan.getFrames()) &&
						                        equalsStandardized(series_description, scan.getSeriesDescription()) &&
						                        equalsStandardized(imagetype, (String)scan.getProperty("parameters_imagetype")))
						                {
						                	scan.setType((String)TurbineUtils.GetPassedParameter("type_"+count, data));
						                	SaveItemHelper.authorizedSave(scan,user, false, false,ci);
						                }
						            }
				        		}
				        		count++;
				        	}
				        	
				        	PersistentWorkflowUtils.complete(wrk, ci);
				        }	
					}
				}		
			}
        	
			this.redirectToReportScreen("XDATScreen_report_xnat_projectData.vm",(ItemI) p, data);
			
        } catch (Throwable t) {
            logger.error("unable to modify scan types",t);
            data.setMessage("Error: Item save failed.  See log for details.");
            handleException(data,null,null);
        }
    }


    private static boolean equalsStandardized(final String standardized, final String s) {
        return standardizeFormat(s).equals(standardized);
    }
}
