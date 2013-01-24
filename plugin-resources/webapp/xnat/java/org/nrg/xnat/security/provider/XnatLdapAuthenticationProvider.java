package org.nrg.xnat.security.provider;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.xdat.entities.XDATUserDetails;
import org.nrg.xdat.services.XdatUserAuthService;
import org.nrg.xnat.security.tokens.XnatLdapUsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

public class XnatLdapAuthenticationProvider extends LdapAuthenticationProvider implements XnatAuthenticationProvider {
	
	public XnatLdapAuthenticationProvider(LdapAuthenticator authenticator) {
		super(authenticator);
	}
	
	public XnatLdapAuthenticationProvider(LdapAuthenticator authenticator, LdapAuthoritiesPopulator authoritiesPopulator){
		super(authenticator,authoritiesPopulator);
	}

	@Override
	public boolean supports(Class<?> authentication) {
		boolean supports = false;
		if(XnatLdapUsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication)){
			supports = true;
		}
		return supports;
	}
	
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
	Authentication auth = super.authenticate(authentication);
	if (_log.isDebugEnabled()) {
	    _log.debug("Found auth object of type: " + auth.getClass() + " (principal is: "
		    + auth.getPrincipal().getClass() + ")");
	}

	/*
	 * Unlike the other authentication providers, we hafta do this check here. This class is on a different branch
	 * of the class hierarchy, and so doesn't inherit additionalAuthenticationChecks.
	 */
	UserDetails userDetails = (UserDetails) auth.getPrincipal();
	if (!XDATUserDetails.class.isAssignableFrom(userDetails.getClass())) {
	    throw new AuthenticationServiceException("User details class is not of a type I know how to handle: "
		    + userDetails.getClass());
	}
	final XDATUserDetails xdatUserDetails = (XDATUserDetails) userDetails;
	xdatUserDetails.validateUserLogin();

	return auth;
    }
	
	@Override
	public String toString(){
		return getName();
	}
	
    @Override
    public String getName() {
        return _displayName;
	}
	
    public void setName(String newName){
        _displayName = newName;
}
	
    @Override
	public String getProviderId(){
		return _providerId;
    }

    public void setProviderId(String providerId){
        _providerId = providerId;
	}

    @Override
    public String getAuthMethod() {
        return XdatUserAuthService.LDAP;
    }

    /**
     * Indicates whether the provider should be visible to and selectable by users. <b>false</b> usually indicates an
     * internal authentication provider, e.g. token authentication.
     *
     * @return <b>true</b> if the provider should be visible to and usable by users.
     */
    @Override
    public boolean isVisible() {
        return true;
    }

    private static final Log _log = LogFactory.getLog(XnatLdapAuthenticationProvider.class);

    private String _displayName = "";
    private String _providerId = "";
}
