package org.nrg.xnat.security.tokens;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class XnatLdapUsernamePasswordAuthenticationToken extends UsernamePasswordAuthenticationToken{
	private String providerName;

	public XnatLdapUsernamePasswordAuthenticationToken(Object principal,
			Object credentials) {
		this(principal, credentials,"");
	}

	public XnatLdapUsernamePasswordAuthenticationToken(Object principal,
			Object credentials, String name) {
		super(principal, credentials);
		providerName = name;
	}
	
	public String getProviderName(){
		return providerName;
	}
}
