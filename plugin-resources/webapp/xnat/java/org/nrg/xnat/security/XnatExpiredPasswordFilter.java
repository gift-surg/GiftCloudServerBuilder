package org.nrg.xnat.security;

import java.io.IOException;
import java.security.Principal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.nrg.xdat.XDAT;
import org.nrg.xdat.entities.XDATUserDetails;
import org.nrg.xdat.entities.XdatUserAuth;
import org.nrg.xdat.om.ArcArchivespecification;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.codec.Base64;
import org.springframework.web.filter.GenericFilterBean;

public class XnatExpiredPasswordFilter extends GenericFilterBean {
	private String changePasswordPath = "";
	private String changePasswordDestination = "";
	
	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;
        XDATUser user = (XDATUser)request.getSession().getAttribute("user");
        Object passwordExpired = request.getSession().getAttribute("expired");
        ArcArchivespecification _arcSpec = ArcSpecManager.GetInstance();
        if(user==null || (passwordExpired!=null && !(Boolean)passwordExpired)){
        	//If the user is not logged in or the date of password change was checked earlier in the session and found to be not expired, do not send them to the expired password page.
        	chain.doFilter(request, response);
        }
        else if(_arcSpec==null || !_arcSpec.isComplete()){
        	//If the arc spec has not yet been set, have the user configure the arc spec before changing their password. This prevents a negative interaction with the arc spec filter.
        	chain.doFilter(request, response);
        }
        else{
        	String referer = request.getHeader("Referer");
	    	String uri = request.getRequestURI();
	    	
	    	if(uri.endsWith(changePasswordPath) || uri.endsWith(changePasswordDestination)){
	    		//If you're already on the change password page, continue on without redirect.
	    		chain.doFilter(req, res);
	    	}
	    	else if(referer!=null && (referer.endsWith(changePasswordPath) || referer.endsWith(changePasswordDestination))){
	    		//If you're on a request within the change password page, continue on without redirect.
	    		chain.doFilter(req, res);
	    	}
	    	else{
	        	String username = user.getUsername();
	
		        boolean isExpired=true;
		        try{
					List<Boolean> expired = (new JdbcTemplate(XDAT.getDataSource())).query("SELECT ((now()-password_updated)> (Interval '"+((XnatProviderManager) XDAT.getContextService().getBean("customAuthenticationManager",ProviderManager.class)).getExpirationInterval()+" seconds')) AS expired FROM xhbm_xdat_user_auth WHERE auth_user = ? AND auth_method = 'localdb'", new String[] {username}, new RowMapper<Boolean>() {
			            public Boolean mapRow(ResultSet rs, int rowNum) throws SQLException {
			            	boolean updated = rs.getBoolean(1);
			                return updated;
			            }
		            });
					isExpired = expired.get(0);
				}
				catch(Exception e){
					logger.error(e);
				}
		        request.getSession().setAttribute("expired",new Boolean(isExpired));
				if(username!=null & isExpired & !username.equals("guest")){
					response.sendRedirect(TurbineUtils.GetFullServerPath() + changePasswordPath);
				}
				else{
					chain.doFilter(request, response);
				}
	    	}    
        }
	}
	
	public void setChangePasswordPath(String path) {
        this.changePasswordPath = path;
    }
	
	public void setChangePasswordDestination(String path) {
        this.changePasswordDestination = path;
    }
}
