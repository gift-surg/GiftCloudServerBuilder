/**
 * Copyright (c) 2011 Washington University
 */
package org.nrg.xnat.utils;

import javax.inject.Provider;

import org.nrg.xdat.security.XDATUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
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
