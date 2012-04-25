/**
 * XnatAuthenticationProvider
 * (C) 2012 Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD License
 *
 * Created on 4/23/12 by rherri01
 */
package org.nrg.xnat.security.provider;

import org.springframework.security.authentication.AuthenticationProvider;

public interface XnatAuthenticationProvider extends AuthenticationProvider {
    abstract public String getName();
    abstract public String getID();

    /**
     * Indicates whether the provider should be visible to and selectable by users. <b>false</b> usually indicates an
     * internal authentication provider, e.g. token authentication.
     * @return <b>true</b> if the provider should be visible to and usable by users.
     */
    abstract public boolean isVisible();
}
