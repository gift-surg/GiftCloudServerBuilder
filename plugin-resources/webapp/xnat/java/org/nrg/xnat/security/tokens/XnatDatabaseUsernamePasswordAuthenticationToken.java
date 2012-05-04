package org.nrg.xnat.security.tokens;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class XnatDatabaseUsernamePasswordAuthenticationToken extends UsernamePasswordAuthenticationToken{

	public XnatDatabaseUsernamePasswordAuthenticationToken(Object principal,
			Object credentials) {
		super(principal, credentials);
	}

	public String toString(){
		if(getPrincipal()!=null)
			return getPrincipal().toString();
		else
			return "";
	}
}
