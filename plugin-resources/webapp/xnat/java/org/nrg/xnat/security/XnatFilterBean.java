package org.nrg.xnat.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.nrg.xdat.entities.XDATUserDetails;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

public class XnatFilterBean extends GenericFilterBean{

	private SessionAuthenticationStrategy sessionStrategy = new NullAuthenticatedSessionStrategy();
    private AuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler();
    private AuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
    private boolean continueChainBeforeSuccessfulAuthentication = false;
    protected ApplicationEventPublisher eventPublisher;
	
	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

		HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if (logger.isDebugEnabled()) {
            logger.debug("Request is to process authentication");
        }

        Authentication authResult;

        try {
            authResult = (Authentication) SecurityContextHolder.getContext().getAuthentication();
            sessionStrategy.onAuthentication(authResult, request, response);
        }
        catch (AuthenticationException failed) {
            // Authentication failed
            unsuccessfulAuthentication(request, response, failed);

            return;
        }
        
        // Authentication success
        if (continueChainBeforeSuccessfulAuthentication) {
            chain.doFilter(request, response);
        }

        successfulAuthentication(request, response, authResult);
	}
	  
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException failed) throws IOException, ServletException {
        SecurityContextHolder.clearContext();

        if (logger.isDebugEnabled()) {
            logger.debug("Authentication request failed: " + failed.toString());
            logger.debug("Updated SecurityContextHolder to contain null Authentication");
            logger.debug("Delegating to authentication failure handler" + failureHandler);
        }

        failureHandler.onAuthenticationFailure(request, response, failed);
    }
    
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
            Authentication authResult) throws IOException, ServletException {

        if (logger.isDebugEnabled()) {
            logger.debug("Authentication success. Updating SecurityContextHolder to contain: " + authResult);
        }

        SecurityContextHolder.getContext().setAuthentication(authResult);
        
        // Fire event
        if (this.eventPublisher != null) {
            eventPublisher.publishEvent(new InteractiveAuthenticationSuccessEvent(authResult, this.getClass()));
        }

        successHandler.onAuthenticationSuccess(request, response, authResult);
        
        HttpSession session = request.getSession();
        XDATUserDetails user = null;
        
        try{
	        if (authResult != null && authResult.getPrincipal() != null && authResult.getPrincipal() instanceof XDATUserDetails){
	        	user = (XDATUserDetails)authResult.getPrincipal();
	        }
	        else if(authResult != null && authResult.getPrincipal() != null && authResult.getPrincipal() instanceof String){
	        	user = new XDATUserDetails((String)authResult.getPrincipal());
	        }
        }
        catch(Exception e){
        	logger.error(e);
        }
        session.setAttribute("user",user);
        session.setAttribute("loggedin",true);
    }

}
