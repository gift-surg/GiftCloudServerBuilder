/*
 * org.nrg.xnat.helpers.merge.AnonUtils
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.helpers.merge;

import org.nrg.config.entities.Configuration;
import org.nrg.config.exceptions.ConfigServiceException;
import org.nrg.config.services.ConfigService;
import org.nrg.xdat.XDAT;
import org.nrg.xft.XFT;
import org.nrg.xnat.helpers.editscript.DicomEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

@Service
public class AnonUtils {
	public AnonUtils() throws Exception {
        if (_instance != null) {
            throw new Exception("The AnonUtils service is already initialized, try calling getInstance() instead.");
        }
        _instance = this;
    }
	
	public static AnonUtils getService() {
	    if (_instance == null) {
	    	_instance = XDAT.getContextService().getBean(AnonUtils.class);
	    }
	    return _instance;
	}
	
	public Configuration getScript(String path, Long project) {
        if (logger.isDebugEnabled()) {
            logger.debug("Retrieving script for {}, {} for project: {}", new Object[] { DicomEdit.ToolName, path, project });
        }

        return _configService.getConfig(DicomEdit.ToolName,
													 path,
												     project);
	}
	
	public boolean isEnabled(String path, Long project) {
        Configuration config = _configService.getConfig(DicomEdit.ToolName,
													 path,
													 project);
        final boolean enabled = config.getStatus().equals(Configuration.ENABLED_STRING);
        if (logger.isDebugEnabled()) {
            logger.debug("Retrieved status {} for {}, {} for project: {}", new Object[] { enabled, DicomEdit.ToolName, path, project });
        }

        return enabled;
	}
	
	public List<Configuration> getAllScripts (Long project) {
        List<Configuration> scripts = _configService.getConfigsByTool(DicomEdit.ToolName, project);
        if (logger.isDebugEnabled()) {
            if (scripts == null) {
                logger.debug("Retrieved no scripts for {}, {} for project: {}", new Object[] { DicomEdit.ToolName, project });
            } else if (scripts.size() == 0) {
                logger.debug("Retrieved no scripts for {}, {} for project: {}", new Object[] { DicomEdit.ToolName, project });
            } else {
                logger.debug("Retrieved {} scripts for {}, {} for project: {}", new Object[] { scripts.size(), DicomEdit.ToolName, project });
            }
        }

		return scripts;
	}
	
	public void setProjectScript (String login, String path, String script, Long project) throws ConfigServiceException {
        if (logger.isDebugEnabled()) {
            logger.debug("Setting script for {}, {} for project: {}", new Object[] { DicomEdit.ToolName, path, project });
        }
        _configService.replaceConfig(login,
							 	  "", 	
							 	  DicomEdit.ToolName, 
							 	  path, 
							 	  script, 
							 	  project);
	}
	
	public void setSiteWideScript(String login, String path, String script) throws ConfigServiceException {
        _configService.replaceConfig(login,
			 	  				  "", 	
			 	  				  DicomEdit.ToolName, 
			 	  				  path, 
			 	  				  script);
	}
	
	public void enableSiteWide (String login, String path ) throws ConfigServiceException {
        _configService.enable(login, "", DicomEdit.ToolName, path);
	}
	
	public void enableProjectSpecific(String login, String path, Long project) throws ConfigServiceException {
        _configService.enable(login, "", DicomEdit.ToolName, path, project);
	}
	
	public void disableSiteWide(String login, String path) throws ConfigServiceException {
        _configService.disable(login, "", DicomEdit.ToolName, path);
	}
	
	public void disableProjectSpecific(String login, String path, Long project) throws ConfigServiceException {
        _configService.disable(login, "", DicomEdit.ToolName, path, project);
	}
	
	public static File getDefaultScript () throws FileNotFoundException {
		final File def = new File (XFT.GetConfDir(), DEFAULT_ANON_SCRIPT);
		if (def.exists()) {
			return def;
		}
		else {
			throw new FileNotFoundException("Default anon script: " + DEFAULT_ANON_SCRIPT + " not found in " + XFT.GetConfDir());
		}
	}

    private static final Logger logger = LoggerFactory.getLogger(AnonUtils.class);

    private static final String DEFAULT_ANON_SCRIPT = "id.das";

    private static AnonUtils _instance;

    @Inject
    private ConfigService _configService;
}
