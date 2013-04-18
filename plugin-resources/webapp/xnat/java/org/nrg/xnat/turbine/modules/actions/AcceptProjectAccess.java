// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.turbine.modules.actions;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.turbine.Turbine;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.turbine.utils.ProjectAccessRequest;

import java.util.List;

public class AcceptProjectAccess extends SecureAction {

	@Override
	public void doPerform(RunData data, Context context) throws Exception {
		XDATUser user = TurbineUtils.getUser(data);
		if (user == null) {
			user = (XDATUser) context.get("user");
        }
		if (user.getUsername().equals("guest")) {
			data.getParameters().add("nextPage", data.getTemplateInfo().getScreenTemplate());
			if (!StringUtils.isBlank(data.getAction())) {
				data.getParameters().add("nextAction", data.getAction());
            } else {
				data.getParameters().add("nextAction", Turbine.getConfiguration().getString("action.login"));
            }

			data.setScreenTemplate(Turbine.getConfiguration().getString("template.login"));
			if (logger.isDebugEnabled()) {
                logger.debug("Re-route to login:" + Turbine.getConfiguration().getString("template.login"));
            }

			return;
		}

		ProjectAccessRequest par = ProjectAccessRequest.RequestPARById(TurbineUtils.GetPassedInteger("par", data), user);
		if (par.getApproved() != null || par.getApprovalDate() != null) {
			data.setMessage("Project Invitation already accepted by a different user.  Please request access to the project directly.");
			data.setScreenTemplate("Index.vm");
            logger.debug("PAR not approved or already accepted: " + par.getRequestId());
		} else {
            List<String> processedProjects = par.process(user, true, getEventType(data), getReason(data), getComment(data));
            if (processedProjects.size() > 0) {
                context.put("accepted_pars", processedProjects);
            }
			redirectToReportScreen(XnatProjectdata.getProjectByIDorAlias(par.getProjectId(), user, false), data);
		}
	}

    private static final Logger logger = Logger.getLogger(AcceptProjectAccess.class);
}
