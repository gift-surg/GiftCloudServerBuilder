//Copyright Washington University School of Medicine All Rights Reserved
/*
 * Created on Dec 11, 2006
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.turbine.Turbine;
import org.apache.turbine.modules.ActionLoader;
import org.apache.turbine.modules.actions.VelocityAction;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFT;
import org.nrg.xnat.turbine.utils.ProjectAccessRequest;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XDATRegisterUser extends org.nrg.xdat.turbine.modules.actions.XDATRegisterUser {

    public void directRequest(RunData data, Context context, XDATUser user) throws Exception {

        String nextPage = (String) TurbineUtils.GetPassedParameter("nextPage", data);
        String nextAction = (String) TurbineUtils.GetPassedParameter("nextAction", data);

        data.setScreenTemplate("Index.vm");

        String parID = (String) TurbineUtils.GetPassedParameter("par", data);

        if (StringUtils.isEmpty(parID)) {
            if (data.getSession().getAttribute("par") != null) {
                parID = (String) data.getSession().getAttribute("par");
                data.getParameters().add("par", parID);
                data.getSession().removeAttribute("par");
            } else {
                final DefaultSavedRequest savedRequest = (DefaultSavedRequest) data.getRequest().getSession().getAttribute(WebAttributes.SAVED_REQUEST);
                if (savedRequest != null) {
                    final String cachedRequest = savedRequest.getRequestURI();
                    if (!StringUtils.isBlank(cachedRequest)) {
                        Matcher matcher = PATTERN_ACCEPT_PAR.matcher(cachedRequest);
                        if (matcher.find()) {
                            parID = matcher.group(1);
                        }
                    }
                }
            }
        }

        if (parID != null) {
            AcceptProjectAccess action = new AcceptProjectAccess();
            context.put("user", user);
            action.doPerform(data, context);
        } else if (!StringUtils.isEmpty(nextAction) && !nextAction.contains("XDATLoginUser") && !nextAction.equals(Turbine.getConfiguration().getString("action.login"))) {
            if (XFT.GetUserRegistration() & !XDAT.verificationOn()) {
                data.setAction(nextAction);
                VelocityAction action = (VelocityAction) ActionLoader.getInstance().getInstance(nextAction);
                action.doPerform(data, context);
            }
        } else if (!StringUtils.isEmpty(nextPage) && !nextPage.equals(Turbine.getConfiguration().getString("template.home"))) {
            if (XFT.GetUserRegistration() && !XDAT.verificationOn()) {
                data.setScreenTemplate(nextPage);
            }
        }
    }

    @Override
    public String getAutoApprovalTextMsg(RunData data, XDATUser newUser) {
    	StringBuilder message = new StringBuilder("New User Created: ");
        message.append(newUser.getUsername());
        message.append("<br>Firstname: ").append(newUser.getFirstname());
        message.append("<br>Lastname: ").append(newUser.getLastname());
        message.append("<br>Email: ").append(newUser.getEmail());
        if (TurbineUtils.HasPassedParameter("comments", data)) {
            message.append("<br>Comments: ").append(TurbineUtils.GetPassedParameter("comments", data));
        }
        if (TurbineUtils.HasPassedParameter("phone", data)) {
            message.append("<br>Phone: ").append(TurbineUtils.GetPassedParameter("phone", data));
        }
        if (TurbineUtils.HasPassedParameter("lab", data)) {
            message.append("<br>Lab: ").append(TurbineUtils.GetPassedParameter("lab", data));
        }
        
        String parID = (String) TurbineUtils.GetPassedParameter("par", data);
		
		if (StringUtils.isEmpty(parID) && data.getSession().getAttribute("par") != null) {
			parID = (String) data.getSession().getAttribute("par");
		}
		
		if (!StringUtils.isEmpty(parID)) {
			ProjectAccessRequest par = ProjectAccessRequest.RequestPARById(Integer.valueOf(parID), null);
			if (par != null) {
				message.append("<br>Project: ").append(par.getProjectId());
			}
		}
		
        return message.toString();
    }

	@Override
	public boolean autoApproval(RunData data, Context context) throws Exception {
		boolean autoApproval = super.autoApproval(data, context);

		if (autoApproval) {
            logger.debug("Auto-approval for registration came from super...");
			return true;
		}
		
		String parID = (String) TurbineUtils.GetPassedParameter("par", data);
		
		if (StringUtils.isEmpty(parID) && data.getSession().getAttribute("par") != null) {
			parID = (String) data.getSession().getAttribute("par");
		}
		
		if (!StringUtils.isEmpty(parID)) {
			ProjectAccessRequest par = ProjectAccessRequest.RequestPARById(Integer.valueOf(parID), null);
            autoApproval = !(par == null || par.getApproved() != null || par.getApprovalDate() != null);
		}

        if (logger.isDebugEnabled()) {
            logger.debug("Auto-approval for registration from PAR: " + autoApproval);
        }

		return autoApproval;
	}

    private static final Logger logger = Logger.getLogger(XDATRegisterUser.class);
    private static final Pattern PATTERN_ACCEPT_PAR = Pattern.compile("^.*AcceptProjectAccess/par/([0-9]+).*");
}
