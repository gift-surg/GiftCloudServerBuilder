package org.nrg.xnat.security;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

public class XnatLdapAuthoritiesPopulator implements LdapAuthoritiesPopulator {
	
	protected String role = "ROLE_USER";
	
	public Collection<GrantedAuthority> getGrantedAuthorities(
		DirContextOperations userData, String username) {
		GrantedAuthority ga = new GrantedAuthorityImpl(role);
		return Arrays.asList(ga);
	}
	
	public String getRole() {
		return role;
	}
	
	public void setRole(String role) {
		this.role = role;
	}
}