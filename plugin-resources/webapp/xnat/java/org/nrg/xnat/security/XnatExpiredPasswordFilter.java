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
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.codec.Base64;
import org.springframework.web.filter.GenericFilterBean;

public class XnatExpiredPasswordFilter extends GenericFilterBean {
	private final String changePasswordPath = "/app/template/ChangePassword.vm";
	private final String changePasswordDestination = "/app/action/XDATChangePassword";
	
	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;
        XDATUserDetails user = (XDATUserDetails)request.getSession().getAttribute("user");
        if(user!=null && !request.getRequestURI().contains(changePasswordPath) && !request.getRequestURI().contains(changePasswordDestination)){
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
	        
			if(username!=null & isExpired){
				response.sendRedirect(TurbineUtils.GetFullServerPath() + changePasswordPath);
			}
			else{
				chain.doFilter(request, response);
			}
        }
        else{
        	chain.doFilter(request, response);
        }
	}
}
