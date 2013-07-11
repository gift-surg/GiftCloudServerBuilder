/*
 * org.nrg.xnat.helpers.prearchive.SessionXMLRebuilderJob
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.helpers.prearchive;

import org.apache.commons.lang.StringUtils;
import org.nrg.schedule.JobInterface;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.exception.InvalidPermissionException;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Provider;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;

public class SessionXMLRebuilderJob implements JobInterface {
	@Override
	public void init(JobExecutionContext context) throws JobExecutionException {
		_map = context.getMergedJobDataMap();
	}

	@Override
	public void execute() throws JobExecutionException {
		final Provider<XDATUser> provider = (Provider<XDATUser>)_map.get("user");
		XDATUser user = provider.get();
        logger.trace("Running prearc job as {}", user.getLogin());
		List<SessionData> sds = null;
		long now = Calendar.getInstance().getTimeInMillis();
		try {
			if (PrearcDatabase.ready) { 
				sds = PrearcDatabase.getAllSessions();
			}
		} catch (SessionException e) {
			logger.error("", e);
		} catch (SQLException e) {
            // Swallow this message so it doesn't fill the logs before the prearchive is initialized.
            if (!e.getMessage().contains("relation \"xdat_search.prearchive\" does not exist")) {
                logger.error("", e);
            }
		} catch (Exception e) {
			logger.error("", e);
		}
		int updated = 0;
		int total = 0;
        if (sds != null && sds.size() > 0) {
            for (final SessionData sessionData : sds) {
                total++;
                if (sessionData.getStatus().equals(PrearcUtils.PrearcStatus.RECEIVING) && !sessionData.getPreventAutoCommit() && !StringUtils.trimToEmpty(sessionData.getSource()).equals("applet")) {
                    File sessionDir = null;
                    try {
                        sessionDir = PrearcUtils.getPrearcSessionDir(user, sessionData.getProject(), sessionData.getTimestamp(), sessionData.getFolderName(), false);
                    } catch (IOException e) {
                        logger.error("", e);
                    } catch (InvalidPermissionException e) {
                        logger.error("", e);
                    } catch (Exception e) {
                        logger.error("", e);
                    }
                    long then = sessionData.getLastBuiltDate().getTime();
                    double interval = (double) _map.getIntValue("interval");
                    double diff = diffInMinutes(then, now);
                    if (diff >= interval) {
                            updated++;
                        try {
                            if (PrearcDatabase.setStatus(sessionData.getFolderName(), sessionData.getTimestamp(), sessionData.getProject(), PrearcUtils.PrearcStatus.QUEUED_BUILDING)) {
                                logger.debug("Creating JMS queue entry for {} to archive {}", user.getUsername(), sessionData.getExternalUrl());
                                SessionXmlRebuilderRequest request = new SessionXmlRebuilderRequest(user, sessionData, sessionDir);
                                XDAT.sendJmsRequest(request);
                            }
                        } catch (Exception exception) {
                            logger.error("Error when setting prearchive session status to QUEUED", exception);
                        }
                    }
                }
            }
        }
        logger.info("Built {} of {}", updated, total);
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
	}

	public static double diffInMinutes(long start, long end) {
		double seconds = Math.floor((end - start) / 1000);
		return Math.floor(seconds / 60);
	}

	private Logger logger = LoggerFactory.getLogger(SessionXMLRebuilderJob.class);
	private JobDataMap _map;
}
