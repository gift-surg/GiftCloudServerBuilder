package org.nrg.xnat.security.provider;

import java.util.regex.Pattern;

import org.nrg.xnat.security.tokens.XnatDatabaseUsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
	
	public void setID(String newID){
		ID = newID;
	}
	
    @Override
	public String getID(){
		return ID;
	}
	
    private String displayName = "";
    private String ID = "";
    private int expiration = -1;
    private Pattern complexity;
}
