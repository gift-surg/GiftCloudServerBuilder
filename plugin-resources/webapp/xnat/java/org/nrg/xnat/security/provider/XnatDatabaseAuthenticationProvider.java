package org.nrg.xnat.security.provider;

import org.nrg.xnat.security.tokens.XnatDatabaseUsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

public class XnatDatabaseAuthenticationProvider extends DaoAuthenticationProvider{

	@Override
    public boolean supports(Class<? extends Object> authentication) {
        return (XnatDatabaseUsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }
}
