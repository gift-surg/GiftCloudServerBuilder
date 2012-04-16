package org.nrg.xnat.security.provider;

import java.util.regex.Pattern;

import org.nrg.xnat.security.tokens.XnatDatabaseUsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

public class XnatDatabaseAuthenticationProvider extends DaoAuthenticationProvider{

	private String displayName = "";
	private String ID = "";
	private int expiration = -1;
	private Pattern complexity;
	
	@Override
    public boolean supports(Class<?> authentication) {
        return XnatDatabaseUsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
	
	@Override
	public String toString(){
		return displayName;
	}
	
	public void setName(String newName){
		displayName = newName;
	}
	
	public void setID(String newID){
		ID = newID;
	}
	
	public String getID(){
		return ID;
	}

}
