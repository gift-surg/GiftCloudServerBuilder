/*
 * org.nrg.xnat.ajax.GrantProjectAccess
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.ajax;

import org.apache.turbine.services.rundata.RunDataService;
import org.apache.turbine.services.rundata.TurbineRunDataFacade;
import org.apache.turbine.services.velocity.TurbineVelocity;
import org.apache.turbine.util.RunData;
import org.apache.turbine.util.TurbineException;
import org.apache.velocity.context.Context;
import org.nrg.xdat.display.DisplayManager;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.turbine.utils.ProjectAccessRequest;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class GrantProjectAccess {
	public void invite(HttpServletRequest req, HttpServletResponse response,ServletConfig sc) throws IOException{
		String email = (String)req.getParameter("email");
		String projectID = (String)req.getParameter("project");
		String level = (String)req.getParameter("level");
		
		RunDataService rundataService = null;
        rundataService = TurbineRunDataFacade.getService();
        try {
            if (rundataService == null)
            {
                throw new TurbineException(
                        "No RunData Service configured!");
            }
            RunData data = rundataService.getRunData(req, response, sc);
            Context context = TurbineVelocity.getContext(data);
            XDATUser user = TurbineUtils.getUser(data);
    		
    		XnatProjectdata project = XnatProjectdata.getXnatProjectdatasById(projectID, user, false);
    		
    		context.put("user",user);
            context.put("server",TurbineUtils.GetFullServerPath());
            context.put("process","Transfer to the archive.");
            context.put("system",TurbineUtils.GetSystemName());
            context.put("access_level",level);
            context.put("admin_email",AdminUtils.getAdminEmailId());
            context.put("projectOM",project);
            //SEND email to user
	    ProjectAccessRequest.InviteUser(context, email, user, user.getFirstname() + " " + user.getLastname()
		    + " has invited you to join the " + project.getName() + " "
		    + DisplayManager.GetInstance().getSingularDisplayNameForProject().toLowerCase() + ".");
        }catch(Exception e){
        	
        }
		
        response.setContentType("text/html");
        response.setHeader("Cache-Control", "no-cache");
        response.getWriter().write("GREEN");
        
    }
}
