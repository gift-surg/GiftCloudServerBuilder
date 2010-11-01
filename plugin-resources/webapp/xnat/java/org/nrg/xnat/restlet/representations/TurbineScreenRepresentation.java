// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.representations;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.turbine.modules.PageLoader;
import org.apache.turbine.services.rundata.TurbineRunData;
import org.apache.turbine.services.template.TemplateService;
import org.apache.turbine.services.template.TurbineTemplate;
import org.apache.turbine.util.RunData;
import org.apache.turbine.util.ServerData;
import org.apache.turbine.util.TurbineException;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.restlet.rundata.RestletRunData;
import org.nrg.xnat.restlet.servlet.XNATRestletServlet;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.resource.OutputRepresentation;

public abstract class TurbineScreenRepresentation extends OutputRepresentation {
	RunData data =null;
	Request request=null;
	XDATUser user =null;

	public TurbineScreenRepresentation(MediaType mediaType,Request _request, XDATUser _user) {
		super(mediaType);
		request=_request;
		user=_user;
	}

	public void turbineScreen(RunData data,OutputStream out)throws IOException,Exception{
		TemplateService templateService = null;
        templateService = TurbineTemplate.getService();
        String defaultPage = (templateService == null)
                ? null :templateService.getDefaultPageName(data);

        PrintWriter writer= new PrintWriter(out);

        if(data instanceof RestletRunData){
			((RestletRunData)data).hijackOutput(writer);
		}else{
			throw new RestletTurbineConfigurationException("Inproper Turbine configuration for RESTLET support.");
		}

        PageLoader.getInstance().exec(data, defaultPage);

		//COPIED FROM org.apache.turbine.Turbine.doGet
        if (data.isPageSet() && data.isOutSet() == false)
        {
            // Output the Page.
            data.getPage().output(out);
        }

        writer.flush();
        writer.close();
	}
	
	public static RunData populateRunData(HttpServletRequest request, HttpServletResponse response,XDATUser user) throws TurbineException{
//		RunDataService rundataService = null;
//		rundataService = TurbineRunDataFacade.getService();
//		if (rundataService == null)
//		{
//		    throw new TurbineException(
//		            "No RunData Service configured!");
//		}
//		RunData data = rundataService.getRunData("restlet",request, response, XNATRestletServlet.REST_CONFIG);

		TurbineRunData data = new RestletRunData();
        data.setParameterParser(new org.apache.turbine.util.parser.DefaultParameterParser());
        data.setCookieParser(new org.apache.turbine.util.parser.DefaultCookieParser());

        // Set the request and response.
        data.setRequest(request);
        data.setResponse(response);

        // Set the servlet configuration.
        data.setServletConfig(XNATRestletServlet.REST_CONFIG);

        // Set the ServerData.
        data.setServerData(new ServerData(request));
		
		if(data.getSession().getAttribute("user")==null){
			data.getSession().setAttribute("user", user);
			data.getSession().setAttribute("loggedin",true);
		}
		
		//RENAME script name /REST to /app
		data.getServerData().setScriptName("/app");
		
		return data;
	}
	
	public class RestletTurbineConfigurationException extends Exception{
		public RestletTurbineConfigurationException(String msg){
			super(msg);
		}
	}
}
