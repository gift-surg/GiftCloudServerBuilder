// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.servlet;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.restlet.Component;

import com.noelios.restlet.ext.servlet.ServerServlet;

public class XNATRestletServlet extends ServerServlet {
	public static ServletConfig REST_CONFIG=null;
	@Override
	public void init() throws ServletException {
		super.init();
		
		XNATRestletServlet.REST_CONFIG=this.getServletConfig();
	}
	@Override
	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		super.service(request, response);
	}

    
}
