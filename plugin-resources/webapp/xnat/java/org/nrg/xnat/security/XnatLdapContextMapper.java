package org.nrg.xnat.security;

import java.util.Collection;

import javax.naming.NamingException;

import org.nrg.xdat.XDAT;
import org.nrg.xdat.entities.XDATUserDetails;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.DirContextAdapter;

public class XnatLdapContextMapper implements UserDetailsContextMapper {
	
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username, Collection<GrantedAuthority> authorities) {
    	String email = ctx.getObjectAttribute("mail").toString();
    	XDATUserDetails user = XDAT.getXdatUserAuthService().getUserDetailsByNameAndAuth(username, "ldap", email);
		return user;   }

    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
    	throw new UnsupportedOperationException("LdapUserDetailsMapper only supports reading from a context.");
    }
}