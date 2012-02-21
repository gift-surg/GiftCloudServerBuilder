// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.nrg.xdat.security.XDATUser;

public class RESTServlet extends HttpServlet {
	static org.apache.log4j.Logger logger = Logger.getLogger(RESTServlet.class);
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		XDATUser user = (XDATUser)request.getSession().getAttribute("user");
		if (user==null){
			response.sendError(401);
			return;
		}
		response.sendError(404);
		
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		XDATUser user = (XDATUser)request.getSession().getAttribute("user");
		if (user==null){
			response.sendError(401);
			return;
		}
		
					response.sendError(404);
	}



	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		XDATUser user = (XDATUser)request.getSession().getAttribute("user");
		if (user==null){
			response.sendError(401);
			return;
		}
					response.sendError(404);
	}
	
	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		XDATUser user = (XDATUser)request.getSession().getAttribute("user");
		if (user==null){
			response.sendError(401);
			return;
		}
		response.sendError(404);
					}
				}
