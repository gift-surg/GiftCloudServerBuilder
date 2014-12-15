/*
 * org.nrg.xnat.helpers.prearchive.SessionXmlRebuilderRequestListener
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 10/14/13 5:42 PM
 */

package org.nrg.xnat.helpers.prearchive;

import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.exception.InvalidPermissionException;
import org.nrg.xnat.archive.FinishImageUpload;
import org.nrg.xnat.restlet.actions.PrearcImporterA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class SessionXmlRebuilderRequestListener {

	@SuppressWarnings("unused")
	public void onSessionXmlRebuilderRequest(
			final SessionXmlRebuilderRequest sessionXmlRebuilderRequest)
			throws Exception {
		try {
			XDATUser user = sessionXmlRebuilderRequest.getUser();
			SessionData sessionData = sessionXmlRebuilderRequest
					.getSessionData();
			boolean receiving = sessionData.getStatus() != null
					&& sessionData.getStatus().equals(
							PrearcUtils.PrearcStatus.RECEIVING);
			File sessionDir = sessionXmlRebuilderRequest.getSessionDir();
			log.info("Received request to process prearchive session at: {}",
					sessionData.getExternalUrl());
			try {
				if (!sessionDir.getParentFile().exists()) {
					PrearcDatabase.unsafeSetStatus(sessionData.getFolderName(),
							sessionData.getTimestamp(),
							sessionData.getProject(),
							PrearcUtils.PrearcStatus._DELETING);
					PrearcDatabase.deleteCacheRow(sessionData.getFolderName(),
							sessionData.getTimestamp(),
							sessionData.getProject());
				} else if (PrearcDatabase.setStatus(
						sessionData.getFolderName(),
						sessionData.getTimestamp(), sessionData.getProject(),
						PrearcUtils.PrearcStatus.BUILDING)) {
					PrearcDatabase.buildSession(sessionDir,
							sessionData.getFolderName(),
							sessionData.getTimestamp(),
							sessionData.getProject(), sessionData.getVisit(),
							sessionData.getProtocol(),
							sessionData.getTimeZone(), sessionData.getSource());
					PrearcUtils.resetStatus(user, sessionData.getProject(),
							sessionData.getTimestamp(),
							sessionData.getFolderName(), true);

					// we don't want to autoarchive a session that are just
					// being rebuilt
					// but we still want to autoarchive sessions that just came
					// from RECEIVING STATE
					final FinishImageUpload uploader = new FinishImageUpload(
							null, user, new PrearcImporterA.PrearcSession(
									sessionData.getProject(),
									sessionData.getTimestamp(),
									sessionData.getFolderName(), null, user),
							null, false, true, false);
					if (receiving || !uploader.isAutoArchive()) {
						log.debug(
								"Processing queue entry for {} in project {} to archive {}",
								user.getUsername(), sessionData.getProject(),
								sessionData.getExternalUrl());
						uploader.call();
					}
				}
			} catch (PrearcDatabase.SyncFailedException e) {
				log.error("", e);
			} catch (SQLException e) {
				log.error("", e);
			} catch (SessionException e) {
				log.error("", e);
			} catch (IOException e) {
				log.error("", e);
			} catch (InvalidPermissionException e) {
				log.error("", e);
			} catch (Exception e) {
				log.error("", e);
			}
			log.info("Listener completed session XML rebuild request.");
		} catch (final Exception exception) {
			// If errors are not logged before they're rethrown, they do not
			// show up in any of the files
			log.error("Choked on request " + sessionXmlRebuilderRequest
					+ " with the indicated error", exception);
			throw exception;
		}
	}

	private final static Logger log = LoggerFactory
			.getLogger(SessionXmlRebuilderRequestListener.class);
}
