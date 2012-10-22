/**
 * XnatAuthenticationEntryPoint
 * (C) 2012 Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD License
 *
 * Created on 10/4/12 by rherri01
 */
package org.nrg.xnat.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class XnatAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException, ServletException {
        final String strippedUri = request.getRequestURI().substring(request.getContextPath().length());
        boolean foundNoninteractivePath = false;
        for (final String noninteractivePath : _noninteractivePaths) {
            if (strippedUri.startsWith(noninteractivePath)) {
                foundNoninteractivePath = true;
                break;
            }
        }
        if (foundNoninteractivePath) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            super.commence(request, response, authException);
        }
    }

    public void setNoninteractivePaths(List<String> noninteractivePaths) {
        _noninteractivePaths = noninteractivePaths;
    }

    private List<String> _noninteractivePaths;
}
