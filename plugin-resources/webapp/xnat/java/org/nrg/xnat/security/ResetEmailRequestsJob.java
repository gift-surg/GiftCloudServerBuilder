package org.nrg.xnat.security;

import org.nrg.schedule.JobInterface;
import org.nrg.xdat.XDAT;
import org.nrg.mail.services.EmailRequestLogService;
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