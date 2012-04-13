package org.nrg.xnat.security.provider;

import java.util.regex.Pattern;

import org.nrg.xnat.security.tokens.XnatDatabaseUsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

public class XnatDatabaseAuthenticationProvider extends DaoAuthenticationProvider{

	private String displayName = "";
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
	
	public void setExpiration(int exp){
		expiration = exp;
	}
	
	public int getExpiration(){
		return expiration;
	}
	
	public void setComplexity(Pattern c){
		complexity = c;
	}
	
	public Pattern getComplexity(){
		return complexity;
	}
}
