/**
 * Copyright (c) 2011 Washington University
 */
package org.nrg.xnat.utils;

import java.sql.SQLException;

import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.security.XDATUser.UserNotFoundException;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public interface UserFactory {
    XDATUser getUser(String login) throws SQLException,UserNotFoundException;
}
