/**
 * XnatLogoutSuccessHandler
 * (C) 2012 Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD License
 *
 * Created on 10/2/12 by rherri01
 */
package org.nrg.xnat.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.xdat.om.ArcArchivespecification;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AbstractAuthenticationTargetUrlRequestHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

/**
 * Handles the navigation on logout by delegating to the {@link AbstractAuthenticationTargetUrlRequestHandler} base
 * class logic, but setting the {@link AbstractAuthenticationTargetUrlRequestHandler#setDefaultTargetUrl(String)} to use
 * the appropriate destination based on the requiresLogin setting.
 *
 * @author Rick Herrick
 * @since 1.6.1
 */
public class XnatLogoutSuccessHandler extends AbstractAuthenticationTargetUrlRequestHandler implements LogoutSuccessHandler {

    public void setOpenXnatLogoutSuccessUrl(String openXnatLogoutSuccessUrl) {
        _openXnatLogoutSuccessUrl = openXnatLogoutSuccessUrl;
    }

    public void setSecuredXnatLogoutSuccessUrl(String securedXnatLogoutSuccessUrl) {
        _securedXnatLogoutSuccessUrl = securedXnatLogoutSuccessUrl;
    }

    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        setDefaultTargetUrl(isRequiredLogin() ? _securedXnatLogoutSuccessUrl : _openXnatLogoutSuccessUrl);
        super.handle(request, response, authentication);
    }

    private boolean isRequiredLogin() {
        // First check for null arcSpace, initialize if null.
        if (_arcSpec == null) {
            initializeArcSpecInstance();
        }
        // If it's STILL null, then arcSpec hasn't been initialized in the database, so just say false.
        if (_arcSpec == null) {
            return false;
        }
        // If it's not null, see what it's got to say.
        return _arcSpec.getRequireLogin();
    }

    // TODO: This is probably going to cause trouble eventually since an existing instance doesn't reflect changes made
    // after instantiation. But we don't want to grab an arc spec instance on every logout. Or do we? What's the load?
    private synchronized void initializeArcSpecInstance() {
        if (_arcSpec == null) {
            _arcSpec = ArcSpecManager.GetInstance();
        }
    }

    private static final Log _log = LogFactory.getLog(XnatLogoutSuccessHandler.class);
    private static ArcArchivespecification _arcSpec;

    private String _openXnatLogoutSuccessUrl;
    private String _securedXnatLogoutSuccessUrl;
}
