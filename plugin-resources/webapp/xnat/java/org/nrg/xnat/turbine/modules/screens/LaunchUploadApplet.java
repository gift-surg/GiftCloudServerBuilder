/*
 * org.nrg.xnat.turbine.modules.screens.LaunchUploadApplet
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.turbine.modules.screens;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.codehaus.jackson.map.ObjectMapper;
import org.nrg.config.services.ConfigService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.utils.AppletConfig;
import org.nrg.xnat.utils.XnatHttpUtils;

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
		
		if(StringUtils.trimToEmpty((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("search_field",data)).equals("xnat:subjectData.ID")) {
		    context.put("subject", StringUtils.trimToEmpty((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("search_value",data)));
		}
		
		//grab the applet config. Project level if it exists, otherwise, do the site-wide
		ConfigService configService = XDAT.getConfigService();
		Long projectId = null;
		
		if(projectName != null){
			final XnatProjectdata p = XnatProjectdata.getXnatProjectdatasById(projectName, user, false);
			try {
				if(user.canRead(("xnat:subjectData/project").intern(), p.getId())){
                    projectId = (long) (Integer) p.getItem().getProps().get("projectdata_info");
				}
			} catch (Exception e) {
				projectId = null;
			}
		}
		
		org.nrg.config.entities.Configuration config = configService.getConfig(AppletConfig.toolName, AppletConfig.path, projectId);
		
		if(config == null || org.nrg.config.entities.Configuration.DISABLED_STRING.equalsIgnoreCase(config.getStatus())) {
			//try to pull a site-wide config
			config = configService.getConfig(AppletConfig.toolName, AppletConfig.path, null);
		}

		if(config != null) {
			String json = config.getContents();
	    	
	        if (json != null) {
	            try {
	            	//we have JSON, so, create applet parameters from it.
	            	ObjectMapper mapper = new ObjectMapper();
	            	AppletConfig jsonParams = mapper.readValue(json, AppletConfig.class);

	            	if(jsonParams.getLaunch() != null){
	            		for(String key:jsonParams.getLaunch().keySet()){
	            			//put EVERYTHING in the context so your VM can use it.
	            			//remember it is all string (no booleans) so your VM has to test for string equality.
	            			context.put(key, jsonParams.getLaunch().get(key));
	            		}
	            	}
	            } catch (Exception exception) {
	                _log.error(exception);
	            }
	        }
		}
	}
}
