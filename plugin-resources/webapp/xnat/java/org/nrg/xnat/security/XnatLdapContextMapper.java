package org.nrg.xnat.security;

import java.util.Collection;

import javax.naming.NamingException;

import org.nrg.xdat.XDAT;
import org.nrg.xdat.entities.XDATUserDetails;
import org.nrg.xdat.services.XdatUserAuthService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.DirContextAdapter;

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
		return user;   }

    public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
    	throw new UnsupportedOperationException("LdapUserDetailsMapper only supports reading from a context.");
    }
}