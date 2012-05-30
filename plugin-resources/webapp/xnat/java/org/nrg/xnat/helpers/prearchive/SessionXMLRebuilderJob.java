package org.nrg.xnat.helpers.prearchive;

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
                    double interval = (double) _map.getIntValue("interval");
                    double diff = diffInMinutes(then, now);
                    if (diff >= interval) {
                        logger.info("committing {}", sessionData.getExternalUrl());
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
