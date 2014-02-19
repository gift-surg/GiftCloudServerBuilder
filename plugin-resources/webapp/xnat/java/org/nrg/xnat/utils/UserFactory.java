/*
 * org.nrg.xnat.utils.UserFactory
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

import java.sql.SQLException;

public interface UserFactory {
    XDATUser getUser(String login) throws SQLException,UserNotFoundException;
}
