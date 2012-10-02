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
        setDefaultTargetUrl(getRequiredLogoutSuccessUrl());
        super.handle(request, response, authentication);
    }

    private String getRequiredLogoutSuccessUrl() {
        ArcArchivespecification arcSpec = ArcSpecManager.GetInstance();

        // Check for null arcSpec.
        if (arcSpec == null) {
            _log.warn("Found null arc spec, returning secured URL for uninitiated system.");
            return _securedXnatLogoutSuccessUrl;
        }
        // If it's not null, see what it's got to say.
        final Boolean requireLogin = arcSpec.getRequireLogin();
        if (requireLogin == null) {
            _log.warn("Found null require login setting, returning secured URL for uninitiated system.");
            return _securedXnatLogoutSuccessUrl;
        }

        final String returnUrl = requireLogin ? _securedXnatLogoutSuccessUrl : _openXnatLogoutSuccessUrl;

        if (_log.isDebugEnabled()) {
            _log.debug("Found require login set to: " + requireLogin + ", setting required logout success URL to: " + returnUrl);
        }

        return returnUrl;
    }

    private static final Log _log = LogFactory.getLog(XnatLogoutSuccessHandler.class);

    private String _openXnatLogoutSuccessUrl;
    private String _securedXnatLogoutSuccessUrl;
}
