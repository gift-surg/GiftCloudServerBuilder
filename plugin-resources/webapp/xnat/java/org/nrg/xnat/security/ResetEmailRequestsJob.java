/*
 * org.nrg.xnat.security.ResetEmailRequestsJob
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.security;

import org.nrg.mail.services.EmailRequestLogService;
import org.nrg.schedule.JobInterface;
import org.nrg.xdat.XDAT;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

	public class ResetEmailRequestsJob implements JobInterface {

		private final EmailRequestLogService requests = XDAT.getContextService().getBean(EmailRequestLogService.class);
		
		@Override
		public void init(JobExecutionContext jobExecutionContext)
				throws JobExecutionException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void execute() throws JobExecutionException {
			if(requests != null){
				requests.clearLogs();
			}
		}

		@Override
		public void destroy() {
			// TODO Auto-generated method stub
		}
	}