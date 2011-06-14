package org.nrg.schedule;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Defines the component interface for a task to be executed under the Spring Quartz framework using the
 * {@link DelegatingJobBean}.
 *
 * This class is based on code by Hari Gangadharan at:
 * 
 * http://www.harinair.com/2008/01/spring-quartz-and-auto-wiring-of-quartz-jobs.
 * 
 * @author rherrick
 */
public interface JobInterface {
	/**
	 * Initializes the job bean. All parameters must be passed in here on bean creation.
	 * @param jobExecutionContext The execution context.
	 * @throws JobExecutionException When something goes wrong with job execution.
	 */
	public void init(JobExecutionContext jobExecutionContext) throws JobExecutionException;
	/**
	 * Executes the job bean.
	 * @throws JobExecutionException When something goes wrong with job execution.
	 */
	public void execute() throws JobExecutionException;
	/**
	 * Destroys the job bean. This can be used for task clean-up, etc.
	 */
	public void destroy();
}