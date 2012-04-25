package org.nrg.xnat.security;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.entities.XDATUserDetails;
import org.nrg.xdat.entities.XdatUserAuth;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.XFTItem;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

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
	      	item.setProperty("xdat:user_login.ip_address", request.getRemoteAddr());
	      	item.setProperty("xdat:user_login.session_id", request.getSession().getId());
	      	item.save(null,true,false);
	      	request.getSession().setAttribute("XNAT_CSRF", UUID.randomUUID().toString());
        }
        catch(Exception e){
        	logger.error(e);
        }
        super.onAuthenticationSuccess(request, response, authentication);
	}

}
