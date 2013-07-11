/*
 * org.nrg.xnat.security.XnatArcSpecFilter
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.security;

import org.nrg.xdat.om.ArcArchivespecification;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class XnatArcSpecFilter extends GenericFilterBean {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;

        final ArcArchivespecification arcSpec = ArcSpecManager.GetInstance();

        if (arcSpec != null && arcSpec.isComplete()) {
            //If arc spec has already been set, do not redirect.
            chain.doFilter(req, res);
        } else {
            final XDATUser user = (XDATUser) request.getSession().getAttribute("user");
            final String uri = request.getRequestURI();

            if (user == null) {
                String header = request.getHeader("Authorization");
                if (header != null && header.startsWith("Basic ") && !uri.contains(_initializationPath)) {
                    //Users that authenticated using basic authentication receive an error message informing them that the arc spec is not set.
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Site has not yet been configured.");
                    return;
                }
            }

            final String referer = request.getHeader("Referer");

            if (uri.contains(_initializationPath) || uri.endsWith(_configurationPath) || uri.endsWith(_nonAdminErrorPath) || isExemptedPath(uri)) {
                //If you're already on the configuration page, error page, or expired password page, continue on without redirect.
                chain.doFilter(req, res);
            } else if (referer != null && (referer.endsWith(_configurationPath) || referer.endsWith(_nonAdminErrorPath) || isExemptedPath(referer)) && !uri.contains("/app/template") && !uri.contains("/app/screen") && !uri.endsWith(".vm") && !uri.equals("/")) {
                //If you're on a request within the configuration page (or error page or expired password page), continue on without redirect. This checks that the referer is the configuration page and that
                // the request is not for another page (preventing the user from navigating away from the Configuration page via the menu bar).
                chain.doFilter(req, res);
            } else {
                try {
                    if(user == null) {
                	// user not authenticated, let another filter handle the redirect
                	// (NB: I tried putting this check up with the basic auth check, 
                	// but you get this weird redirect with 2 login pages on the same screen.  Seems to work here).
                        chain.doFilter(req, res);
                    } else if (user.checkRole("Administrator")) {
                        //Otherwise, if the user has administrative permissions, direct the user to the configuration page.
                        response.sendRedirect(TurbineUtils.GetFullServerPath() + _configurationPath);
                    } else {
                        //The arc spec is not set but the user does not have administrative permissions. Direct the user to an error page.
                        response.sendRedirect(TurbineUtils.GetFullServerPath() + _nonAdminErrorPath);
                    }
                } catch (Exception e) {
                    logger.error("Error checking user role in the Arc Spec Filter.", e);
                    response.sendRedirect(TurbineUtils.GetFullServerPath() + _nonAdminErrorPath);
                }
            }
        }
    }

    public void setInitializationPath(final String initializationPath) {
        _initializationPath = initializationPath;
    }

    public void setConfigurationPath(String configurationPath) {
        _configurationPath = configurationPath;
    }

    public void setNonAdminErrorPath(String nonAdminErrorPath) {
        _nonAdminErrorPath = nonAdminErrorPath;
    }

    public void setExemptedPaths(List<String> exemptedPaths) {
        _exemptedPaths.clear();
        _exemptedPaths.addAll(exemptedPaths);
    }

    private boolean isExemptedPath(String path) {
        for (final String exemptedPath : _exemptedPaths) {
            if (path.endsWith(exemptedPath)) {
                return true;
            }
        }
        return false;
    }

    private String _initializationPath = "";
    private String _configurationPath = "";
    private String _nonAdminErrorPath = "";
    private final List<String> _exemptedPaths = new ArrayList<String>();
}
