package org.nrg.xnat.security;

import org.nrg.xdat.XDAT;
import org.nrg.xdat.entities.AliasToken;
import org.nrg.xdat.om.ArcArchivespecification;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.services.AliasTokenService;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.codec.Base64;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

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
        if(passwordExpired!=null && !(Boolean)passwordExpired){
            //If the date of password change was checked earlier in the session and found to be not expired, do not send them to the expired password page.
            chain.doFilter(request, response);
        }
        else if(_arcSpec==null || !_arcSpec.isComplete()){
            //If the arc spec has not yet been set, have the user configure the arc spec before changing their password. This prevents a negative interaction with the arc spec filter.
            chain.doFilter(request, response);
        }
        else if (user==null){
            //If the user is not logged in, do not send them to the expired password page.

            String header = request.getHeader("Authorization");
            if(header!=null && header.startsWith("Basic ")){
                //For users that authenticated using basic authentication, check whether their password is expired, and if so give them a 403 and a message that they need to change their password.

                String token = new String(Base64.decode(header.substring(6).getBytes("UTF-8")), "UTF-8");
                String username = "";
                int delim = token.indexOf(":");
                if (delim != -1) {
                    username = token.substring(0, delim);
                }
                if (AliasToken.isAliasFormat(username)) {
                    AliasTokenService service = XDAT.getContextService().getBean(AliasTokenService.class);
                    AliasToken alias = service.locateToken(username);
                    if (alias == null) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Your security token has expired. Please try again after updating your session.");
                        return;
                    }
                    username = alias.getXdatUserId();
                }
                boolean isExpired=true;
                String interval = ((XnatProviderManager) XDAT.getContextService().getBean("customAuthenticationManager",ProviderManager.class)).getExpirationInterval().trim();
                if(interval.equals("-1")){
                	chain.doFilter(request, response);
                }
                else{
	                try{
	                    List<Boolean> expired = (new JdbcTemplate(XDAT.getDataSource())).query("SELECT ((now()-password_updated)> (Interval '"+interval+"')) AS expired FROM xhbm_xdat_user_auth WHERE auth_user = ? AND auth_method = 'localdb'", new String[] {username}, new RowMapper<Boolean>() {
	                        public Boolean mapRow(ResultSet rs, int rowNum) throws SQLException {
	                            return rs.getBoolean(1);
	                        }
	                    });
	                    isExpired = expired.get(0);
	                }
	                catch(Exception e){
	                    logger.error(e);
	                }
	                request.getSession().setAttribute("expired", isExpired);
	                if(username!=null && isExpired && !username.equals("guest")){
	                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Your password has expired. Please try again after changing your password.");
	                }
	                else{
	                    chain.doFilter(request, response);
	                }
                }
            }
            else{
                //User is not authenticated through basic authentication either.
                chain.doFilter(req, res);
            }
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
                String interval = ((XnatProviderManager) XDAT.getContextService().getBean("customAuthenticationManager",ProviderManager.class)).getExpirationInterval().trim();
                if(interval.equals("-1")){
                	chain.doFilter(request, response);
                }
                else{
	                try{
	                    List<Boolean> expired = (new JdbcTemplate(XDAT.getDataSource())).query("SELECT ((now()-password_updated)> (Interval '"+interval+"')) AS expired FROM xhbm_xdat_user_auth WHERE auth_user = ? AND auth_method = 'localdb'", new String[] {username}, new RowMapper<Boolean>() {
	                        public Boolean mapRow(ResultSet rs, int rowNum) throws SQLException {
	                            return rs.getBoolean(1);
	                        }
	                    });
	                    isExpired = expired.get(0);
	                }
	                catch(Exception e){
	                    logger.error(e);
	                }
	                request.getSession().setAttribute("expired", isExpired);
	                if(username!=null && isExpired && !username.equals("guest")){
	                    response.sendRedirect(TurbineUtils.GetFullServerPath() + changePasswordPath);
	                }
	                else{
	                    chain.doFilter(request, response);
	                }
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
