package org.nrg.xnat.security;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.nrg.xnat.security.tokens.XnatDatabaseUsernamePasswordAuthenticationToken;
import org.nrg.xnat.security.tokens.XnatLdapUsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.codec.Base64;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class XnatAuthenticationFilter extends UsernamePasswordAuthenticationFilter{

	static org.apache.log4j.Logger logger = Logger.getLogger(XnatAuthenticationFilter.class);
	
	@Override
	public Authentication attemptAuthentication(HttpServletRequest request,
			HttpServletResponse response) throws AuthenticationException {

		String username = request.getParameter("j_username");
		String password = request.getParameter("j_password");		

        // If we didn't find a username
        if (StringUtils.isBlank(username)) {
            // See if there's an authorization header.
            String header = request.getHeader("Authorization");
            if (!StringUtils.isBlank(header) && header.startsWith("Basic ")) {
                byte[] base64Token = new byte[0];
                try {
                    base64Token = header.substring(6).getBytes("UTF-8");
                    String token = new String(Base64.decode(base64Token), "UTF-8");
                    int delim = token.indexOf(":");

                    if (delim != -1) {
                        username = token.substring(0, delim);
                        password = token.substring(delim + 1);
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug("Basic Authentication Authorization header found for user '" + username + "'");
                    }
                } catch (UnsupportedEncodingException exception) {
                    logger.error("Encoding exception on authentication attempt", exception);
                }
            }
        }

		UsernamePasswordAuthenticationToken authRequest = buildUPToken(request.getParameter("login_method"),username,password);
	    
	    setDetails(request, authRequest);

	    return super.getAuthenticationManager().authenticate(authRequest);
	}

	public UsernamePasswordAuthenticationToken buildUPToken(String id, String username, String password){
		if ("LDAP".equals(id)) {
	        return new XnatLdapUsernamePasswordAuthenticationToken(username, password);
	    }
	    else {
	        return new XnatDatabaseUsernamePasswordAuthenticationToken(username, password);
	    }

	}

	public static Class[] args=new Class[]{String.class,String.class};
	
	public UsernamePasswordAuthenticationToken buildUPTokenFromClass(String username, String password, String classname){
		try {
			Class c=Class.forName("org.nrg.xnat.security.DatabaseUsernamePasswordAuthenticationToken");
			Constructor constructor=c.getConstructor(args);
			
			String[] local_args=new String[]{username,password};
			return (UsernamePasswordAuthenticationToken) constructor.newInstance((Object[])local_args);
		} catch (Exception e) {
			logger.error("", e);
			return new XnatDatabaseUsernamePasswordAuthenticationToken(username, password);
		}

	}
}
