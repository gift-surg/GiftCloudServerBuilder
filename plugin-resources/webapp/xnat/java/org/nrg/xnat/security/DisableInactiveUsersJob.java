package org.nrg.xnat.security;

	import javax.inject.Provider;

import org.nrg.schedule.JobInterface;
	import org.nrg.xdat.XDAT;
import org.nrg.xdat.security.XDATUser;
import org.quartz.JobDataMap;
	import org.quartz.JobExecutionContext;
	import org.quartz.JobExecutionException;
	import org.slf4j.Logger;
	import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

	public class DisableInactiveUsersJob implements JobInterface {
		private int inactivityBeforeLockout;
		private JobDataMap _map;

		@Override
		public void init(JobExecutionContext context) throws JobExecutionException {
			_map = context.getMergedJobDataMap();
		}

		@Override
		public void execute() throws JobExecutionException {
			final int secondsBeforeLockout = Integer.parseInt((String) _map.get("inactivityBeforeLockout"));
			JdbcTemplate template = new JdbcTemplate(XDAT.getDataSource());
			
			template.execute("UPDATE xdat_user SET enabled=0 WHERE login IN "+
			"("+
			"SELECT xdat_user.login FROM xdat_user INNER JOIN "+
				"("+
				"SELECT y.login, last_login, activation_date FROM xdat_user_meta_data INNER JOIN "+
					"("+
					"SELECT xdat_user.login, xdat_user.xdat_user_id, MAX(xdat_user_login.login_date) AS last_login FROM xdat_user_login RIGHT JOIN xdat_user ON xdat_user_login.user_xdat_user_id=xdat_user.xdat_user_id GROUP BY xdat_user.login,xdat_user.xdat_user_id"+
					") y "+ //get last login times for each user
				"ON y.xdat_user_id=xdat_user_meta_data.meta_data_id AND y.xdat_user_id NOT IN (SELECT xdat_user_xdat_user_id FROM xdat_r_xdat_role_type_assign_xdat_user WHERE xdat_r_xdat_role_type_assign_xdat_user.xdat_role_type_role_name = 'Administrator')"+
				") x "+ //get dates that each non-admin user was created
			"ON x.login=xdat_user.login AND ((x.activation_date<(now()- INTERVAL '"+secondsBeforeLockout+" seconds')) AND ((x.last_login IS NULL) OR x.last_login<(now()- INTERVAL '"+secondsBeforeLockout+" seconds'))) AND xdat_user.enabled=1"+
			")");//disable the user if user was not created and did not log in within the last 'secondsBeforeLockout' seconds and is currently enabled
		}

		@Override
		public void destroy() {
			// TODO Auto-generated method stub
		}

		private Logger logger = LoggerFactory.getLogger(DisableInactiveUsersJob.class);
	}

