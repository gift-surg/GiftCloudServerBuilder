/*
 * org.nrg.xnat.servlet.RESTServlet
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.servlet;

import org.apache.log4j.Logger;
import org.nrg.xdat.security.XDATUser;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RESTServlet extends HttpServlet {
	static org.apache.log4j.Logger logger = Logger.getLogger(RESTServlet.class);
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		XDATUser user = (XDATUser)request.getSession().getAttribute("user");
		if (user==null){
			response.sendError(401);
			return;
		}
		response.sendError(400);
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		XDATUser user = (XDATUser)request.getSession().getAttribute("user");
		if (user==null){
			response.sendError(401);
			return;
		}
		response.sendError(400);
	}



	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		XDATUser user = (XDATUser)request.getSession().getAttribute("user");
		if (user==null){
			response.sendError(401);
			return;
		}
		response.sendError(400);
	}
	
	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		XDATUser user = (XDATUser)request.getSession().getAttribute("user");
		if (user==null){
			response.sendError(401);
			return;
		}
		response.sendError(400);
	}
}
