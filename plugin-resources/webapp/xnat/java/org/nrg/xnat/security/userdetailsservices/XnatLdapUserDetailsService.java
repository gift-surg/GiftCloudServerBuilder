package org.nrg.xnat.security.userdetailsservices;

import java.util.Collection;

import org.nrg.xdat.entities.XdatUserAuth;
import org.nrg.xnat.security.XnatLdapAuthoritiesPopulator;
import org.nrg.xnat.security.XnatLdapUserDetailsMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.util.Assert;

public class XnatLdapUserDetailsService implements UserDetailsService{
	
    private final LdapUserSearch userSearch;
    private final LdapAuthoritiesPopulator authoritiesPopulator;
    private UserDetailsContextMapper userDetailsMapper = new XnatLdapUserDetailsMapper();

    public XnatLdapUserDetailsService() {
        this(new FilterBasedLdapUserSearch("","",new LdapContextSource()), new XnatLdapAuthoritiesPopulator());
    }
    
    public XnatLdapUserDetailsService(LdapUserSearch userSearch) {
        this(userSearch, new XnatLdapAuthoritiesPopulator());
    }

    public XnatLdapUserDetailsService(LdapUserSearch userSearch, LdapAuthoritiesPopulator authoritiesPopulator) {
        Assert.notNull(userSearch, "userSearch must not be null");
        Assert.notNull(authoritiesPopulator, "authoritiesPopulator must not be null");
        this.userSearch = userSearch;
        this.authoritiesPopulator = authoritiesPopulator;
    }

    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
        DirContextOperations userData = userSearch.searchForUser(username);

        return userDetailsMapper.mapUserFromContext(userData, username,
                authoritiesPopulator.getGrantedAuthorities(userData, username));
    }

    public void setUserDetailsMapper(UserDetailsContextMapper userDetailsMapper) {
        Assert.notNull(userDetailsMapper, "userDetailsMapper must not be null");
        this.userDetailsMapper = userDetailsMapper;
    }
	
}
