package org.nrg.xnat.security;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.entities.XdatUserAuth;
import org.nrg.xnat.security.provider.XnatLdapAuthenticationProvider;
import org.nrg.xnat.security.tokens.XnatDatabaseUsernamePasswordAuthenticationToken;
import org.nrg.xnat.security.tokens.XnatLdapUsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.codec.Base64;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.google.common.collect.Maps;

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
        
        //SHOULD we be throwing an exception if the username is null?
        
        String auth_method=request.getParameter("login_method");
        if(StringUtils.isEmpty(auth_method) && !StringUtils.isEmpty(username)){
        	//try to guess the auth_method
        	auth_method=retrieveAuthMethod(username);
        	if(StringUtils.isEmpty(auth_method)){
        		throw new BadCredentialsException("Missing login_method parameter.");
        	}
        }

		UsernamePasswordAuthenticationToken authRequest = buildUPToken(auth_method,username,password);
	    
	    setDetails(request, authRequest);

	    return super.getAuthenticationManager().authenticate(authRequest);
	}

	public UsernamePasswordAuthenticationToken buildUPToken(String id, String username, String password){
		List<AuthenticationProvider> prov = XDAT.getContextService().getBean("customAuthenticationManager",ProviderManager.class).getProviders();
		AuthenticationProvider chosenProvider = null;
		for(AuthenticationProvider p : prov){
			if(p.toString().equals(id)){
				chosenProvider = p;
			}
		}
		if (chosenProvider instanceof XnatLdapAuthenticationProvider) {
	        return new XnatLdapUsernamePasswordAuthenticationToken(username, password, id);
	    }
	    else {
	        return new XnatDatabaseUsernamePasswordAuthenticationToken(username, password);
	    }
	}
	
	private static Map<String,String> cached_methods=Maps.newConcurrentMap();//this will prevent 20,000 curl scripts from hitting the db everytime
	private static String retrieveAuthMethod(final String username){
		String auth=cached_methods.get(username);
		if(auth==null){
			List<XdatUserAuth> user_auths=XDAT.getXdatUserAuthService().getUsersByName(username);
	    	if(user_auths.size()==1){
	    		auth=user_auths.get(0).getAuthMethod();
	    		cached_methods.put(username.intern(),auth.intern());
	    	}
		}
		return auth;
	}
}
