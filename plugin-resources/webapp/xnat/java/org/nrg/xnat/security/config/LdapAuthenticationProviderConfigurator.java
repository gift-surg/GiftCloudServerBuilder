package org.nrg.xnat.security.config;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.xnat.security.XnatLdapAuthoritiesPopulator;
import org.nrg.xnat.security.XnatLdapUserDetailsMapper;
import org.nrg.xnat.security.provider.XnatLdapAuthenticationProvider;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class LdapAuthenticationProviderConfigurator extends AbstractAuthenticationProviderConfigurator {
    @Override
    public List<AuthenticationProvider> getAuthenticationProviders(String id, String name) {
        throw new NotImplementedException("You must provide LDAP properties in order to configure an LDAP connection.");
    }

    @Override
    public List<AuthenticationProvider> getAuthenticationProviders(String id, String name, Map<String, String> properties) {
        try {
            String address = properties.get("address");
            XnatLdapUserDetailsMapper ldapUserDetailsContextMapper = new XnatLdapUserDetailsMapper(id);
            XnatLdapAuthoritiesPopulator ldapAuthoritiesPopulator = new XnatLdapAuthoritiesPopulator();
            DefaultSpringSecurityContextSource ldapServer = new DefaultSpringSecurityContextSource(address);
            ldapServer.setUserDn(properties.get("userdn"));
            ldapServer.setPassword(properties.get("password"));
            ldapServer.afterPropertiesSet();
            FilterBasedLdapUserSearch ldapSearchBean = new FilterBasedLdapUserSearch(properties.get("search.base"), properties.get("search.filter"), ldapServer);
            BindAuthenticator ldapBindAuthenticator = new BindAuthenticator(ldapServer);
            ldapBindAuthenticator.setUserSearch(ldapSearchBean);
            XnatLdapAuthenticationProvider ldapAuthProvider = new XnatLdapAuthenticationProvider(ldapBindAuthenticator, ldapAuthoritiesPopulator);
            ldapAuthProvider.setUserDetailsContextMapper(ldapUserDetailsContextMapper);
            ldapAuthProvider.setName(name);
            ldapAuthProvider.setID(id);
            return Arrays.asList(new AuthenticationProvider[] { ldapAuthProvider });
        } catch (Exception exception) {
            _log.error("Something went wrong when configuring the LDAP authentication provider", exception);
            return null;
        }
    }

    private static final Log _log = LogFactory.getLog(LdapAuthenticationProviderConfigurator.class);
}
