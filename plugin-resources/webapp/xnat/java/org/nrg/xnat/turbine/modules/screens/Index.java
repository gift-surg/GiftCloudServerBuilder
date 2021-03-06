/*
 * org.nrg.xnat.turbine.modules.screens.Index
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.db.PoolDBUtils;
import org.nrg.xnat.helpers.prearchive.PrearcDatabase;
import org.nrg.xnat.turbine.utils.ProjectAccessRequest;

import java.util.Date;

public class Index extends SecureScreen {

	@Override
	protected void doBuildTemplate(RunData data, Context context)
			throws Exception {

		XDATUser user = TurbineUtils.getUser(data);

		if (((String) org.nrg.xdat.turbine.utils.TurbineUtils
				.GetPassedParameter("node", data)) != null) {
			context.put("node",
					((String) org.nrg.xdat.turbine.utils.TurbineUtils
							.GetPassedParameter("node", data)));
		}

		ProjectAccessRequest.CreatePARTable();

		if (user.getEmail() == null) {
			data.setMessage("WARNING: A valid email account is required for many features.  Please use the (edit) link at the top of the page to add a valid email address to your user account.");
		} else {
			Integer parcount = (Integer) PoolDBUtils
					.ReturnStatisticQuery(
							"SELECT COUNT(par_id)::int4 AS count FROM xs_par_table WHERE approval_date IS NULL AND LOWER(email)='"
									+ user.getEmail().toLowerCase() + "'",
							"count", user.getDBName(), user.getLogin());
			context.put("par_count", parcount);
		}

		Date lastLogin = user.getPreviousLogin();
		if (lastLogin != null) {
			context.put("last_login", lastLogin);
		}

		context.put("proj_count", user.getTotalCounts().get("xnat:projectData"));

		context.put("sub_count", user.getTotalCounts().get("xnat:subjectData"));

		context.put("user", user);

		Long isd_count = (Long) PoolDBUtils.ReturnStatisticQuery(
				"SELECT COUNT(*) FROM xnat_imageSessionData", "count",
				TurbineUtils.getUser(data).getDBName(),
				TurbineUtils.getUser(data).getUsername());
		context.put("isd_count", isd_count);

		// count prearc entries
		try {
			context.put("prearc_count", PrearcDatabase.buildRows(user, null)
					.size());
		} catch (Throwable e) {
			logger.error("", e);
		}
	}
}
