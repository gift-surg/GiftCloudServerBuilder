/*
 * org.nrg.xnat.security.XnatLdapAuthoritiesPopulator
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
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
