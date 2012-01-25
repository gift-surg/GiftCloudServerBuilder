package org.nrg.xnat.helpers.prearchive;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import javax.inject.Provider;

import org.nrg.schedule.JobInterface;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.exception.InvalidPermissionException;
import org.nrg.xnat.archive.FinishImageUpload;
import org.nrg.xnat.helpers.prearchive.PrearcDatabase.SyncFailedException;
import org.nrg.xnat.restlet.actions.PrearcImporterA.PrearcSession;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
			sds = PrearcDatabase.getAllSessions();
		} catch (SessionException e) {
			logger.error("", e);
		} catch (SQLException e) {
			logger.error("", e);
		} catch (Exception e) {
			logger.error("", e);
		}
		int updated = 0;
		int total = 0;
		Iterator<SessionData> i = sds.iterator();
		while (i.hasNext()) {
			total++;
			SessionData s = i.next();
			if (s.getStatus().equals(PrearcUtils.PrearcStatus.RECEIVING)) {
				File sessionDir = null;
				try {
					sessionDir = PrearcUtils.getPrearcSessionDir(user, s.getProject(), s.getTimestamp(), s.getFolderName(), false);
				} catch (IOException e) {
					logger.error("", e);
				} catch (InvalidPermissionException e) {
					logger.error("", e);
				} catch (Exception e) {
					logger.error("", e);
				}
				long then = s.getLastBuiltDate().getTime();
				double interval = (double) _map.getIntValue("interval");
				double diff = diffInMinutes(then, now);
				if (diff >= interval) {
					logger.info("commiting {}", s.getExternalUrl());
					try {
						updated++;
						if (PrearcDatabase.setStatus(s.getFolderName(), s.getTimestamp(), s.getProject(), PrearcUtils.PrearcStatus.BUILDING)) {
							PrearcDatabase.buildSession(sessionDir, s.getFolderName(), s.getTimestamp(), s.getProject());
							PrearcUtils.resetStatus(user, s.getProject(), s.getTimestamp(), s.getFolderName(), true);

							final FinishImageUpload uploader = new FinishImageUpload(null, user, new PrearcSession(s.getProject(), s.getTimestamp(), s.getFolderName(), null, user), null, false, true, false);
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
