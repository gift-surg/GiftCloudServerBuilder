/*
 * org.nrg.xnat.restlet.actions.TriggerPipelines
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.restlet.actions;

import org.apache.log4j.Logger;
import org.nrg.pipeline.XnatPipelineLauncher;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.restlet.util.XNATRestConstants;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

import java.util.concurrent.Callable;

/**
 * @author Timothy R. Olsen <olsent@wustl.edu>
 *
 */

public class TriggerPipelines implements Callable<Boolean> {
	static Logger logger = Logger.getLogger(TriggerPipelines.class);
	private static final String XNAT_TOOLS_AUTO_RUN_XML = "xnat_tools/AutoRun.xml";

	private final XnatExperimentdata expt;
	private final boolean supressEmail;
	private final XDATUser user;
	private final boolean waitFor;

	public TriggerPipelines(XnatExperimentdata expt,boolean supressEmail,XDATUser user){
		this(expt,supressEmail,user,false);
	}
	
	public TriggerPipelines(XnatExperimentdata expt,boolean supressEmail,XDATUser user, boolean waitFor){
		this.expt=expt;
		this.supressEmail=supressEmail;
		this.user=user;
		this.waitFor=waitFor;
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
        xnatPipelineLauncher.setWaitFor(this.waitFor);
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


        return xnatPipelineLauncher.launch(null);

	}
}
