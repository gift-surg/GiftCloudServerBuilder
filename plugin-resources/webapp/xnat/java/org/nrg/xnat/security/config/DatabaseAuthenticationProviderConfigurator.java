/*
 * org.nrg.xnat.security.config.DatabaseAuthenticationProviderConfigurator
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.security.config;

import org.nrg.xdat.XDAT;
import org.nrg.xdat.security.ObfuscatedPasswordEncoder;
import org.nrg.xnat.security.provider.XnatDatabaseAuthenticationProvider;
import org.nrg.xnat.security.userdetailsservices.XnatDatabaseUserDetailsService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.ReflectionSaltSource;
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

        ReflectionSaltSource saltSource = new ReflectionSaltSource();
        saltSource.setUserPropertyToUse("salt");

        XnatDatabaseAuthenticationProvider sha2DatabaseAuthProvider = new XnatDatabaseAuthenticationProvider();
        ShaPasswordEncoder encoder = new ShaPasswordEncoder(256);
        sha2DatabaseAuthProvider.setUserDetailsService(detailsService);
        sha2DatabaseAuthProvider.setPasswordEncoder(encoder);
        sha2DatabaseAuthProvider.setName(name);
        sha2DatabaseAuthProvider.setProviderId(id);
        sha2DatabaseAuthProvider.setSaltSource(saltSource);
        providers.add(sha2DatabaseAuthProvider);

        XnatDatabaseAuthenticationProvider sha2ObfuscatedDatabaseAuthProvider = new XnatDatabaseAuthenticationProvider();
        ObfuscatedPasswordEncoder encoder2 = new ObfuscatedPasswordEncoder();
        sha2ObfuscatedDatabaseAuthProvider.setUserDetailsService(detailsService);
        sha2ObfuscatedDatabaseAuthProvider.setPasswordEncoder(encoder2);
        sha2ObfuscatedDatabaseAuthProvider.setName(name);
        sha2ObfuscatedDatabaseAuthProvider.setProviderId(id);
        sha2ObfuscatedDatabaseAuthProvider.setSaltSource(saltSource);
        providers.add(sha2ObfuscatedDatabaseAuthProvider);

        return providers;
    }

    @Override
    public List<AuthenticationProvider> getAuthenticationProviders(String id, String name, Map<String, String> properties) {
        return getAuthenticationProviders(id, name);
    }
}
