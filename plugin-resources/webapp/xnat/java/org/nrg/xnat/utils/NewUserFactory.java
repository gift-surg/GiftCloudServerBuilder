/*
 * org.nrg.xnat.utils.NewUserFactory
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
import org.nrg.xdat.security.XDATUser.UserNotFoundException;
import org.nrg.xft.exception.DBPoolException;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

@Deprecated
public class NewUserFactory implements UserFactory {

    /* (non-Javadoc)
     * @see org.nrg.xnat.utils.UserFactoryI#getUser(java.lang.String)
     */
    @Override
    public XDATUser getUser(String login) throws SQLException,UserNotFoundException {
        try {
            return new XDATUser(login);
        } catch (UserNotFoundException e) {
            throw e;
        } catch (SQLException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (DBPoolException e) {
            throw new SQLException(e);
        } catch (Exception e) {
            LoggerFactory.getLogger(NewUserFactory.class).warn("wrapping as RuntimeException", e);
            throw new RuntimeException(e);
        }
    }
}
