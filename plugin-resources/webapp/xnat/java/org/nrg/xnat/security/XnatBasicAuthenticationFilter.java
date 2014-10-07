/*
 * org.nrg.xnat.security.XnatBasicAuthenticationFilter
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.security;

import org.nrg.xdat.entities.XDATUserDetails;
import org.nrg.xdat.turbine.utils.AccessLogger;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xft.XFTItem;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.utils.SaveItemHelper;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.codec.Base64;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

public class XnatBasicAuthenticationFilter extends BasicAuthenticationFilter {
	private AuthenticationDetailsSource authenticationDetailsSource = new WebAuthenticationDetailsSource();
    private SessionAuthenticationStrategy sessionStrategy = new NullAuthenticatedSessionStrategy();
    
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
        // This shouldn't happen very often, as BasicProcessingFilter is meant to be earlier in the filter
        // chain than AnonymousAuthenticationFilter. Nevertheless, presence of both an AnonymousAuthenticationToken
        // together with a BASIC authentication request header should indicate reauthentication using the
        // BASIC protocol is desirable. This behaviour is also consistent with that provided by form and digest,
        // both of which force re-authentication if the respective header is detected (and in doing so replace
        // any existing AnonymousAuthenticationToken). See SEC-610.
        return existingAuth instanceof AnonymousAuthenticationToken;

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
                	XnatAuthenticationFilter.buildUPTokenForAuthMethod(XnatAuthenticationFilter.retrieveAuthMethod(username), username,password);
                authRequest.setDetails(authenticationDetailsSource.buildDetails(request));

                Authentication authResult;

                try {
                    authResult = getAuthenticationManager().authenticate(authRequest);
                    
                    sessionStrategy.onAuthentication(authResult, request, response);
                    
                    
                } catch (AuthenticationException failed) {
                    // Authentication failed
                    if (debug) {
                        logger.debug("Authentication request for user: " + username + " failed: " + failed.toString());
                    }

                    SecurityContextHolder.getContext().setAuthentication(null);
                    onUnsuccessfulAuthentication(request, response, failed);
                    
                	XnatAuthenticationFilter.logFailedAttempt(username, request);//originally I put this in the onUnsuccessfulAuthentication method, but that would force me to re-parse the username
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, AdminUtils.GetLoginFailureMessage());

                    return;
                }

                // Authentication success
                if (debug) {
                    logger.debug("Authentication success: " + authResult.toString());
                }

                SecurityContextHolder.getContext().setAuthentication(authResult);
                onSuccessfulAuthentication(request, response, authResult);
            }
        }

        chain.doFilter(request, response);
	}
	
    @Override
    // XNAT-2186 requested that REST logins also leave records of last login date
    protected void onSuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              Authentication authResult) throws IOException {
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
	            SaveItemHelper.authorizedSave(item, null, true, false, EventUtils.DEFAULT_EVENT(user, null));

            request.getSession().setAttribute("XNAT_CSRF", UUID.randomUUID().toString());
        }
        catch(Exception e){
            logger.error(e);
        }

        super.onSuccessfulAuthentication(request, response, authResult);
    }
    
    /**
     * The session handling strategy which will be invoked immediately after an authentication request is
     * successfully processed by the <tt>AuthenticationManager</tt>. Used, for example, to handle changing of the
     * session identifier to prevent session fixation attacks.
     *
     * @param sessionStrategy the implementation to use. If not set a null implementation is
     * used.
     */
    public void setSessionAuthenticationStrategy(SessionAuthenticationStrategy sessionStrategy) {
        this.sessionStrategy = sessionStrategy;
    }
	
}
