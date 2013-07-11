/*
 * org.nrg.xnat.turbine.modules.actions.AdminProjectAccess
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
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xnat.utils.WorkflowUtils;

import java.util.ArrayList;
import java.util.List;

public class AdminProjectAccess extends SecureAction {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.turbine.modules.actions.VelocitySecureAction#doPerform(org
	 * .apache.turbine.util.RunData, org.apache.velocity.context.Context)
	 */
	@Override
	public void doPerform(RunData data, Context context) throws Exception {
		XDATUser user = TurbineUtils.getUser(data);
		int counter = 0;
		List<String> successfulArray = new ArrayList<String>();
		List<String> unsuccessfulArray = new ArrayList<String>();
		while (TurbineUtils.HasPassedParameter("project" + counter, data)) {
			String pId = (String) TurbineUtils.GetPassedParameter("project"
					+ counter, data);
			String access = (String) TurbineUtils.GetPassedParameter("access"
					+ counter, data);
			if (access != null && !access.equals("")) {
				XnatProjectdata p = XnatProjectdata.getXnatProjectdatasById(
						pId, user, false);

				if (user.canEdit(p)) {
					successfulArray.add(pId);
					String currentAccess = p.getPublicAccessibility();

					if (!currentAccess.equals(access)) {
						PersistentWorkflowI wrk = WorkflowUtils
								.buildProjectWorkflow(
										user,
										p,
										newEventInstance(
												data,
												EventUtils.CATEGORY.PROJECT_ACCESS,
												EventUtils.MODIFY_PROJECT_ACCESS));
						EventMetaI c = wrk.buildEvent();
						if (p.initAccessibility(access, true, user, c)) {
							WorkflowUtils.complete(wrk, c);
						}

					}
				} else {
					unsuccessfulArray.add(pId);
				}

			}

			counter++;

		}
		if (successfulArray.isEmpty()) {
			data.setScreenTemplate("ActionsCompleted.vm");
			String message = "Projects' Accessibility was not changed. You do not have access to these projects: "
					+ unsuccessfulArray.toString();
			data.setMessage(message);
		} else if (!unsuccessfulArray.isEmpty()){
			data.setScreenTemplate("ActionsCompleted.vm");
			String message = "Projects' Accessibility was changed for: "
					+ successfulArray.toString()
					+ "<br><br>Projects' Accessibility was not changed for: "
					+ unsuccessfulArray.toString()
					+ "<br><br>You don't have access to these projects";
			data.setMessage(message);
		} else {
			data.setScreenTemplate("ActionsCompleted.vm");
			String message = "Projects' Accessibility was changed for: "
					+ successfulArray.toString();
			data.setMessage(message);
		}

	}

}
