package org.nrg.xnat.security.config;

public abstract class AbstractAuthenticationProviderConfigurator implements AuthenticationProviderConfigurator {
    private String _configuratorId;

    @Override
    public String getConfiguratorId() {
        return _configuratorId;
    }

    @Override
    public void setConfiguratorId(String configuratorId) {
        _configuratorId = configuratorId;
    }
}
