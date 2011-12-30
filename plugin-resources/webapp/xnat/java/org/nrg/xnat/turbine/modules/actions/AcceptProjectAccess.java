// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.turbine.modules.actions;

import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.turbine.utils.ProjectAccessRequest;

public class AcceptProjectAccess extends SecureAction {
    static org.apache.log4j.Logger logger = Logger.getLogger(AcceptProjectAccess.class);

    XnatProjectdata project=null;
    
	@Override
	public void doPerform(RunData data, Context context) throws Exception {
		final Integer parID = data.getParameters().getInteger("par");
		XDATUser user = TurbineUtils.getUser(data);
		if (user==null)
			user = (XDATUser) context.get("user");
		if (user.getUsername().equals("guest")) {
			String Destination = data.getTemplateInfo().getScreenTemplate();
			data.getParameters().add("nextPage", Destination);
			if (!data.getAction().equalsIgnoreCase(""))
				data.getParameters().add("nextAction", data.getAction());
			else
				data.getParameters().add("nextAction", org.apache.turbine.Turbine.getConfiguration().getString("action.login"));
			data.setScreenTemplate(org.apache.turbine.Turbine.getConfiguration().getString("template.login"));

			System.out.println("Re-route to login:" + org.apache.turbine.Turbine.getConfiguration().getString("template.login"));
			return;
		}
		ProjectAccessRequest par =ProjectAccessRequest.RequestPARById(parID, user);
		if (par.getApproved()!=null || par.getApprovalDate()!=null){
			data.setMessage("Project Invitation already accepted by a different user.  Please request access to the project directly.");
			data.setScreenTemplate("Index.vm");
		}else{
			par.process(user,true, getEventType(data), getReason(data), getComment(data));
	        
			this.redirectToReportScreen(project, data);
		}
	}

}
