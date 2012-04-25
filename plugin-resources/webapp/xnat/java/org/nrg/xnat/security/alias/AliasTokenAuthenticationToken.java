package org.nrg.xnat.security.alias;

import org.nrg.xdat.XDAT;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class AliasTokenAuthenticationToken extends UsernamePasswordAuthenticationToken{
	public AliasTokenAuthenticationToken(Object principal, Object credentials) {
		super(principal, credentials);
	}

	public String toString(){
		return getPrincipal().toString();
	}
}
