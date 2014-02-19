/*
 * org.nrg.xnat.security.XnatLogoutHandler
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.security;

import org.nrg.xdat.XDAT;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class XnatLogoutHandler extends SecurityContextLogoutHandler implements LogoutHandler {
    SessionRegistry sessionRegistry;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        super.logout(request, response, authentication);

        //expire that guy here.
        SessionInformation si = getSessionRegistry().getSessionInformation(request.getSession().getId());
        if (si!=null) {
            si.expireNow();
        }

    }

    public SessionRegistry getSessionRegistry() {
        if (sessionRegistry == null) {
            setSessionRegistry(XDAT.getContextService().getBean("sessionRegistry", SessionRegistryImpl.class));
        }
        return sessionRegistry;
    }

    public void setSessionRegistry(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }
}

