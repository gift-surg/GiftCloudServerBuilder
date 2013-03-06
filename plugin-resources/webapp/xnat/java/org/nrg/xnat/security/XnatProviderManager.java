package org.nrg.xnat.security;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.entities.XDATUserDetails;
import org.nrg.xdat.entities.XdatUserAuth;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.services.XdatUserAuthService;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.utils.AuthUtils;
import org.nrg.xnat.security.config.AuthenticationProviderConfigurator;
import org.nrg.xnat.security.provider.XnatLdapAuthenticationProvider;
import org.nrg.xnat.security.tokens.XnatLdapUsernamePasswordAuthenticationToken;
import org.nrg.xnat.security.userdetailsservices.XnatDatabaseUserDetailsService;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.GrantedAuthorityImpl;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class XnatProviderManager extends ProviderManager {

    @Override
    public void afterPropertiesSet() throws Exception {
        if (properties == null) {
            throw new IllegalArgumentException("The list of authentication providers cannot be set to null.");
        }

        if(!StringUtils.isBlank(properties.getProperty(SECURITY_MAX_FAILED_LOGINS_PROPERTY))) {
            AuthUtils.MAX_FAILED_LOGIN_ATTEMPTS=Integer.valueOf(properties.getProperty(SECURITY_MAX_FAILED_LOGINS_PROPERTY));
        }

        if(!StringUtils.isBlank(properties.getProperty(SECURITY_MAX_FAILED_LOGINS_LOCKOUT_DURATION_PROPERTY))) {
            AuthUtils.LOCKOUT_DURATION=Integer.valueOf(properties.getProperty(SECURITY_MAX_FAILED_LOGINS_LOCKOUT_DURATION_PROPERTY));
            if(AuthUtils.LOCKOUT_DURATION>0)AuthUtils.LOCKOUT_DURATION=-(AuthUtils.LOCKOUT_DURATION); //LOCKOUT must be negative for date comparison to work
        }

        if(!StringUtils.isBlank(properties.getProperty(SECURITY_PASSWORD_EXPIRATION_PROPERTY))) {
            PASSWORD_EXPIRATION=properties.getProperty(SECURITY_PASSWORD_EXPIRATION_PROPERTY);
        }

        String commaDelineatedProviders = properties.getProperty("provider.providers.enabled");
        assert !StringUtils.isBlank(commaDelineatedProviders) : "You must specify at least one authentication provider configuration.";
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

            assert !StringUtils.isBlank(name) : "You must provide a name for all authentication provider configurations";
            assert !StringUtils.isBlank(id) : "You must provide an ID for all authentication provider configurations";
            assert !StringUtils.isBlank(type) : "You must provide a type for all authentication provider configurations";

            if (_configurators.containsKey(type)) {
                AuthenticationProviderConfigurator configurator = _configurators.get(type);
                providers.addAll(configurator.getAuthenticationProviders(id, name, providerMap.get(prov)));
            }
        }

        if (_standaloneProviders != null) {
            providers.addAll(_standaloneProviders);
        }

        setProviders(providers);
    }

    @Override
    public Authentication doAuthentication(Authentication authentication) throws AuthenticationException {
        Class<? extends Authentication> toTest = authentication.getClass();
        AuthenticationException lastException = null;
        Authentication result = null;
        List<AuthenticationProvider> providers = new ArrayList<AuthenticationProvider>();

        // HACK: This is a hack to work around open XNAT auth issue. If this is a bare un/pw auth token, use anon auth.
        if (authentication.getClass() == UsernamePasswordAuthenticationToken.class && authentication.getName().equalsIgnoreCase("guest")) {
            final AnonymousAuthenticationProvider anonymousAuthenticationProvider = XDAT.getContextService().getBean(AnonymousAuthenticationProvider.class);
            providers.add(anonymousAuthenticationProvider);
            authentication = new AnonymousAuthenticationToken(anonymousAuthenticationProvider.getKey(), authentication.getPrincipal(), Arrays.<GrantedAuthority>asList(new GrantedAuthorityImpl("ROLE_ANONYMOUS")));
        } else {
            for (AuthenticationProvider candidate : getProviders()) {
                if (!candidate.supports(toTest)) {
                    continue;
                }
                if(authentication instanceof XnatLdapUsernamePasswordAuthenticationToken){
                    if (!(candidate instanceof XnatLdapAuthenticationProvider)) {
                        continue;
                    }
                    XnatLdapAuthenticationProvider ldapCandidate = (XnatLdapAuthenticationProvider) candidate;
                    if (!((XnatLdapUsernamePasswordAuthenticationToken) authentication).getProviderId().equalsIgnoreCase(ldapCandidate.getProviderId())) {
                        //This is a different LDAP provider than the one that was selected.
                        continue;
                    }
                }
                providers.add(candidate);
            }
        }

        assert providers.size() > 0: "No provider found for authentication of type " + authentication.getClass().getSimpleName();

        for (AuthenticationProvider provider : providers) {
            _log.debug("Authentication attempt using " + provider.getClass().getName());

            try {
                result = provider.authenticate(authentication);
                if (result != null) {
                    if (_log.isDebugEnabled()) {
                        _log.debug("Found a provider that worked for " + authentication.getName() + ": " + provider.getClass().getSimpleName());
                    }

                    copyDetails(authentication, result);
                    break;
                }
            } catch (AccountStatusException exception) {
                _log.warn("Error occurred authenticating login request", exception);
                lastException = exception;
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
                    _log.error("Error occurred sending new user request email", exception);
                }
                lastException = e;

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

            eventPublisher.publishAuthenticationSuccess(authentication);

            return result;
            
        } else {
            // Parent was null, or didn't authenticate (or throw an exception).
            if (lastException == null) {
                lastException = new ProviderNotFoundException(messages.getMessage("ProviderManager.providerNotFound",
                        new Object[] {toTest.getName()}, "No AuthenticationProvider found for {0}"));
            }

            eventPublisher.publishAuthenticationFailure(lastException, authentication);
            throw lastException;
        }
    }

    public void setProperties(List<String> fileNames) {
        properties = new Properties();
        for (String filename : fileNames) {
            String path = "../../../../../../" + filename;
            URL url = getClass().getResource(path);
            if (url != null) {
                try {
                    properties.load(url.openStream());
                } catch (IOException e) {
                    _log.error(e);
                }
            }
        }
    }

    public void setAuthenticationProviderConfigurators(Map<String, AuthenticationProviderConfigurator> configurators) {
        _configurators = configurators;
    }
    	
    public void setStandaloneProviders(List<AuthenticationProvider> providers) {
        _standaloneProviders = providers;
    }

    public String getExpirationInterval(){
        return PASSWORD_EXPIRATION;
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
            provider=((XnatLdapUsernamePasswordAuthenticationToken)authentication).getProviderId();
            method=XdatUserAuthService.LDAP;
        }else{
            provider=XnatDatabaseUserDetailsService.DB_PROVIDER;
            method=XdatUserAuthService.LOCALDB;
        }

        return XDAT.getXdatUserAuthService().getUserByNameAndAuth(u, method, provider);
    }

    private void copyDetails(Authentication source, Authentication destination) {
        if ((destination instanceof AbstractAuthenticationToken) && (destination.getDetails() == null)) {
            AbstractAuthenticationToken token = (AbstractAuthenticationToken) destination;

            token.setDetails(source.getDetails());
}
    }

    private static final class AuthenticationAttemptEventPublisher implements AuthenticationEventPublisher {

        private final FailedAttemptsManager failedAttemptsManager = new FailedAttemptsManager();
        private final LastSuccessfulLoginManager lastSuccessfulLoginManager = new LastSuccessfulLoginManager();

        public void publishAuthenticationFailure(AuthenticationException exception, Authentication authentication) {
            //increment failed login attempt
            failedAttemptsManager.addFailedLoginAttempt(authentication);
        }

        public void publishAuthenticationSuccess(Authentication authentication) {
            failedAttemptsManager.clearCount(authentication);
            lastSuccessfulLoginManager.updateLastSuccessfulLogin(authentication);
        }
    }

    private static final class LastSuccessfulLoginManager {
        private void updateLastSuccessfulLogin(Authentication auth) {
            XdatUserAuth ua = getUserByAuth(auth);
            if (ua != null) {
                Date now = java.util.Calendar.getInstance(TimeZone.getDefault()).getTime();
                ua.setLastSuccessfulLogin(now);
                XDAT.getXdatUserAuthService().update(ua);
            }
        }
    }

    private static final class FailedAttemptsManager {
        /**
         * Increments failed Login count
         *
         * @param auth The authentication that failed.
         */
        private synchronized void addFailedLoginAttempt(final Authentication auth) {
            XdatUserAuth ua = getUserByAuth(auth);
            if (ua != null) {
                if (AuthUtils.MAX_FAILED_LOGIN_ATTEMPTS > 0) {
                    ua.setFailedLoginAttempts(ua.getFailedLoginAttempts() + 1);
                    XDAT.getXdatUserAuthService().update(ua);
                }

                if (StringUtils.isNotEmpty(ua.getXdatUsername())) {
                    Integer uid = XDATUser.getUserid(ua.getXdatUsername());
                    if (uid != null) {
                        try {
                            if (ua.getFailedLoginAttempts().equals(AuthUtils.MAX_FAILED_LOGIN_ATTEMPTS)) {
                                String expiration = TurbineUtils.getDateTimeFormatter().format(DateUtils.addMilliseconds(GregorianCalendar.getInstance().getTime(), -(AuthUtils.LOCKOUT_DURATION)));
                                System.out.println("Locked out " + ua.getXdatUsername() + " user account until " + expiration);
                                AdminUtils.sendAdminEmail(ua.getXdatUsername() + " account temporarily disabled.", "User " + ua.getXdatUsername() + " has been temporarily disabled due to excessive failed login attempts. The user's account will be automatically enabled at " + expiration + ".");
                            }
                        } catch (Exception e) {
                            //ignore
                        }
                    }
                }
            }
        }

        public void clearCount(final Authentication auth) {
            if (AuthUtils.MAX_FAILED_LOGIN_ATTEMPTS > 0) {
                XdatUserAuth ua = getUserByAuth(auth);
                if (ua != null) {
                    ua.setFailedLoginAttempts(0);
                    XDAT.getXdatUserAuthService().update(ua);
                }
            }
        }
    }

    private static final String SECURITY_MAX_FAILED_LOGINS_LOCKOUT_DURATION_PROPERTY = "security.max_failed_logins_lockout_duration";
    private static final String SECURITY_MAX_FAILED_LOGINS_PROPERTY = "security.max_failed_logins";
    private static final String SECURITY_PASSWORD_EXPIRATION_PROPERTY = "security.password_expiration";

    private static String PASSWORD_EXPIRATION = "-1";

    private static final Log _log = LogFactory.getLog(XnatProviderManager.class);

    protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

    private final AuthenticationEventPublisher eventPublisher = new AuthenticationAttemptEventPublisher();
    private Properties properties;
    private Map<String, AuthenticationProviderConfigurator> _configurators;
    private List<AuthenticationProvider> _standaloneProviders;
}
