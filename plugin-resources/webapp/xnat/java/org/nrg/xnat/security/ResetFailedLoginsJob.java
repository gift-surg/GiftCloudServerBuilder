/*
 * org.nrg.xnat.security.ResetFailedLoginsJob
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.security;

import org.nrg.schedule.JobInterface;
	import org.nrg.xdat.XDAT;
import org.quartz.JobDataMap;
	import org.quartz.JobExecutionContext;
	import org.quartz.JobExecutionException;
	import org.slf4j.Logger;
	import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

	public class ResetFailedLoginsJob implements JobInterface {
		private JobDataMap _map;

		@Override
		public void init(JobExecutionContext context) throws JobExecutionException {
			_map = context.getMergedJobDataMap();
		}

		@Override
		public void execute() throws JobExecutionException {
			JdbcTemplate template = new JdbcTemplate(XDAT.getDataSource());
			template.execute("UPDATE xhbm_xdat_user_auth SET failed_login_attempts=0");
			//reset the failed_login_attempts for all accounts to 0
		}

		@Override
		public void destroy() {
			// TODO Auto-generated method stub
		}

		private Logger logger = LoggerFactory.getLogger(ResetFailedLoginsJob.class);
	}

