//Copyright 2005 Harvard University / Howard Hughes Medical Institute (HHMI) All Rights Reserved
/*
 * Created on Sep 12, 2006
 *
 */
package org.nrg.xnat.turbine.modules.screens;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.nrg.config.services.ConfigService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.screens.SecureReport;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.protocol.Protocol;
import org.nrg.xnat.utils.AppletConfig;
import org.nrg.xnat.utils.XnatHttpUtils;
import org.restlet.data.MediaType;
import org.restlet.data.Status;

/**
 * @author timo
 *
 */
public class LaunchUploadApplet extends SecureScreen {
	
	private static final Log _log = LogFactory.getLog(LaunchUploadApplet.class);
	

	
	@Override
	public void doBuildTemplate(RunData data, Context context) {
		context.put("jsessionid", XnatHttpUtils.getJSESSIONID(data));
		
		XDATUser user = TurbineUtils.getUser(data);
		String projectName = (String)context.get("project");
		
		//grab the applet config. Project level if it exists, otherwise, do the site-wide
		ConfigService configService = XDAT.getConfigService();
		Callable<Long> getProjectId = null;
		
		Callable<Long> nullCallable = new Callable<Long>() { public Long call() { return null; }};
		
		if(projectName != null){
			final XnatProjectdata p = XnatProjectdata.getXnatProjectdatasById(projectName, user, false);
			try {
				if(!user.canRead(("xnat:subjectData/project").intern(), p.getId())){
					getProjectId = nullCallable;
				}
			} catch (Exception e){
				getProjectId = nullCallable;
			}
			getProjectId = new Callable<Long>() { public Long call() { return new Long((Integer)p.getItem().getProps().get("projectdata_info"));}};
		} else {
			getProjectId = nullCallable;
		}
		
		org.nrg.config.entities.Configuration config = configService.getConfig(AppletConfig.toolName, AppletConfig.path, getProjectId);
		
		if(config == null || org.nrg.config.entities.Configuration.DISABLED_STRING.equalsIgnoreCase(config.getStatus())){
			//try to pull a site-wide config
			config = configService.getConfig(AppletConfig.toolName, AppletConfig.path, nullCallable);
		}
		if(config != null){
			String json = config.getContents();
	    	
	        if (json != null) {
	        	
	        	
	        	
	            try {            	
	            	//we have JSON, so, create applet parameters from it.
	            	ObjectMapper mapper = new ObjectMapper();
	            	AppletConfig jsonParams = mapper.readValue(json, AppletConfig.class);
	            	StringBuilder sb = new StringBuilder();
	            
	            	
	            	if(jsonParams.getLaunch() != null){
	            		for(String key:jsonParams.getLaunch().keySet()){
	            			//put EVERYTHING in the context so your VM can use it.
	            			//remember it is all string (no booleans) so your VM has to test for string equality.
	            			context.put(key, jsonParams.getLaunch().get(key));

	            		}
	            	}
	                
	            } catch (Exception exception) {
	                _log.equals(exception);
	            }
	        }
		}
	}
}
