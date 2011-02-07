/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.xnat.restlet.actions;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.nrg.pipeline.XnatPipelineLauncher;
import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.db.DBAction;
import org.nrg.xnat.restlet.util.XNATRestConstants;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

/**
 * @author Timothy R. Olsen <olsent@wustl.edu>
 *
 */

public class TriggerPipelines implements Callable<Boolean> {
	static Logger logger = Logger.getLogger(TriggerPipelines.class);
	private static final String XNAT_TOOLS_AUTO_RUN_XML = "xnat_tools/AutoRun.xml";

	private final XnatExperimentdata expt;
	private final boolean clearExistingWorkflows;
	private final boolean supressEmail;
	private final XDATUser user;
	
	public TriggerPipelines(XnatExperimentdata expt, boolean clearExistingWorkflows,boolean supressEmail,XDATUser user){
		this.expt=expt;
		this.clearExistingWorkflows=clearExistingWorkflows;
		this.supressEmail=supressEmail;
		this.user=user;
	}
	
	public Boolean call() {
		XnatPipelineLauncher xnatPipelineLauncher = new XnatPipelineLauncher((XDATUser)user);
        xnatPipelineLauncher.setAdmin_email(AdminUtils.getAdminEmailId());
        xnatPipelineLauncher.setAlwaysEmailAdmin(ArcSpecManager.GetInstance().getEmailspecifications_pipeline());
        String pipelineName = XNAT_TOOLS_AUTO_RUN_XML;
        xnatPipelineLauncher.setPipelineName(pipelineName);
        xnatPipelineLauncher.setNeedsBuildDir(false);
        xnatPipelineLauncher.setSupressNotification(true);
        xnatPipelineLauncher.setId(expt.getId());
        xnatPipelineLauncher.setLabel(expt.getLabel());
        xnatPipelineLauncher.setDataType(expt.getXSIType());
        xnatPipelineLauncher.setExternalId(expt.getProject());
        xnatPipelineLauncher.setParameter(XNATRestConstants.SUPRESS_EMAIL, (new Boolean(supressEmail)).toString());
        xnatPipelineLauncher.setParameter("session", expt.getId());
        xnatPipelineLauncher.setParameter("sessionLabel", expt.getLabel());
        xnatPipelineLauncher.setParameter("useremail", user.getEmail());
        xnatPipelineLauncher.setParameter("userfullname", XnatPipelineLauncher.getUserName(user));
        xnatPipelineLauncher.setParameter("adminemail", AdminUtils.getAdminEmailId());
        xnatPipelineLauncher.setParameter("xnatserver", TurbineUtils.GetSystemName());
        xnatPipelineLauncher.setParameter("mailhost", AdminUtils.getMailServer());
        xnatPipelineLauncher.setParameter("sessionType", expt.getXSIType());
        xnatPipelineLauncher.setParameter("xnat_project", expt.getProject());
        
        if (clearExistingWorkflows)
        {
            try {
				ArrayList<WrkWorkflowdata> workflows = WrkWorkflowdata.getWrkWorkflowdatasByField("wrk:workFlowData.ID", expt.getId(), user, false);
				
				for (WrkWorkflowdata wrk : workflows){
				    DBAction.DeleteItem(wrk.getItem(),user);
				}
			} catch (SQLException e) {
				logger.error("",e);
			} catch (Exception e) {
				logger.error("",e);
			}
        }
   
        return xnatPipelineLauncher.launch(null);
    
	}	
}
