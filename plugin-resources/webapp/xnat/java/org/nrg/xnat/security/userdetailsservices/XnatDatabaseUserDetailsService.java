package org.nrg.xnat.security.userdetailsservices;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nrg.xdat.XDAT;
import org.nrg.xnat.security.PasswordExpiredException;
import org.nrg.xnat.security.XnatProviderManager;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.jdbc.JdbcDaoImpl;

public class XnatDatabaseUserDetailsService extends JdbcDaoImpl implements UserDetailsService{
	
	@Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException, PasswordExpiredException {
		UserDetails user = (XDAT.getXdatUserAuthService()).getUserDetailsByNameAndAuth(username, "localdb","");
        return user;
    }
}
