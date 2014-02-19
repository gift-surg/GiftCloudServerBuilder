/*
 * org.nrg.xnat.security.OnXnatLogin
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 12/12/13 5:27 PM
 */
package org.nrg.xnat.security;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.config.exceptions.ConfigServiceException;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.entities.XDATUserDetails;
import org.nrg.xdat.turbine.utils.AccessLogger;
import org.nrg.xft.XFTItem;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.utils.SaveItemHelper;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

public class OnXnatLogin extends SavedRequestAwareAuthenticationSuccessHandler {

	protected final Log logger = LogFactory.getLog(getClass());
	
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request,
			HttpServletResponse response, Authentication authentication)
			throws IOException, ServletException {

        if (logger.isDebugEnabled()) {
            logger.debug("Request is to process authentication");
        }
        
        try{
        	SecurityContext securityContext = SecurityContextHolder.getContext();
        	
	        XDATUserDetails user= null;
	        Object principal = securityContext.getAuthentication().getPrincipal();
	        
	        if(principal instanceof XDATUserDetails){
	        	user = (XDATUserDetails)principal;
	        }
	        else if (principal instanceof String){
	        	user = new XDATUserDetails((String)principal);
	        }
	        
	        request.getSession().setAttribute("user", user);
	      	java.util.Date today = java.util.Calendar.getInstance(java.util.TimeZone.getDefault()).getTime();
	      	XFTItem item = XFTItem.NewItem("xdat:user_login",user);
	      	item.setProperty("xdat:user_login.user_xdat_user_id", user.getID());
	      	item.setProperty("xdat:user_login.login_date",today);
	      	item.setProperty("xdat:user_login.ip_address", AccessLogger.GetRequestIp(request));
	      	item.setProperty("xdat:user_login.session_id", request.getSession().getId());
	      	SaveItemHelper.authorizedSave(item,null,true,false, EventUtils.DEFAULT_EVENT(user,null));
	      	
	      	request.getSession().setAttribute("XNAT_CSRF", UUID.randomUUID().toString());
        }
        catch(Exception e){
        	logger.error(e);
        }
        super.onAuthenticationSuccess(request, response, authentication);
	}
    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response){
        String loginLanding = "/app/template/Index.vm?login=true";
        String url = getDefaultTargetUrl();
        if("/".equals(url)){
            setDefaultTargetUrl(loginLanding);
            return loginLanding;
        } else {
            return super.determineTargetUrl(request, response);
        }
    }
}
