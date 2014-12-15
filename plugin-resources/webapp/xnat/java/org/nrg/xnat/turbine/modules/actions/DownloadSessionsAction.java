/*
 * org.nrg.xnat.turbine.modules.actions.DownloadSessionsAction
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.turbine.modules.actions;

import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.search.DisplaySearch;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.ListingAction;
import org.nrg.xdat.turbine.utils.TurbineUtils;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

public class DownloadSessionsAction extends ListingAction {
	static Logger logger = Logger.getLogger(DownloadSessionsAction.class);

	@Override
	public String getDestinationScreenName(RunData data) {
		return "XDATScreen_download_sessions.vm";
	}

	public void finalProcessing(RunData data, Context context) throws Exception {
		DisplaySearch search = TurbineUtils.getSearch(data);
		search.setPagingOn(false);
		// Load search results into a table
		org.nrg.xft.XFTTable table = (org.nrg.xft.XFTTable) search.execute(
				null, TurbineUtils.getUser(data).getLogin());
		search.setPagingOn(true);

		XDATUser user = TurbineUtils.getUser(data);
		if (user == null) {
			throw new Exception("Invalid User.");
		}

		// exceptable display field ids for the session ID
		List<String> sessionIDHeaders = Arrays.asList("session_id", "expt_id",
				"id");

		String sessionIDHeader = null;
		for (String key : sessionIDHeaders) {
			if (table.getColumnIndex(key) != null) {
				sessionIDHeader = key;
				break;
			}
		}

		if (sessionIDHeader == null) {
			logger.error("Missing expected display field for "
					+ search.getRootElement().getFullXMLName()
					+ " download feature (SESSION_ID, EXPT_ID, or ID)");
			throw new Exception("Missing expected ID display field.");
		}

		table.resetRowCursor();
		while (table.hasMoreRows()) {
			Hashtable row = table.nextRowHash();
			data.getParameters().append("sessions",
					(String) row.get(sessionIDHeader));
		}
	}
}
