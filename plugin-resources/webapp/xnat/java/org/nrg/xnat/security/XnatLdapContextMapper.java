/*
 * org.nrg.xnat.security.XnatLdapContextMapper
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.security;

import org.nrg.xdat.XDAT;
import org.nrg.xdat.entities.XDATUserDetails;
import org.nrg.xdat.services.XdatUserAuthService;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

import java.util.Collection;

public class XnatLdapContextMapper implements UserDetailsContextMapper {
	private String authMethodId="";
	
	public XnatLdapContextMapper(){
		super();
	}
	
	public XnatLdapContextMapper(String authMethodId){
		super();
		this.authMethodId = authMethodId;
	}
	
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username, Collection<GrantedAuthority> authorities) {
    	String email = ctx.getObjectAttribute("mail").toString();
    	XDATUserDetails user = XDAT.getXdatUserAuthService().getUserDetailsByNameAndAuth(username, XdatUserAuthService.LDAP, authMethodId, email);
		return user;
   }

    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
    	throw new UnsupportedOperationException("LdapUserDetailsMapper only supports reading from a context.");
    }
}