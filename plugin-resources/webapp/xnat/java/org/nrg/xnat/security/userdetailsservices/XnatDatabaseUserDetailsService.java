package org.nrg.xnat.security.userdetailsservices;

import org.nrg.xdat.XDAT;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.jdbc.JdbcDaoImpl;

public class XnatDatabaseUserDetailsService extends JdbcDaoImpl implements UserDetailsService{
	
	@Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
		return (XDAT.getXdatUserAuthService()).getUserDetailsByNameAndAuth(username, "localdb","");
    }
}
