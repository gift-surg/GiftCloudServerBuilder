package org.nrg.xnat.helpers.prearchive;

import org.apache.log4j.Logger;
import org.nrg.dcm.DicomSCP;
import org.nrg.schedule.JobInterface;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.security.XDATUser.UserNotFoundException;
import org.nrg.xft.exception.*;
import org.nrg.xnat.archive.FinishImageUpload;
import org.nrg.xnat.helpers.prearchive.PrearcDatabase.SyncFailedException;
import org.nrg.xnat.restlet.actions.PrearcImporterA.PrearcSession;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

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
		logger.debug("Running prearc job");
		XDATUser user = null;
		try {
			user = DicomSCP.getUser();
		} catch (UserNotFoundException e1) {
			logger.error("", e1);
		} catch (XFTInitException e1) {
			logger.error("", e1);
		} catch (ElementNotFoundException e1) {
			logger.error("", e1);
		} catch (DBPoolException e1) {
			logger.error("", e1);
		} catch (SQLException e1) {
			logger.error("", e1);
		} catch (FieldNotFoundException e1) {
			logger.error("", e1);
		} catch (Exception e1) {
			logger.error("", e1);
		}
		List<SessionData> dataList = null;
		long now = Calendar.getInstance().getTimeInMillis();
		try {
			dataList = PrearcDatabase.getAllSessions();
		} catch (SessionException e) {
			logger.error("", e);
		} catch (SQLException e) {
			logger.error("", e);
		} catch (Exception e) {
			logger.error("", e);
		}
		int updated = 0;
		int total = 0;
        for (SessionData sessionData : dataList) {
			total++;
            if (sessionData.getStatus().equals(PrearcUtils.PrearcStatus.RECEIVING) && !sessionData.getPreventAutoCommit()) {
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
                final double interval = (double) _map.getIntValue("interval");
                final double elapsed = diffInMinutes(then, now);
                logger.debug("Found configured interval of " + interval + " minutes, " + elapsed + " minutes have elapsed.");
                if (elapsed >= interval) {
                    logger.info("committing " + sessionData.getExternalUrl());
					try {
						updated++;
                        if (PrearcDatabase.setStatus(sessionData.getFolderName(), sessionData.getTimestamp(), sessionData.getProject(), PrearcUtils.PrearcStatus.BUILDING)) {
                            PrearcDatabase.buildSession(sessionDir, sessionData.getFolderName(), sessionData.getTimestamp(), sessionData.getProject());
                            PrearcUtils.resetStatus(user, sessionData.getProject(), sessionData.getTimestamp(), sessionData.getFolderName(), true);

                            final FinishImageUpload uploader = new FinishImageUpload(null, user, new PrearcSession(sessionData.getProject(), sessionData.getTimestamp(), sessionData.getFolderName(), null, user), null, false, true, false);
							uploader.call();
						}
					} catch (SyncFailedException e) {
						logger.error("", e);
					} catch (SQLException e) {
						logger.error("", e);
					} catch (SessionException e) {
						logger.error("", e);
					} catch (IOException e) {
						logger.error("", e);
					} catch (InvalidPermissionException e) {
						logger.error("", e);
					} catch (Exception e) {
						logger.error("", e);
					}
				}
			}
		}
		logger.info(String.format("Built %d of %d", updated, total));
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
	}

	public static double diffInMinutes(long start, long end) {
		double seconds = Math.floor((end - start) / 1000);
		return Math.floor(seconds / 60);
	}

	private static Logger logger = Logger.getLogger(SessionXMLRebuilderJob.class);
	private JobDataMap _map;
}
