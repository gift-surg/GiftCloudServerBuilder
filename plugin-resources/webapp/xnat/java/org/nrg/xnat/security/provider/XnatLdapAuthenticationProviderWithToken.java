package org.nrg.xnat.security.provider;

import org.nrg.xnat.security.tokens.XnatLdapUsernamePasswordAuthenticationToken;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

public class XnatLdapAuthenticationProviderWithToken extends LdapAuthenticationProvider{
	
	public XnatLdapAuthenticationProviderWithToken(LdapAuthenticator authenticator,
			LdapAuthoritiesPopulator authoritiesPopulator) {
		super(authenticator, authoritiesPopulator);
	}

	@Override
	public boolean supports(Class<? extends Object> authentication) {
	    return (XnatLdapUsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
	}
}
