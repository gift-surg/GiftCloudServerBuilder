package org.nrg.xnat.security;

import java.lang.reflect.Constructor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.nrg.xnat.restlet.representations.TurbineScreenRepresentation;
import org.nrg.xnat.security.tokens.XnatDatabaseUsernamePasswordAuthenticationToken;
import org.nrg.xnat.security.tokens.XnatLdapUsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class XnatAuthenticationFilter extends UsernamePasswordAuthenticationFilter{

	static org.apache.log4j.Logger logger = Logger.getLogger(XnatAuthenticationFilter.class);
	
	@Override
	public Authentication attemptAuthentication(HttpServletRequest request,
			HttpServletResponse response) throws AuthenticationException {

		String username = request.getParameter("j_username");
		String password = request.getParameter("j_password");		

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
