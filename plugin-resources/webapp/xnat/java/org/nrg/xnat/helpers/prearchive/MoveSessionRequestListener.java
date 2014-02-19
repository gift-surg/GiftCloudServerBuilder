/*
 * org.nrg.xnat.helpers.prearchive.MoveSessionRequestListener
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 12/19/13 3:01 PM
 */
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
import org.nrg.xft.exception.InvalidPermissionException;
import org.nrg.xnat.archive.FinishImageUpload;
import org.nrg.xnat.restlet.actions.PrearcImporterA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class MoveSessionRequestListener {

    public void onMoveSessionRequest(final MoveSessionRequest moveSessionRequest) throws Exception {
        try {
            SessionData sessionData = moveSessionRequest.getSessionData();
            String newProject = moveSessionRequest.getNewProject();
            File sessionDir = moveSessionRequest.getSessionDir();
            log.info("Received request to process prearchive session at: {}", sessionData.getExternalUrl());
            try {
                if (!sessionDir.getParentFile().exists()) {
                    PrearcDatabase.unsafeSetStatus(sessionData.getFolderName(), sessionData.getTimestamp(), sessionData.getProject(), PrearcUtils.PrearcStatus._DELETING);
                    PrearcDatabase.deleteCacheRow(sessionData.getFolderName(), sessionData.getTimestamp(), sessionData.getProject());
                }
                PrearcDatabase.moveToProject(sessionData.getFolderName(), sessionData.getTimestamp(), sessionData.getProject(), newProject);
            } catch (PrearcDatabase.SyncFailedException e) {
                log.error("", e);
            } catch (SQLException e) {
                log.error("", e);
            } catch (SessionException e) {
                log.error("", e);
            } catch (Exception e) {
                log.error("", e);
            }
            log.info("Listener completed session move request.");
        } catch (final Exception exception) {
            // If errors are not logged before they're rethrown, they do not show up in any of the files
            log.error("Choked on request " + moveSessionRequest + " with the indicated error", exception);
            throw exception;
        }
    }

    private final static Logger log = LoggerFactory.getLogger(MoveSessionRequestListener.class);
}
