package org.nrg.xnat.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.entities.XDATUserDetails;
import org.nrg.xdat.entities.XdatUserAuth;
import org.nrg.xdat.services.XdatUserAuthService;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xft.XFT;
import org.nrg.xft.utils.AuthUtils;
import org.nrg.xnat.security.alias.AliasTokenAuthenticationProvider;
import org.nrg.xnat.security.config.AuthenticationProviderConfigurator;
import org.nrg.xnat.security.provider.XnatDatabaseAuthenticationProvider;
import org.nrg.xnat.security.provider.XnatLdapAuthenticationProvider;
import org.nrg.xnat.security.tokens.XnatLdapUsernamePasswordAuthenticationToken;
import org.nrg.xnat.security.userdetailsservices.XnatDatabaseUserDetailsService;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.mail.MailSendException;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.encoding.PlaintextPasswordEncoder;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class XnatProviderManager extends ProviderManager {
    private static final String SECURITY_MAX_FAILED_LOGINS_LOCKOUT_DURATION_PROPERTY = "security.max_failed_logins_lockout_duration";
    private static final String SECURITY_MAX_FAILED_LOGINS_PROPERTY = "security.max_failed_logins";
    private static final String SECURITY_PASSWORD_COMPLEXITY_PROPERTY = "security.password_complexity";
    private static final String SECURITY_PASSWORD_COMPLEXITY_MESSAGE_PROPERTY = "security.password_complexity_message";
	private static final String SECURITY_PASSWORD_EXPIRATION_PROPERTY = "security.password_expiration";

	
	private static final Log logger = LogFactory.getLog(XnatProviderManager.class);

    private AuthenticationEventPublisher eventPublisher = new NullEventPublisher();
    protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();
    private AuthenticationManager parent;
    private Properties properties;

	private static final FailedAttemptsManager failures= new FailedAttemptsManager();
	private static String PASSWORD_COMPLEXITY="";
	private static String PASSWORD_COMPLEXITY_MESSAGE="";

	private static String PASSWORD_EXPIRATION="-1";
    private Map<String, AuthenticationProviderConfigurator> _configurators;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (properties == null) {
            throw new IllegalArgumentException("The list of authentication providers cannot be set to null.");
        }

        if(properties.getProperty(SECURITY_MAX_FAILED_LOGINS_PROPERTY)!=null){
            AuthUtils.MAX_FAILED_LOGIN_ATTEMPTS=Integer.valueOf(properties.getProperty(SECURITY_MAX_FAILED_LOGINS_PROPERTY));
        }

        if(properties.getProperty(SECURITY_MAX_FAILED_LOGINS_LOCKOUT_DURATION_PROPERTY)!=null){
            AuthUtils.LOCKOUT_DURATION=Integer.valueOf(properties.getProperty(SECURITY_MAX_FAILED_LOGINS_LOCKOUT_DURATION_PROPERTY));
            if(AuthUtils.LOCKOUT_DURATION>0)AuthUtils.LOCKOUT_DURATION=-(AuthUtils.LOCKOUT_DURATION); //LOCKOUT must be negative for date comparison to work
        }

        if(properties.getProperty(SECURITY_PASSWORD_COMPLEXITY_PROPERTY)!=null){
            PASSWORD_COMPLEXITY=properties.getProperty(SECURITY_PASSWORD_COMPLEXITY_PROPERTY);
        }

        if(properties.getProperty(SECURITY_PASSWORD_COMPLEXITY_MESSAGE_PROPERTY)!=null){
            PASSWORD_COMPLEXITY_MESSAGE=properties.getProperty(SECURITY_PASSWORD_COMPLEXITY_MESSAGE_PROPERTY);
        }

        if(properties.getProperty(SECURITY_PASSWORD_EXPIRATION_PROPERTY)!=null){
            PASSWORD_EXPIRATION=properties.getProperty(SECURITY_PASSWORD_EXPIRATION_PROPERTY);
        }

        String commaDelineatedProviders = properties.getProperty("provider.providers.enabled");
        assert commaDelineatedProviders != null : "You must specify at least one authentication provider configuration.";
        String[] providerArray=commaDelineatedProviders.split("[\\s,]+");
        HashMap<String, HashMap<String, String>> providerMap = new HashMap<String, HashMap<String, String>>();
        for(String prov : providerArray){
        	providerMap.put(prov, new HashMap<String, String>());
        }
        // Populate map of properties
        for(Map.Entry<Object, Object> entry : properties.entrySet()) {
        	String key = (String) entry.getKey();
        	StringTokenizer st = new StringTokenizer(key, ".");
            String provider = st.nextToken();
            if (provider.equals("provider")) {
        		String name = st.nextToken();
        		if(providerMap.containsKey(name)) {
                    StringBuilder providerProperty = new StringBuilder();
                    while (st.hasMoreTokens()) {
                        if (providerProperty.length() > 0) {
                            providerProperty.append(".");
                        }
                        providerProperty.append(st.nextToken());
                    }
        			providerMap.get(name).put(providerProperty.toString(), (String) entry.getValue());
        		}
        	}
        }
        
     // Create providers
        List<AuthenticationProvider> providers = new ArrayList<AuthenticationProvider>();
        for(String prov: providerArray){
        	String name = providerMap.get(prov).get("name");
        	String id = providerMap.get(prov).get("id");
        	String type = providerMap.get(prov).get("type");

            assert name != null : "You must provide a name for all authentication provider configurations";
            assert id != null : "You must provide an ID for all authentication provider configurations";
            assert type != null : "You must provide a type for all authentication provider configurations";

            if (_configurators.containsKey(type)) {
                AuthenticationProviderConfigurator configurator = _configurators.get(type);
                providers.addAll(configurator.getAuthenticationProviders(id, name, providerMap.get(prov)));
            }
        }
        setProviders(providers);
    }

	public void setProperties(List<String> filenames) {
		properties = new Properties();
		for(String filename:filenames){
			String path = "../../../../../../"+filename;
			try {
				URL url = getClass().getResource(path);
				properties.load(url.openStream());
			} catch (IOException e) {
				logger.error(e);
			}
		}
	}

    public void setAuthenticationProviderConfigurators(Map<String, AuthenticationProviderConfigurator> configurators) {
        _configurators = configurators;
    }

	@Override
	public Authentication doAuthentication(Authentication authentication) throws AuthenticationException {
        Class<? extends Authentication> toTest = authentication.getClass();
        AuthenticationException lastException = null;
        Authentication result = null;

        for (AuthenticationProvider provider : getProviders()) {
            if (!provider.supports(toTest)) {
                continue;
            }
            if(authentication instanceof XnatLdapUsernamePasswordAuthenticationToken){
            	if (!((XnatLdapUsernamePasswordAuthenticationToken)authentication).getProviderName().equals(provider.toString())){
            		//This is a different LDAP provider than the one that was selected.
            		continue;
            	}
            }
           
            logger.debug("Authentication attempt using " + provider.getClass().getName());

            try {
                result = provider.authenticate(authentication);

                if (result != null) {
                    copyDetails(authentication, result);
                    break;
                }
            } catch (AccountStatusException e) {
                // SEC-546: Avoid polling additional providers if auth failure is due to invalid account status
                eventPublisher.publishAuthenticationFailure(e, authentication);
                throw e;
            } catch(NewLdapAccountNotAutoEnabledException e) {
                try {
                   AdminUtils.sendNewUserRequestNotification(
                		   e.getUserDetails().getUsername(), 
                		   e.getUserDetails().getFirstname(), 
                		   e.getUserDetails().getLastname(), 
                		   e.getUserDetails().getEmail()
                		   , "", "", "", new VelocityContext()
                	);
                } catch (Exception exception) {
                    logger.error("Error occurred sending new user request email", exception);
                }
                lastException = e;
                
            } catch (AuthenticationException e) {
                lastException = e;
            }
        }

        if (result == null && parent != null) {
            // Allow the parent to try.
            try {
                result = parent.authenticate(authentication);
            } catch (ProviderNotFoundException e) {
                // ignore as we will throw below if no other exception occurred prior to calling parent and the parent
                // may throw ProviderNotFound even though a provider in the child already handled the request
            } catch (AuthenticationException e) {
                lastException = e;
            }
        }
        
        if (result != null) {
            boolean eraseCredentialsAfterAuthentication = false;
            if (eraseCredentialsAfterAuthentication && (result instanceof CredentialsContainer)) {
                // Authentication is complete. Remove credentials and other secret data from authentication
                ((CredentialsContainer)result).eraseCredentials();
            }

            eventPublisher.publishAuthenticationSuccess(result);
            
            failures.clearCount(authentication);
            
            return result;
        }else{
        	//increment failed login attempt
        	failures.addFailedLoginAttempt(authentication);
        }

        // Parent was null, or didn't authenticate (or throw an exception).

        if (lastException == null) {
            lastException = new ProviderNotFoundException(messages.getMessage("ProviderManager.providerNotFound",
                        new Object[] {toTest.getName()}, "No AuthenticationProvider found for {0}"));
        }

        eventPublisher.publishAuthenticationFailure(lastException, authentication);

        throw lastException;
    }
	
    private void copyDetails(Authentication source, Authentication destination) {
        if ((destination instanceof AbstractAuthenticationToken) && (destination.getDetails() == null)) {
            AbstractAuthenticationToken token = (AbstractAuthenticationToken) destination;

            token.setDetails(source.getDetails());
        }
    }
    
    private static final class NullEventPublisher implements AuthenticationEventPublisher {
        public void publishAuthenticationFailure(AuthenticationException exception, Authentication authentication) {}
        public void publishAuthenticationSuccess(Authentication authentication) {}
    }

    public String getExpirationInterval(){
    	return PASSWORD_EXPIRATION;
    }
    
	private static class FailedAttemptsManager {
		/**
		 * Increments failed Login count
         * @param auth    The authentication that failed.
		 *
		 */
		private synchronized void addFailedLoginAttempt(final Authentication auth){
			if(AuthUtils.MAX_FAILED_LOGIN_ATTEMPTS>0 ){
				XdatUserAuth ua=getUserByAuth(auth);
				if(ua!=null){
					ua.setFailedLoginAttempts(ua.getFailedLoginAttempts()+1);
					XDAT.getXdatUserAuthService().update(ua);
				}
			}
		} 
		
		public void clearCount(final Authentication auth) {
			if(AuthUtils.MAX_FAILED_LOGIN_ATTEMPTS>0 ){
				XdatUserAuth ua=getUserByAuth(auth);
				if(ua!=null){
					ua.setFailedLoginAttempts(0);
					XDAT.getXdatUserAuthService().update(ua);
				}
			}
					}
				}
				
	public static XdatUserAuth getUserByAuth(Authentication authentication) {
		if(authentication==null){
			return null;
		}
		
		final String u;
		if(authentication.getPrincipal() instanceof String){
			u=(String)authentication.getPrincipal();
		}else{
			u=((XDATUserDetails)authentication.getPrincipal()).getLogin();
		}
		final String method;
		final String provider;
		if(authentication instanceof XnatLdapUsernamePasswordAuthenticationToken){
        	provider=((XnatLdapUsernamePasswordAuthenticationToken)authentication).getProviderName();
        	method=XdatUserAuthService.LDAP;
        }else{
        	provider=XnatDatabaseUserDetailsService.DB_PROVIDER;
        	method=XdatUserAuthService.LOCALDB;
			}
		
		return XDAT.getXdatUserAuthService().getUserByNameAndAuth(u, method, provider);
		}
	}
