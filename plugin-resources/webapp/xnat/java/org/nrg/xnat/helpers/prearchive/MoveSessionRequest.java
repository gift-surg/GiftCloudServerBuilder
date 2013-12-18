package org.nrg.xnat.helpers.prearchive;/*
 * org.nrg.xnat.helpers.prearchive.PrearcDatabase
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Created 12/16/13 10:21 AM
 */

import org.nrg.xdat.security.XDATUser;

import java.io.File;
import java.io.Serializable;

public class MoveSessionRequest implements Serializable {
    public MoveSessionRequest(XDATUser user, SessionData sessionData, File sessionDir, String newProject) {
        _user = user;
        _sessionData = sessionData;
        _sessionDir = sessionDir;
        _newProject = newProject;
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

    public String getNewProject() { return _newProject; }

    private static final long serialVersionUID = -6953780271999788326L;

    private final XDATUser _user;
    private final SessionData _sessionData;
    private final File _sessionDir;
    private final String _newProject;
}
