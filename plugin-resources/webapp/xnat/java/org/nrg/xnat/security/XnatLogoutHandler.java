package org.nrg.xnat.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nrg.xdat.XDAT;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.logout.LogoutHandler;

public class XnatLogoutHandler implements LogoutHandler {
	SessionRegistry sessionRegistry;
	
	@Override
	public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {

		
		//expire that guy here.
		sessionRegistry = XDAT.getContextService().getBean("sessionRegistry", SessionRegistryImpl.class );
		SessionInformation si = sessionRegistry.getSessionInformation(request.getSession().getId());
		if(si!=null){
			si.expireNow();
		}
		
	}

	public SessionRegistry getSessionRegistry() {
		return sessionRegistry;
	}

	public void setSessionRegistry(SessionRegistry sessionRegistry) {
		this.sessionRegistry = sessionRegistry;
	}

}
