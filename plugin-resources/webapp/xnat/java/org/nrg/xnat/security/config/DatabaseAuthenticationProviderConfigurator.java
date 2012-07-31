package org.nrg.xnat.security.config;

import org.nrg.xdat.XDAT;
import org.nrg.xnat.security.XnatObfuscatedPasswordEncoder;
import org.nrg.xnat.security.alias.AliasTokenAuthenticationProvider;
import org.nrg.xnat.security.provider.XnatDatabaseAuthenticationProvider;
import org.nrg.xnat.security.userdetailsservices.XnatDatabaseUserDetailsService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.encoding.PlaintextPasswordEncoder;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DatabaseAuthenticationProviderConfigurator extends AbstractAuthenticationProviderConfigurator {
    @Override
    public List<AuthenticationProvider> getAuthenticationProviders(String id, String name) {
        List<AuthenticationProvider> providers = new ArrayList<AuthenticationProvider>();

        XnatDatabaseUserDetailsService detailsService = new XnatDatabaseUserDetailsService();
        detailsService.setDataSource(XDAT.getDataSource());

        XnatDatabaseAuthenticationProvider sha2DatabaseAuthProvider = new XnatDatabaseAuthenticationProvider();
        ShaPasswordEncoder encoder = new ShaPasswordEncoder(256);
        sha2DatabaseAuthProvider.setUserDetailsService(detailsService);
        sha2DatabaseAuthProvider.setPasswordEncoder(encoder);
        sha2DatabaseAuthProvider.setName(name);
        sha2DatabaseAuthProvider.setID(id);
        providers.add(sha2DatabaseAuthProvider);

        XnatDatabaseAuthenticationProvider sha2ObfuscatedDatabaseAuthProvider = new XnatDatabaseAuthenticationProvider();
        XnatObfuscatedPasswordEncoder encoder2 = new XnatObfuscatedPasswordEncoder(true);
        sha2ObfuscatedDatabaseAuthProvider.setUserDetailsService(detailsService);
        sha2ObfuscatedDatabaseAuthProvider.setPasswordEncoder(encoder2);
        sha2ObfuscatedDatabaseAuthProvider.setName(name);
        sha2ObfuscatedDatabaseAuthProvider.setID(id);
        providers.add(sha2ObfuscatedDatabaseAuthProvider);

        XnatDatabaseAuthenticationProvider obfuscatedDatabaseAuthProvider = new XnatDatabaseAuthenticationProvider();
        XnatObfuscatedPasswordEncoder encoder3 = new XnatObfuscatedPasswordEncoder();
        obfuscatedDatabaseAuthProvider.setUserDetailsService(detailsService);
        obfuscatedDatabaseAuthProvider.setPasswordEncoder(encoder3);
        obfuscatedDatabaseAuthProvider.setName(name);
        obfuscatedDatabaseAuthProvider.setID(id);
        providers.add(obfuscatedDatabaseAuthProvider);

        XnatDatabaseAuthenticationProvider plaintextDatabaseAuthProvider = new XnatDatabaseAuthenticationProvider();
        PlaintextPasswordEncoder encoder4 = new PlaintextPasswordEncoder();
        plaintextDatabaseAuthProvider.setUserDetailsService(detailsService);
        plaintextDatabaseAuthProvider.setPasswordEncoder(encoder4);
        plaintextDatabaseAuthProvider.setName(name);
        plaintextDatabaseAuthProvider.setID(id);
        providers.add(plaintextDatabaseAuthProvider);

        AliasTokenAuthenticationProvider aliasTokenAuthenticationProvider = new AliasTokenAuthenticationProvider();
        aliasTokenAuthenticationProvider.setId("token");
        aliasTokenAuthenticationProvider.setName("token");
        providers.add(aliasTokenAuthenticationProvider);

        return providers;
    }

    @Override
    public List<AuthenticationProvider> getAuthenticationProviders(String id, String name, Map<String, String> properties) {
        return getAuthenticationProviders(id, name);
    }
}