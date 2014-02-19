/*
 * org.nrg.xnat.utils.CachedUserFactory
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.utils;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;

import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.security.XDATUser.UserNotFoundException;
import org.nrg.xft.exception.DBPoolException;

import java.sql.SQLException;

@Deprecated
public class CachedUserFactory implements UserFactory {
    private static final String CACHE_NAME = "XDATUser";
    private static final long EXPIRY_SECONDS = 120;
    private final Cache userCache;

    public CachedUserFactory(final CacheManager manager) {
        final CacheManager m = null == manager ? CacheManager.getInstance() : manager;
        synchronized (m) {
            if (m.cacheExists(CACHE_NAME)) {
                userCache = m.getCache(CACHE_NAME);
            } else {
                final CacheConfiguration config = new CacheConfiguration(CACHE_NAME, 0)
                .copyOnRead(false).copyOnWrite(false)
                .eternal(false)
                .overflowToDisk(false)
                .timeToLiveSeconds(EXPIRY_SECONDS);
                final Cache cache = new Cache(config);
                m.addCache(cache);
                userCache = cache;
            }
        }
    }

    public CachedUserFactory() {
        this(null);
    }

    /* (non-Javadoc)
     * @see org.nrg.xnat.utils.UserFactoryI#getUser(java.lang.String)
     */
    @Override
    public XDATUser getUser(final String login) throws SQLException,UserNotFoundException {
        final Element ue = userCache.get(login);
        if (null != ue) {
            return (XDATUser)ue.getValue();
        } else {
            try {
                final XDATUser u = new XDATUser(login);
                userCache.put(new Element(login, u));
                return u;
            } catch (UserNotFoundException e) {
                throw e;
            } catch (SQLException e) {
                throw e;
            } catch (DBPoolException e) {
                throw new SQLException(e);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
