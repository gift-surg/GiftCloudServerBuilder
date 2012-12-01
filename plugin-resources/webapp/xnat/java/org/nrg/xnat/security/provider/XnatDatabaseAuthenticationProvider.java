package org.nrg.xnat.security.provider;

import org.nrg.xdat.services.XdatUserAuthService;
import org.nrg.xnat.security.tokens.XnatDatabaseUsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

public class XnatDatabaseAuthenticationProvider extends DaoAuthenticationProvider implements XnatAuthenticationProvider {

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
	
	@Override
    public boolean supports(Class<?> authentication) {
        return XnatDatabaseUsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
	
	@Override
	public String toString(){
		return getName();
	}

    @Override
    public String getName() {
		return displayName;
	}
	
	public void setName(String newName){
		displayName = newName;
	}
	
    @Override
    public String getProviderId() {
        return _providerId;
	}
	
    public void setProviderId(String providerId) {
        _providerId = providerId;
	}
	
    @Override
    public String getAuthMethod() {
        return XdatUserAuthService.LOCALDB;
    }

    private String displayName = "";
    private String _providerId = "";
}
