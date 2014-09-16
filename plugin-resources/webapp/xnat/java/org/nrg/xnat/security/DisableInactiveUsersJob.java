/*
 * org.nrg.xnat.security.DisableInactiveUsersJob
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 11/4/13 9:51 AM
 */
package org.nrg.xnat.security;

import org.apache.commons.lang.time.DateUtils;
import org.nrg.schedule.JobInterface;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFTTable;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.utils.AuthUtils;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.GregorianCalendar;

	public class DisableInactiveUsersJob implements JobInterface {
		private JobDataMap _map;

		@Override
		public void init(JobExecutionContext context) throws JobExecutionException {
			_map = context.getMergedJobDataMap();
		}

		@Override
		public void execute() throws JobExecutionException {
			final int secondsBeforeLockout = Integer.parseInt((String) _map.get("inactivityBeforeLockout"));
			if(secondsBeforeLockout!=-1){
				//modified to allow auditing of these modifications.
				try {
					XFTTable t=XFTTable.Execute("SELECT xdat_user.login FROM xdat_user INNER JOIN "+
								"("+
								"SELECT y.login, last_login, activation_date FROM xdat_user_meta_data INNER JOIN "+
									"("+
									"SELECT xdat_user.login, xdat_user.xdat_user_id, MAX(xdat_user_login.login_date) AS last_login FROM xdat_user_login RIGHT JOIN xdat_user ON xdat_user_login.user_xdat_user_id=xdat_user.xdat_user_id GROUP BY xdat_user.login,xdat_user.xdat_user_id"+
									") y "+ //get last login times for each user
									"ON y.xdat_user_id=xdat_user_meta_data.meta_data_id AND y.login NOT IN (SELECT username FROM xhbm_user_role WHERE role='Administrator') AND y.xdat_user_id NOT IN (SELECT xdat_user_xdat_user_id FROM xdat_r_xdat_role_type_assign_xdat_user WHERE xdat_r_xdat_role_type_assign_xdat_user.xdat_role_type_role_name = 'Administrator')"+
									") x "+ //get dates that each non-admin user was created
							"ON x.login=xdat_user.login AND ((x.activation_date<(now()- INTERVAL '"+secondsBeforeLockout+" seconds')) AND ((x.last_login IS NULL) OR x.last_login<(now()- INTERVAL '"+secondsBeforeLockout+" seconds'))) AND xdat_user.enabled=1", null, null);
					
					t.resetRowCursor();
					while(t.hasMoreRows()){
						Object[] row=t.nextRow();
						
						try {
							XDATUser u=new XDATUser((String)row[0]);
							// Fixes XNAT-2407. Only disable user if they have not been recently modified (enabled). 
							if(!hasUserBeenModified(u, secondsBeforeLockout)){
								u.setEnabled("0");
                                u.setVerified("0");
								XDATUser.ModifyUser(u, u, EventUtils.newEventInstance(EventUtils.CATEGORY.SIDE_ADMIN, EventUtils.TYPE.PROCESS, "Disabled due to inactivity"));
							
								String expiration=TurbineUtils.getDateTimeFormatter().format(DateUtils.addMilliseconds(GregorianCalendar.getInstance().getTime(), -(AuthUtils.LOCKOUT_DURATION)));
								System.out.println("Locked out " + u.getLogin() + " user account until "+expiration);
								AdminUtils.sendAdminEmail(u.getLogin() +" account disabled due to inactivity.", "User "+ u.getLogin() +" has been automatically disabled due to inactivity.");
							}
						} catch (Exception e) {
							logger.error("",e);
						}
					}
				} catch (SQLException e) {
					logger.error("",e);
				} catch (DBPoolException e) {
					logger.error("",e);
				}
			}
		}
	
		/**
		 * Function determines if the user has been modified in the past amount of seconds.
		 * Fixes XNAT-2407. This keeps the job from disabling a user if the admin has just enabled (modified) them. 
		 * @param u - the user we are interested in. 
		 * @param seconds - Has the user been modified in the past amount of seconds. 
		 * @return true if the user has been modified / otherwise false.
		 */
		private boolean hasUserBeenModified(XDATUser u, int seconds){
			
			// Subtract seconds from today's date.
			final Calendar c = Calendar.getInstance();
			c.add(Calendar.SECOND, -seconds);
			
			// If the time is before the last modified date, the user has been modified.
			return (c.getTime().before(u.getItem().getLastModified()));
		}

		@Override
		public void destroy() {
			// TODO Auto-generated method stub
		}

		private Logger logger = LoggerFactory.getLogger(DisableInactiveUsersJob.class);
	}

