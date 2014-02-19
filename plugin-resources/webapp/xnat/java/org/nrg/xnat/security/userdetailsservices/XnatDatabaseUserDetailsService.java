/*
 * org.nrg.xnat.security.userdetailsservices.XnatDatabaseUserDetailsService
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.security.userdetailsservices;

import org.nrg.xdat.XDAT;
import org.nrg.xdat.services.XdatUserAuthService;
import org.nrg.xnat.security.PasswordExpiredException;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.jdbc.JdbcDaoImpl;

public class XnatDatabaseUserDetailsService extends JdbcDaoImpl implements UserDetailsService{
	
	public static final String DB_PROVIDER = "";
	
	@Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException, PasswordExpiredException {
		UserDetails user = (XDAT.getXdatUserAuthService()).getUserDetailsByNameAndAuth(username, XdatUserAuthService.LOCALDB, DB_PROVIDER);
        return user;
    }
}
