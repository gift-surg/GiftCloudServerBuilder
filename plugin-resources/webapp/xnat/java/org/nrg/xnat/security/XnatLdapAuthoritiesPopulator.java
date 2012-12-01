package org.nrg.xnat.security;

import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

import java.util.Arrays;
import java.util.Collection;

public class XnatLdapAuthoritiesPopulator implements LdapAuthoritiesPopulator {
	
	public Collection<GrantedAuthority> getGrantedAuthorities(
		DirContextOperations userData, String username) {
		GrantedAuthority ga = new GrantedAuthorityImpl("ROLE_USER");
		return Arrays.asList(ga);
	}
	}
