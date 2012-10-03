package org.nrg.xnat.security.provider;

import org.nrg.xdat.services.XdatUserAuthService;
import org.nrg.xnat.security.tokens.XnatLdapUsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;

public class XnatLdapAuthenticationProvider extends LdapAuthenticationProvider implements XnatAuthenticationProvider {
	
	private String displayName = "";
	private String ID = "";
	
	public XnatLdapAuthenticationProvider(LdapAuthenticator authenticator) {
		super(authenticator);
	}
	
	public XnatLdapAuthenticationProvider(LdapAuthenticator authenticator, LdapAuthoritiesPopulator authoritiesPopulator){
		super(authenticator,authoritiesPopulator);
	}

	@Override
	public boolean supports(Class<? extends Object> authentication) {
		boolean supports = false;
		if(XnatLdapUsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication)){
			supports = true;
		}
		return supports;
	}
	
	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		Authentication auth = super.authenticate(authentication);
		return auth;
	}
	
	@Override
	public String toString(){
		return getName();
	}
	
	public void setName(String newName){
		displayName = newName;
	}
	
	public void setID(String newID){
		ID = newID;
}
	
    @Override
    public String getName() {
        return displayName;
    }

	public String getID(){
		return ID;
	}

    @Override
    public String getAuthMethod() {
        return XdatUserAuthService.LDAP;
    }

    /**
     * Indicates whether the provider should be visible to and selectable by users. <b>false</b> usually indicates an
     * internal authentication provider, e.g. token authentication.
     *
     * @return <b>true</b> if the provider should be visible to and usable by users.
     */
    @Override
    public boolean isVisible() {
        return true;
    }
}
