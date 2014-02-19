/*
 * org.nrg.xnat.utils.XnatUserProvider
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.utils;

import org.nrg.xdat.security.XDATUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Provider;

public class XnatUserProvider implements Provider<XDATUser> {
    private final Logger logger = LoggerFactory.getLogger(XnatUserProvider.class);
    private final String login;
    private XDATUser user = null;
    
    XnatUserProvider(final String login) {
        this.login = login;
    }
    
    /*
     * (non-Javadoc)
     * @see javax.inject.Provider#get()
     */
    public XDATUser get() {
        if (null == user) {
            try {
                user = new XDATUser(login);
            } catch (Throwable t) {
                logger.error("Unable to retrieve user " + login, t);
                return null;
            }
        }
        return user;
    }
}
