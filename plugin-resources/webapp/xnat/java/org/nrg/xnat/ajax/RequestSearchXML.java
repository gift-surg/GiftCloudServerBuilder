/*
 * org.nrg.xnat.ajax.RequestSearchXML
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.ajax;

import org.apache.log4j.Logger;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.security.XdatStoredSearch;
import org.nrg.xft.schema.Wrappers.XMLWrapper.SAXWriter;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class RequestSearchXML {

	static org.apache.log4j.Logger logger = Logger
			.getLogger(RequestSearchXML.class);

	public void execute(HttpServletRequest req, HttpServletResponse response,
			ServletConfig sc) throws IOException {
		String bundle = req.getParameter("bundleID");

		response.setContentType("text/xml");
		response.setHeader("Cache-Control", "no-cache");

		HttpSession session = req.getSession();
		XDATUser user = XDAT.getUserDetails();

		if (user != null) {
			XdatStoredSearch xss = XdatStoredSearch.GetPreLoadedSearch(bundle,
					true);
			if (xss != null) {
				try {
					SAXWriter writer = new SAXWriter(
							response.getOutputStream(), false);
					writer.setAllowSchemaLocation(false);
					writer.write(xss.getItem(), false);
				} catch (Throwable e) {
					logger.error("", e);
				}
			}
		}

		// System.out.print("Monitor Progress " + uploadID + "... ");
	}
}
