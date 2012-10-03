package org.nrg.xnat.security.tokens;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class XnatLdapUsernamePasswordAuthenticationToken extends UsernamePasswordAuthenticationToken{
	private static final long serialVersionUID = 4020331737088633595L;
	private String providerId;

	public XnatLdapUsernamePasswordAuthenticationToken(Object principal,
			Object credentials, String providerId) {
		super(principal, credentials);
		this.providerId = providerId;
	}
	
	public String getProviderId(){
		return providerId;
	}
	
	public String toString(){
		return getPrincipal()+providerId;
	}
}
