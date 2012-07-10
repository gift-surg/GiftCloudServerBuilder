package org.nrg.xnat.security.config;

import org.springframework.security.authentication.AuthenticationProvider;

import java.util.List;
import java.util.Map;

public interface AuthenticationProviderConfigurator {
    public abstract String getConfiguratorId();
    public abstract void setConfiguratorId(String configuratorId);
    public abstract List<AuthenticationProvider> getAuthenticationProviders(String id, String name);
    public abstract List<AuthenticationProvider> getAuthenticationProviders(String id, String name, Map<String, String> properties);
}
