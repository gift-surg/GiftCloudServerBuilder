package org.nrg.xnat.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nrg.xnat.security.tokens.XnatDatabaseUsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.codec.Base64;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.NullRememberMeServices;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

public class XnatBasicAuthenticationFilter extends BasicAuthenticationFilter {
	private AuthenticationDetailsSource authenticationDetailsSource = new WebAuthenticationDetailsSource();
    private RememberMeServices rememberMeServices = new NullRememberMeServices();
    private boolean ignoreFailure = false;
    
	private boolean authenticationIsRequired(String username) {
        // Only reauthenticate if username doesn't match SecurityContextHolder and user isn't authenticated
        // (see SEC-53)
        Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();

        if(existingAuth == null || !existingAuth.isAuthenticated()) {
            return true;
        }

        // Limit username comparison to providers which use usernames (ie UsernamePasswordAuthenticationToken)
        // (see SEC-348)

        if (existingAuth instanceof UsernamePasswordAuthenticationToken && !existingAuth.getName().equals(username)) {
            return true;
        }

        // Handle unusual condition where an AnonymousAuthenticationToken is already present
        // This shouldn't happen very often, as BasicProcessingFitler is meant to be earlier in the filter
        // chain than AnonymousAuthenticationFilter. Nevertheless, presence of both an AnonymousAuthenticationToken
        // together with a BASIC authentication request header should indicate reauthentication using the
        // BASIC protocol is desirable. This behaviour is also consistent with that provided by form and digest,
        // both of which force re-authentication if the respective header is detected (and in doing so replace
        // any existing AnonymousAuthenticationToken). See SEC-610.
        if (existingAuth instanceof AnonymousAuthenticationToken) {
            return true;
        }

        return false;
    }
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
    throws IOException, ServletException {
		final boolean debug = logger.isDebugEnabled();
        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;

        String header = request.getHeader("Authorization");

        if ((header != null) && header.startsWith("Basic ")) {
            byte[] base64Token = header.substring(6).getBytes("UTF-8");
            String token = new String(Base64.decode(base64Token), getCredentialsCharset(request));

            String username = "";
            String password = "";
            int delim = token.indexOf(":");

            if (delim != -1) {
                username = token.substring(0, delim);
                password = token.substring(delim + 1);
            }

            if (debug) {
                logger.debug("Basic Authentication Authorization header found for user '" + username + "'");
            }

            if (authenticationIsRequired(username)) {
                UsernamePasswordAuthenticationToken authRequest = 
                	XnatAuthenticationFilter.buildUPToken(XnatAuthenticationFilter.retrieveAuthMethod(username), username,password);
                authRequest.setDetails(authenticationDetailsSource.buildDetails(request));

                Authentication authResult;

                try {
                    authResult = getAuthenticationManager().authenticate(authRequest);
                } catch (AuthenticationException failed) {
                    // Authentication failed
                    if (debug) {
                        logger.debug("Authentication request for user: " + username + " failed: " + failed.toString());
                    }

                    SecurityContextHolder.getContext().setAuthentication(null);

                    rememberMeServices.loginFail(request, response);

                    onUnsuccessfulAuthentication(request, response, failed);

                    if (ignoreFailure) {
                        chain.doFilter(request, response);
                    } else {
                        getAuthenticationEntryPoint().commence(request, response, failed);
                    }

                    return;
                }

                // Authentication success
                if (debug) {
                    logger.debug("Authentication success: " + authResult.toString());
                }

                SecurityContextHolder.getContext().setAuthentication(authResult);

                rememberMeServices.loginSuccess(request, response, authResult);

                onSuccessfulAuthentication(request, response, authResult);
            }
        }

        chain.doFilter(request, response);
	}
}
