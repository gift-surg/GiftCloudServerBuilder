package org.nrg.xnat.security.provider;

import org.nrg.xnat.security.tokens.XnatLdapUsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.ldap.authentication.*;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

public class XnatLdapAuthenticationProvider extends LdapAuthenticationProvider{

	public XnatLdapAuthenticationProvider(LdapAuthenticator authenticator) {
		super(authenticator);
	}
	
	public XnatLdapAuthenticationProvider(LdapAuthenticator authenticator, LdapAuthoritiesPopulator authoritiesPopulator){
		super(authenticator,authoritiesPopulator);
	}

	@Override
	public boolean supports(Class<? extends Object> authentication) {
	    return (XnatLdapUsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
	}
	
	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		Authentication auth = super.authenticate(authentication);
		return auth;
	}
}
