package org.nrg.xnat.security;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.entities.AliasToken;
import org.nrg.xdat.entities.XdatUserAuth;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.services.XdatUserAuthService;
import org.nrg.xdat.turbine.utils.AccessLogger;
import org.nrg.xft.XFTItem;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.InvalidValueException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xnat.security.alias.AliasTokenAuthenticationProvider;
import org.nrg.xnat.security.alias.AliasTokenAuthenticationToken;
import org.nrg.xnat.security.provider.XnatAuthenticationProvider;
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

import com.google.common.collect.Lists;
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
                byte[] base64Token;
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

        String providerName=request.getParameter("login_method");
        UsernamePasswordAuthenticationToken authRequest = null;
        
        if(StringUtils.isEmpty(providerName) && !StringUtils.isEmpty(username)){
            //try to guess the auth_method
        	String auth_method = retrieveAuthMethod(username);
            if(StringUtils.isEmpty(auth_method)){
                throw new BadCredentialsException("Missing login_method parameter.");
            }
            else {
                authRequest=buildUPTokenForAuthMethod(auth_method,username,password);
            }
        }
        else {
            authRequest=buildUPTokenForProviderName(providerName,username,password);
        }

        setDetails(request, authRequest);

        try {
			return super.getAuthenticationManager().authenticate(authRequest);
		} catch (AuthenticationException e) {
			 if (!StringUtils.isBlank(username)) {
				 Integer uid=retrieveUserId(username);
				 if(uid!=null){
					try {
						XFTItem item = XFTItem.NewItem("xdat:user_login",null);
						item.setProperty("xdat:user_login.user_xdat_user_id",uid);
						item.setProperty("xdat:user_login.ip_address",AccessLogger.GetRequestIp(request));
						item.setProperty("xdat:user_login.login_date",java.util.Calendar.getInstance(java.util.TimeZone.getDefault()).getTime());
						SaveItemHelper.authorizedSave(item,null,true,false,(EventMetaI)null);
					} catch (Exception e1) {
						//ignore... throw the authentication exception
					}
				 }
			 }
			throw e;
		}
    }
    
    private static Map<String,Integer> checked=Maps.newHashMap();
    public static Integer retrieveUserId(String username){
    	synchronized(checked){
	    	if(username==null){
	    		return null;
	    	}
	    	
	    	if(checked.containsKey(username)){
	    		return checked.get(username);
	    	}
	    	
	    	Integer i=XDATUser.getUserid(username);
	    	checked.put(username, i);
	    	
	    	return i;
    	}
    }
    
    public static UsernamePasswordAuthenticationToken buildUPTokenForAuthMethod(String authMethod, String username, String password){
        XnatAuthenticationProvider chosenProvider = findAuthenticationProviderByAuthMethod(authMethod);
        return buildUPToken(chosenProvider, username, password);
    }
    
    public static UsernamePasswordAuthenticationToken buildUPTokenForProviderName(String providerName, String username, String password){
        XnatAuthenticationProvider chosenProvider = findAuthenticationProviderByProviderName(providerName);
        return buildUPToken(chosenProvider, username, password);
    }
    
    private interface XnatAuthenticationProviderMatcher  {
    	boolean matches(XnatAuthenticationProvider provider);
    }
    
    private static XnatAuthenticationProvider findAuthenticationProviderByAuthMethod(final String authMethod){
    	return findAuthenticationProvider(new XnatAuthenticationProviderMatcher() {
			@Override
			public boolean matches(XnatAuthenticationProvider provider) {
				return provider.getAuthMethod().equalsIgnoreCase(authMethod);
			}
		});
    }

    private static XnatAuthenticationProvider findAuthenticationProviderByProviderName(final String providerName){
    	return findAuthenticationProvider(new XnatAuthenticationProviderMatcher() {
			@Override
			public boolean matches(XnatAuthenticationProvider provider) {
				return provider.getName().equalsIgnoreCase(providerName);
			}
		});
    }

    private static XnatAuthenticationProvider findAuthenticationProvider(XnatAuthenticationProviderMatcher matcher){
        List<AuthenticationProvider> prov = XDAT.getContextService().getBean("customAuthenticationManager",ProviderManager.class).getProviders();
        for(AuthenticationProvider ap : prov){
        	XnatAuthenticationProvider xap = (XnatAuthenticationProvider) ap;
            if(matcher.matches(xap)){
            	return xap;
            }
        }
        return null;
    }
    
    private static UsernamePasswordAuthenticationToken buildUPToken(XnatAuthenticationProvider provider, String username, String password){
        if (provider instanceof XnatLdapAuthenticationProvider) {
            return new XnatLdapUsernamePasswordAuthenticationToken(username, password, provider.getID());
        } else  if (provider instanceof AliasTokenAuthenticationProvider) {
            return new AliasTokenAuthenticationToken(username, Long.parseLong(password));
        } else {
            return new XnatDatabaseUsernamePasswordAuthenticationToken(username, password);
        }
    }

    private static Map<String,String> cached_methods=Maps.newConcurrentMap();//this will prevent 20,000 curl scripts from hitting the db everytime
    public static String retrieveAuthMethod(final String username){
        String auth=cached_methods.get(username);
        if(auth==null){
            List<XdatUserAuth> user_auths=XDAT.getXdatUserAuthService().getUsersByName(username);
            if(user_auths.size()==1){
                auth=user_auths.get(0).getAuthMethod();
                cached_methods.put(username.intern(),auth.intern());
            // The list may contain localdb auth method even when password is empty and LDAP authentication is used (MRH)    
            } else if(user_auths.size()>1){
            	for (XdatUserAuth uauth : user_auths) {
           			if (!uauth.getAuthMethod().equalsIgnoreCase(XdatUserAuthService.LOCALDB)) {
           				auth=uauth.getAuthMethod();
            			cached_methods.put(username.intern(),auth.intern());
            			break;
           			}
            	}
            } else if (AliasToken.isAliasFormat(username)) {
                auth = XdatUserAuthService.TOKEN;
                cached_methods.put(username.intern(), auth.intern());
            }
        }
        return auth;
    }
}
