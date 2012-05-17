package org.nrg.xnat.security;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.nrg.xdat.om.ArcArchivespecification;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.springframework.security.core.codec.Base64;
import org.springframework.web.filter.GenericFilterBean;

public class XnatArcSpecFilter extends GenericFilterBean {
	private String configurationPath = "";	
	private String nonAdminErrorPath = "";
	private String changePasswordPath = "";
	private String changePasswordDestination = "";
	
	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;
        
        ArcArchivespecification _arcSpec = ArcSpecManager.GetInstance();
        
        XDATUser user = (XDATUser)request.getSession().getAttribute("user");
        if(_arcSpec!=null && _arcSpec.isComplete()){
        	//If arc spec has already been set, do not redirect.
        	chain.doFilter(req, res);
        }
        else if(user==null){
        	//Do not direct users to the configuration page if they are not logged in.

        	String header = request.getHeader("Authorization");
        	if(header!=null && header.startsWith("Basic ")){
        		//Users that authenticated using basic authentication receive an error message informing them that the arc spec is not set.
        		response.sendError(HttpServletResponse.SC_FORBIDDEN, "Site has not yet been configured.");
        	}
        	else{
        		//User is not authenticated through basic authentication either.
        		chain.doFilter(req, res);
        	}
        }
	    else{
	    	String referer = request.getHeader("Referer");
	    	String uri = request.getRequestURI();
	    	
	    	if(uri.endsWith(configurationPath) || uri.endsWith(nonAdminErrorPath) || uri.endsWith(changePasswordPath) || uri.endsWith(changePasswordDestination)){
	    		//If you're already on the configuration page, error page, or expired password page, continue on without redirect.
	    		chain.doFilter(req, res);
	    	}
	    	else if(referer!=null && (referer.endsWith(configurationPath) || referer.endsWith(nonAdminErrorPath) || referer.endsWith(changePasswordPath) || referer.endsWith(changePasswordDestination)) && !uri.contains("/app/template") && !uri.contains("/app/screen") && !uri.endsWith(".vm")){
	    		//If you're on a request within the configuration page (or error page or expired password page), continue on without redirect. This checks that the referer is the configuration page and that 
	    		// the request is not for another page (preventing the user from navigating away from the Configuration page via the menu bar).
	    		chain.doFilter(req, res);
	    	}
	    	else{
	    		try {
					if(user.checkRole("Administrator")){
						//Otherwise, if the user has administrative permissions, direct the user to the configuration page.
						response.sendRedirect(TurbineUtils.GetFullServerPath() + configurationPath);
					}
					else{
						//The arc spec is not set but the user does not have administrative permissions. Direct the user to an error page.
						response.sendRedirect(TurbineUtils.GetFullServerPath() + nonAdminErrorPath);
					}
				} catch (Exception e) {
					logger.error("Error checking user role in the Arc Spec Filter.",e);
					response.sendRedirect(TurbineUtils.GetFullServerPath() + nonAdminErrorPath);
				}
	    	}   
	    }
	}
	
	public void setConfigurationPath(String path) {
        this.configurationPath = path;
    }
	
	public void setNonAdminErrorPath(String path) {
        this.nonAdminErrorPath = path;
    }
	
	public void setChangePasswordPath(String path) {
        this.changePasswordPath = path;
    }
	public void setChangePasswordDestination(String path) {
        this.changePasswordDestination = path;
    }
}
