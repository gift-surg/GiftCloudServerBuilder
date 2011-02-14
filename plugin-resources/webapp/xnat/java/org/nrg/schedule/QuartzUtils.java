package org.nrg.schedule;

import org.nrg.schedule.JobBuilderA.XJob;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tim Olsen
 *
 * This Quartz integration is not a long term solution.  We should be using Spring to configure this.  But, we don't have time to implement that for XNAT 1.5 and need a scheduler.
 */
public class QuartzUtils {
	private final static Logger logger = LoggerFactory.getLogger(QuartzUtils.class);
	
	private static Scheduler scheduler=null;
	public static void init(){
		if(scheduler==null){
			try {
				scheduler = StdSchedulerFactory.getDefaultScheduler();
				
				addJobs(scheduler);
				
				scheduler.start();
			} catch (SchedulerException e) {
				logger.error("",e);
			}
		}
	}
	
	public static Scheduler getInstance(){
		return scheduler;
	}
	
	public static void addJobs(Scheduler scheduler){
		for(XJob job: JobBuilderA.getJobs()){
			try {
				if(job.jobDetail==null){
					scheduler.scheduleJob(job.trigger);
				}else{
					scheduler.scheduleJob(job.jobDetail,job.trigger);
				}
			} catch (SchedulerException e) {
				logger.error("",e);
			}
		}
	}
	
}
