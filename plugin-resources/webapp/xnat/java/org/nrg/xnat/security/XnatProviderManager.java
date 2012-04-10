package org.nrg.xnat.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.xdat.XDAT;
import org.nrg.xnat.security.provider.XnatDatabaseAuthenticationProvider;
import org.nrg.xnat.security.provider.XnatLdapAuthenticationProvider;
import org.nrg.xnat.security.tokens.XnatLdapUsernamePasswordAuthenticationToken;
import org.nrg.xnat.security.userdetailsservices.XnatDatabaseUserDetailsService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.authentication.encoding.PlaintextPasswordEncoder;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.util.Assert;
import java.util.Map;
import java.util.Properties;

public class XnatProviderManager extends ProviderManager {
    private static final Log logger = LogFactory.getLog(XnatProviderManager.class);

    private AuthenticationEventPublisher eventPublisher = new NullEventPublisher();
    protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();
    private AuthenticationManager parent;
    private boolean eraseCredentialsAfterAuthentication = false;
    private Properties properties;
    private List<String> loginOptions = new ArrayList<String>();
    
    @Override
    public void afterPropertiesSet() throws Exception {
        if (properties == null) {
            throw new IllegalArgumentException("The list of authentication providers cannot be set to null.");
        }
        String commaDeliniatedProviders = properties.getProperty("provider.providers.enabled");
        String[] providerArray=commaDeliniatedProviders.split(",");
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
    
     // Create providers
        List<AuthenticationProvider> tempProviders = new ArrayList<AuthenticationProvider>();
        for(String prov: providerArray){
        	String type = providerMap.get(prov).get("provider." + prov + ".type");
    		String title = providerMap.get(prov).get("provider." + prov + ".title"); 
        	if(type.equals("db")){
        		XnatDatabaseUserDetailsService detailsService = new XnatDatabaseUserDetailsService();
            	detailsService.setDataSource(XDAT.getDataSource());
        		
        		XnatDatabaseAuthenticationProvider sha2DatabaseAuthProvider = new XnatDatabaseAuthenticationProvider();
            	ShaPasswordEncoder encoder = new ShaPasswordEncoder(256);
            	sha2DatabaseAuthProvider.setUserDetailsService(detailsService);
            	sha2DatabaseAuthProvider.setPasswordEncoder(encoder);
            	sha2DatabaseAuthProvider.setName(title);
            	tempProviders.add(sha2DatabaseAuthProvider);
            	
            	XnatDatabaseAuthenticationProvider sha2ObfuscatedDatabaseAuthProvider = new XnatDatabaseAuthenticationProvider();
            	XnatObfuscatedPasswordEncoder encoder2 = new XnatObfuscatedPasswordEncoder(true);
            	sha2ObfuscatedDatabaseAuthProvider.setUserDetailsService(detailsService);
            	sha2ObfuscatedDatabaseAuthProvider.setPasswordEncoder(encoder2);
            	sha2ObfuscatedDatabaseAuthProvider.setName(title);
            	tempProviders.add(sha2ObfuscatedDatabaseAuthProvider);
            	
            	XnatDatabaseAuthenticationProvider obfuscatedDatabaseAuthProvider = new XnatDatabaseAuthenticationProvider();
            	XnatObfuscatedPasswordEncoder encoder3 = new XnatObfuscatedPasswordEncoder();
            	obfuscatedDatabaseAuthProvider.setUserDetailsService(detailsService);
            	obfuscatedDatabaseAuthProvider.setPasswordEncoder(encoder3);
            	obfuscatedDatabaseAuthProvider.setName(title);
            	tempProviders.add(obfuscatedDatabaseAuthProvider);
            	
            	XnatDatabaseAuthenticationProvider plaintextDatabaseAuthProvider = new XnatDatabaseAuthenticationProvider();
            	PlaintextPasswordEncoder encoder4 = new PlaintextPasswordEncoder();
            	plaintextDatabaseAuthProvider.setUserDetailsService(detailsService);
            	plaintextDatabaseAuthProvider.setPasswordEncoder(encoder4);
            	plaintextDatabaseAuthProvider.setName(title);
            	tempProviders.add(plaintextDatabaseAuthProvider);
        		
        	}
        	else if (type.equals("ldap")){
        		try{
	        		String address = providerMap.get(prov).get("provider." + prov + ".address");       		
	        		XnatLdapContextMapper ldapUserDetailsContextMapper = new XnatLdapContextMapper();
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
	            	ldapAuthProvider.setName(title);
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
}
