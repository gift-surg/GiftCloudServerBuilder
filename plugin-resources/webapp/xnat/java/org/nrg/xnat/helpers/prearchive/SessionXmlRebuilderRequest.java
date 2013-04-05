/*
 * SessionXmlRebuilderRequest
 * Copyright (c) 2013. Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD License
 */

package org.nrg.xnat.helpers.prearchive;

import org.nrg.xdat.security.XDATUser;

import java.io.File;
import java.io.Serializable;

/**
 * SessionXmlRebuilderRequest
 *
 * @author rherri01
 * @since 4/3/13
 */
public class SessionXmlRebuilderRequest implements Serializable {
    public SessionXmlRebuilderRequest(XDATUser user, SessionData sessionData, File sessionDir) {
        _user = user;
        _sessionData = sessionData;
        _sessionDir = sessionDir;
    }

    public XDATUser getUser() {
        return _user;
    }

    public SessionData getSessionData() {
        return _sessionData;
    }

    public File getSessionDir() {
        return _sessionDir;
    }

    private static final long serialVersionUID = -6953780271999788326L;

    private final XDATUser _user;
    private final SessionData _sessionData;
    private final File _sessionDir;
}
