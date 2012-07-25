package org.nrg.xnat.security;

import org.springframework.security.core.AuthenticationException;

public class NewLdapAccountNotAutoEnabledException extends AuthenticationException 
{
	private static final long serialVersionUID = 1L;

	public NewLdapAccountNotAutoEnabledException(String msg) 
	{
		super(msg);
	}
}
