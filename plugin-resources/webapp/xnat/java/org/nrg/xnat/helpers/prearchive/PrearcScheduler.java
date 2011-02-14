package org.nrg.xnat.helpers.prearchive;

import java.io.File;
import java.io.IOException;
import java.io.SyncFailedException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.nrg.schedule.JobBuilderA;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.security.XDATUser.UserNotFoundException;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.InvalidPermissionException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xnat.utils.UserUtils;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleTrigger;

public class PrearcScheduler extends JobBuilderA {
	static Logger logger = Logger.getLogger(PrearcScheduler.class);
	
	public final static int MIN_SINCE_MOD=5, SCHED_INTERVAL=1;
	
	public PrearcScheduler () {
		logger.error("Creating prearcScheduler");
	}
	
	public static double diffInMinutes (long start, long end) {
		double seconds = Math.floor((end - start) / 1000);
		return Math.floor(seconds / 60);
	}
	
	public int getMinutes(){
		return MIN_SINCE_MOD;
	}
	
	public int getInterval(){
		return SCHED_INTERVAL;
	}
	
	@Override
	protected List<XJob> createJobs() {
		List<XJob> js = new ArrayList<XJob>();
        JobDetail jobDetail = new JobDetail("session-rebuilder", "prearchive-jobs", SessionXMLRebuilder.class);
        SimpleTrigger simpleTrigger = new SimpleTrigger("session-rebuilder-trigger", "prearchive-triggers");
        simpleTrigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
        // run every minute
        simpleTrigger.setRepeatInterval(1000 * 60 * getInterval());
        JobDataMap map = new JobDataMap();
        map.put("interval", getMinutes());
        jobDetail.setJobDataMap(map);
        js.add(new XJob(jobDetail, simpleTrigger));
		return js;
	}
	
	public static class SessionXMLRebuilder implements Job {
		static Logger logger = Logger.getLogger(SessionXMLRebuilder.class);
		@Override
		public void execute(JobExecutionContext arg0)
				throws JobExecutionException {
			logger.error("Running prearc job");
			JobDataMap map = arg0.getMergedJobDataMap();
			XDATUser user = null;
			try {
				user = UserUtils.getDICOMStoreUser();
			} catch (UserNotFoundException e1) {
				logger.error(e1.getMessage());
			} catch (XFTInitException e1) {
				logger.error(e1.getMessage());
			} catch (ElementNotFoundException e1) {
				logger.error(e1.getMessage());
			} catch (DBPoolException e1) {
				logger.error(e1.getMessage());
			} catch (SQLException e1) {
				logger.error(e1.getMessage());
			} catch (FieldNotFoundException e1) {
				logger.error(e1.getMessage());
			} catch (Exception e1) {
				logger.error(e1.getMessage());
			}
			List<SessionData> sds = null;
			long now = Calendar.getInstance().getTimeInMillis();
			try {
				sds = PrearcDatabase.getAllSessions();
			} catch (SessionException e) {
				logger.error(e.getMessage());
			} catch (SQLException e) {
				logger.error(e.getMessage());
			}
			int updated=0;
			int total=0;
			Iterator<SessionData> i = sds.iterator();
			while (i.hasNext()) {
				total++;
				SessionData s = i.next();
				if (s.getStatus().equals(PrearcUtils.PrearcStatus.RECEIVING)) {
					File sessionDir = null;
					try {
						sessionDir = PrearcUtils.getPrearcSessionDir(user, s.getProject(), s.getTimestamp(), s.getFolderName(),false);
					} catch (IOException e) {
						logger.error(e.getMessage());
					} catch (InvalidPermissionException e) {
						logger.error(e.getMessage());
					} catch (Exception e) {
						logger.error(e.getMessage());
					}
					long then = s.getLastBuiltDate().getTime();					
					double interval = (double) map.getIntValue("interval");
					double diff = PrearcScheduler.diffInMinutes(then, now);
					if (diff >= interval) {
						logger.error("rebuilding");
						try {
							updated++;
							if (PrearcDatabase.setStatus(s.getFolderName(), s.getTimestamp(), s.getProject(), PrearcUtils.PrearcStatus.BUILDING)) {
							    PrearcDatabase.buildSession(sessionDir, s.getFolderName(), s.getTimestamp(), s.getProject());
							    PrearcUtils.resetStatus(user, s.getProject(), s.getTimestamp(), s.getFolderName(),true);
							}
						} catch (SyncFailedException e) {
							logger.error(e.getMessage());
						} catch (SQLException e) {
							logger.error(e.getMessage());
						} catch (SessionException e) {
							logger.error(e.getMessage());
						} catch (IOException e) {
							logger.error(e.getMessage());
						} catch (InvalidPermissionException e) {
							logger.error(e.getMessage());
						} catch (Exception e) {
							logger.error(e.getMessage());
						} 
					}
				}
			}
			logger.info(String.format("Built %d of %d",updated,total));
		}
	}
}
