package org.nrg.xnat.security.tokens;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class XnatLdapUsernamePasswordAuthenticationToken extends UsernamePasswordAuthenticationToken{

	public XnatLdapUsernamePasswordAuthenticationToken(Object principal,
			Object credentials) {
		super(principal, credentials);
	}

}
