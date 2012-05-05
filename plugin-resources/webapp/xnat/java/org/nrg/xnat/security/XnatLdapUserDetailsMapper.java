package org.nrg.xnat.security;

import java.util.Collection;
import org.apache.log4j.Logger;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.entities.XDATUserDetails;
import org.nrg.xdat.entities.XdatUserAuth;
import org.nrg.xdat.services.XdatUserAuthService;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;

public class XnatLdapUserDetailsMapper extends LdapUserDetailsMapper {
	private String authMethodId="";
	
	public XnatLdapUserDetailsMapper(){
		super();
	}
	
	public XnatLdapUserDetailsMapper(String authMethodId){
		super();
		this.authMethodId = authMethodId;
	}

	static org.apache.log4j.Logger logger = Logger.getLogger(XnatLdapUserDetailsMapper.class);
    public XDATUserDetails mapUserFromContext(DirContextOperations ctx, String username, Collection<GrantedAuthority> authorities) {
        UserDetails user = super.mapUserFromContext(ctx, username, authorities);
        XdatUserAuth userAuth = XDAT.getXdatUserAuthService().getUserByNameAndAuth(user.getUsername(), XdatUserAuthService.LDAP, authMethodId);
        try {
			return new XDATUserDetails(userAuth.getXdatUsername());
		} catch (Exception e) {
			logger.error("",e);
		}
        return null;
    }
}