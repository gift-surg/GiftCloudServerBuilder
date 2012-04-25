package org.nrg.xnat.security;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.xdat.XDAT;
import org.nrg.xnat.security.alias.AliasTokenAuthenticationProvider;
import org.nrg.xnat.security.provider.XnatDatabaseAuthenticationProvider;
import org.nrg.xnat.security.provider.XnatLdapAuthenticationProvider;
import org.nrg.xnat.security.tokens.XnatLdapUsernamePasswordAuthenticationToken;
import org.nrg.xnat.security.userdetailsservices.XnatDatabaseUserDetailsService;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.authentication.encoding.PlaintextPasswordEncoder;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class XnatProviderManager extends ProviderManager {
    private static final String SECURITY_MAX_FAILED_LOGINS_LOCKOUT_DURATION_PROPERTY = "security.max_failed_logins_lockout_duration";
    private static final String SECURITY_MAX_FAILED_LOGINS_PROPERTY = "security.max_failed_logins";
    private static final String SECURITY_PASSWORD_COMPLEXITY_PROPERTY = "security.password_complexity";
    private static final String SECURITY_PASSWORD_COMPLEXITY_MESSAGE_PROPERTY = "security.password_complexity_message";

	private static final Log logger = LogFactory.getLog(XnatProviderManager.class);

    private AuthenticationEventPublisher eventPublisher = new NullEventPublisher();
    protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();
    private AuthenticationManager parent;
    private boolean eraseCredentialsAfterAuthentication = false;
    private Properties properties;
    private List<String> loginOptions = new ArrayList<String>();

	private static final FailedAttemptsManager failures= new FailedAttemptsManager();
	private static Integer MAX_FAILED_LOGIN_ATTEMPTS=-1;
	private static Integer LOCKOUT_DURATION=-60000;//in seconds
    
	private static String PASSWORD_COMPLEXITY="";
	private static String PASSWORD_COMPLEXITY_MESSAGE="";
	
    @Override
    public void afterPropertiesSet() throws Exception {
        if (properties == null) {
            throw new IllegalArgumentException("The list of authentication providers cannot be set to null.");
        }
        String commaDeliniatedProviders = properties.getProperty("provider.providers.enabled");
        String[] providerArray=commaDeliniatedProviders.split("[\\s,]+");
        HashMap<String, HashMap<String, String>> providerMap = new HashMap<String, HashMap<String, String>>();
        for(String prov : providerArray){
        	providerMap.put(prov, new HashMap<String, String>());
        }
        // Populate map of properties
        for(Map.Entry<Object, Object> entry : properties.entrySet()) {
        	String key = (String) entry.getKey();
        	StringTokenizer st = new StringTokenizer(key, ".");
        	if (st.nextToken().equals("provider")) {
        		String name = st.nextToken();
        		if(providerMap.containsKey(name)){
        			providerMap.get(name).put(key, (String) entry.getValue());	
        		}
        	}
        }
        
        if(properties.getProperty(SECURITY_MAX_FAILED_LOGINS_PROPERTY)!=null){
        	MAX_FAILED_LOGIN_ATTEMPTS=Integer.valueOf(properties.getProperty(SECURITY_MAX_FAILED_LOGINS_PROPERTY));
        }
        
        if(properties.getProperty(SECURITY_MAX_FAILED_LOGINS_LOCKOUT_DURATION_PROPERTY)!=null){
        	LOCKOUT_DURATION=Integer.valueOf(properties.getProperty(SECURITY_MAX_FAILED_LOGINS_LOCKOUT_DURATION_PROPERTY));
        	if(LOCKOUT_DURATION>0)LOCKOUT_DURATION=-(LOCKOUT_DURATION); //LOCKOUT must be negative for date comparison to work
        }
    
        if(properties.getProperty(SECURITY_PASSWORD_COMPLEXITY_PROPERTY)!=null){
        	PASSWORD_COMPLEXITY=properties.getProperty(SECURITY_PASSWORD_COMPLEXITY_PROPERTY);
        }
        
        if(properties.getProperty(SECURITY_PASSWORD_COMPLEXITY_MESSAGE_PROPERTY)!=null){
        	PASSWORD_COMPLEXITY_MESSAGE=properties.getProperty(SECURITY_PASSWORD_COMPLEXITY_MESSAGE_PROPERTY);
        }
    
     // Create providers
        List<AuthenticationProvider> tempProviders = new ArrayList<AuthenticationProvider>();
        for(String prov: providerArray){
        	String name = providerMap.get(prov).get("provider." + prov + ".name");
        	String id = providerMap.get(prov).get("provider." + prov + ".id");
        	String type = providerMap.get(prov).get("provider." + prov + ".type");
    		 
        	if(type.equals("db")){
        		XnatDatabaseUserDetailsService detailsService = new XnatDatabaseUserDetailsService();
            	detailsService.setDataSource(XDAT.getDataSource());
        		
        		XnatDatabaseAuthenticationProvider sha2DatabaseAuthProvider = new XnatDatabaseAuthenticationProvider();
            	ShaPasswordEncoder encoder = new ShaPasswordEncoder(256);
            	sha2DatabaseAuthProvider.setUserDetailsService(detailsService);
            	sha2DatabaseAuthProvider.setPasswordEncoder(encoder);
            	sha2DatabaseAuthProvider.setName(name);
            	sha2DatabaseAuthProvider.setID(id);
            	tempProviders.add(sha2DatabaseAuthProvider);
            	
            	XnatDatabaseAuthenticationProvider sha2ObfuscatedDatabaseAuthProvider = new XnatDatabaseAuthenticationProvider();
            	XnatObfuscatedPasswordEncoder encoder2 = new XnatObfuscatedPasswordEncoder(true);
            	sha2ObfuscatedDatabaseAuthProvider.setUserDetailsService(detailsService);
            	sha2ObfuscatedDatabaseAuthProvider.setPasswordEncoder(encoder2);
            	sha2ObfuscatedDatabaseAuthProvider.setName(name);
            	sha2ObfuscatedDatabaseAuthProvider.setID(id);
            	tempProviders.add(sha2ObfuscatedDatabaseAuthProvider);
            	
            	XnatDatabaseAuthenticationProvider obfuscatedDatabaseAuthProvider = new XnatDatabaseAuthenticationProvider();
            	XnatObfuscatedPasswordEncoder encoder3 = new XnatObfuscatedPasswordEncoder();
            	obfuscatedDatabaseAuthProvider.setUserDetailsService(detailsService);
            	obfuscatedDatabaseAuthProvider.setPasswordEncoder(encoder3);
            	obfuscatedDatabaseAuthProvider.setName(name);
            	obfuscatedDatabaseAuthProvider.setID(id);
            	tempProviders.add(obfuscatedDatabaseAuthProvider);
            	
            	XnatDatabaseAuthenticationProvider plaintextDatabaseAuthProvider = new XnatDatabaseAuthenticationProvider();
            	PlaintextPasswordEncoder encoder4 = new PlaintextPasswordEncoder();
            	plaintextDatabaseAuthProvider.setUserDetailsService(detailsService);
            	plaintextDatabaseAuthProvider.setPasswordEncoder(encoder4);
            	plaintextDatabaseAuthProvider.setName(name);
            	plaintextDatabaseAuthProvider.setID(id);
            	tempProviders.add(plaintextDatabaseAuthProvider);
        		
                AliasTokenAuthenticationProvider aliasTokenAuthenticationProvider = new AliasTokenAuthenticationProvider();
                aliasTokenAuthenticationProvider.setId("token");
                aliasTokenAuthenticationProvider.setName("token");
                tempProviders.add(aliasTokenAuthenticationProvider);
        	}
        	else if (type.equals("ldap")){
        		try{
	        		String address = providerMap.get(prov).get("provider." + prov + ".address");       		
	        		XnatLdapUserDetailsMapper ldapUserDetailsContextMapper = new XnatLdapUserDetailsMapper(id);
	            	XnatLdapAuthoritiesPopulator ldapAuthoritiesPopulator = new XnatLdapAuthoritiesPopulator();
	            	DefaultSpringSecurityContextSource ldapServer = new DefaultSpringSecurityContextSource(address);
	            	ldapServer.setUserDn(providerMap.get(prov).get("provider." + prov + ".userdn"));
	            	ldapServer.setPassword(providerMap.get(prov).get("provider." + prov + ".password"));
	            	ldapServer.afterPropertiesSet();
	            	FilterBasedLdapUserSearch ldapSearchBean = new FilterBasedLdapUserSearch(providerMap.get(prov).get("provider." + prov + ".search.base"),
	            			providerMap.get(prov).get("provider." + prov + ".search.filter"), ldapServer);
	            	BindAuthenticator ldapBindAuthenticator = new BindAuthenticator(ldapServer);
	            	ldapBindAuthenticator.setUserSearch(ldapSearchBean);
	            	XnatLdapAuthenticationProvider ldapAuthProvider = new XnatLdapAuthenticationProvider(ldapBindAuthenticator, ldapAuthoritiesPopulator);
	            	ldapAuthProvider.setUserDetailsContextMapper(ldapUserDetailsContextMapper);
	            	ldapAuthProvider.setName(name);
	            	ldapAuthProvider.setID(id);
	            	tempProviders.add(ldapAuthProvider);
        		}
        		catch(Exception e){
        			logger.error(e);
        		}
        	}
        }
        setProviders(tempProviders);
    }

	public void setProperties(String filename) {
		String path = "../../../../../../"+filename;
		properties = new Properties();
		try {
			URL url = getClass().getResource(path);
			properties.load(url.openStream());
		} catch (IOException e) {
			logger.error(e);
		}
	}
	
	@Override
	public Authentication doAuthentication(Authentication authentication) throws AuthenticationException {
        Class<? extends Authentication> toTest = authentication.getClass();
        AuthenticationException lastException = null;
        Authentication result = null;
        
        final String auth_id=authentication.toString().intern();
        failures.checkFailedLoginAttempts(auth_id);

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
            if (eraseCredentialsAfterAuthentication && (result instanceof CredentialsContainer)) {
                // Authentication is complete. Remove credentials and other secret data from authentication
                ((CredentialsContainer)result).eraseCredentials();
            }

            eventPublisher.publishAuthenticationSuccess(result);
            return result;
        }else{
        	//increment failed login attempt
        	failures.addFailedLoginAttempt(auth_id);
        }

        // Parent was null, or didn't authenticate (or throw an exception).

        if (lastException == null) {
            lastException = new ProviderNotFoundException(messages.getMessage("ProviderManager.providerNotFound",
                        new Object[] {toTest.getName()}, "No AuthenticationProvider found for {0}"));
        }

        eventPublisher.publishAuthenticationFailure(lastException, authentication);

        throw lastException;
    }
	
    private void copyDetails(Authentication source, Authentication dest) {
        if ((dest instanceof AbstractAuthenticationToken) && (dest.getDetails() == null)) {
            AbstractAuthenticationToken token = (AbstractAuthenticationToken) dest;

            token.setDetails(source.getDetails());
        }
    }
    
    private static final class NullEventPublisher implements AuthenticationEventPublisher {
        public void publishAuthenticationFailure(AuthenticationException exception, Authentication authentication) {}
        public void publishAuthenticationSuccess(Authentication authentication) {}
    }


	private static class FailedAttemptsManager {
		private Map<String,List<Date>> cached_attempts=Maps.newConcurrentMap();//cached failed login attempts... should be cleared when 
		/**
		 * Increments failed Login count
         * @param id    The ID of the failed login attempt.
		 *
		 */
		private synchronized void addFailedLoginAttempt(final String id){
			if(MAX_FAILED_LOGIN_ATTEMPTS>0 && StringUtils.isNotEmpty(id)){
				if(!cached_attempts.containsKey(id)){
					cached_attempts.put(id, Lists.newArrayList(Calendar.getInstance().getTime()));
				}else{
					cached_attempts.get(id).add(Calendar.getInstance().getTime());
				}
			}
		}
		
		private synchronized void checkFailedLoginAttempts(final String id) throws AuthenticationException{
			if(MAX_FAILED_LOGIN_ATTEMPTS>0 && cached_attempts.containsKey(id)){
				Iterator<Date> iter = cached_attempts.get(id).iterator();
				int counter=0;
				while(iter.hasNext()){
					Date d=iter.next();
					if(d.after(DateUtils.addSeconds(Calendar.getInstance().getTime(), LOCKOUT_DURATION))){
						counter++;
					}else{
						cached_attempts.get(id).remove(d);
					}
				}
				
				if(counter>=MAX_FAILED_LOGIN_ATTEMPTS){
					throw new LockedException("User exceeded maximum username/password attempts");
				}
			}
		}
	}
}
