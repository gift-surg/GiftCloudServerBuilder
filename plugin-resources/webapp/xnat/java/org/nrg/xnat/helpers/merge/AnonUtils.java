package org.nrg.xnat.helpers.merge;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.nrg.config.entities.Configuration;
import org.nrg.config.exceptions.ConfigServiceException;
import org.nrg.config.services.ConfigService;
import org.nrg.config.services.impl.DefaultConfigService;
import org.nrg.dcm.xnat.EditTable;
import org.nrg.dcm.xnat.EditTableDAO;
import org.nrg.dcm.xnat.ScriptTable;
import org.nrg.dcm.xnat.ScriptTableDAO;
import org.nrg.framework.services.ContextService;
import org.nrg.xdat.XDAT;
import org.nrg.xft.XFT;
import org.nrg.xnat.helpers.editscript.DicomEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnonUtils {
	private static AnonUtils _instance;
	private static final Logger logger = LoggerFactory.getLogger(AnonUtils.class);
	
	private static final String DEFAULT_ANON_SCRIPT = "id.das";
	
	@Autowired
	private ConfigService c;
	
	public AnonUtils() throws Exception {
        if (_instance != null) {
            throw new Exception("The ContextService is already initialized, try calling getInstance() instead.");
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
		Configuration config = AnonUtils.getService().c.getConfig(DicomEdit.ToolName, 
													 path,
												     project);
		return config == null? null : config;
	}
	
	public boolean isEnabled(String path, Long project) {
		Configuration config = this.c.getConfig(DicomEdit.ToolName, 
													 path,
													 project);
		return config.getStatus().equals(Configuration.ENABLED_STRING);
	}
	
	public List<Configuration> getAllScripts (Long project) {
		List<Configuration> scripts = this.c.getConfigsByTool(DicomEdit.ToolName, project);
		return scripts;
	}
	
	public void setProjectScript (String login, String path, String script, Long project) throws ConfigServiceException {
		this.c.replaceConfig(login, 
							 	  "", 	
							 	  DicomEdit.ToolName, 
							 	  path, 
							 	  script, 
							 	  project);
	}
	
	public void setSiteWideScript(String login, String path, String script) throws ConfigServiceException {
		this.c.replaceConfig(login, 
			 	  				  "", 	
			 	  				  DicomEdit.ToolName, 
			 	  				  path, 
			 	  				  script);
	}
	
	public void enableSiteWide (String login, String path ) {
		this.c.enable(login, "", DicomEdit.ToolName, path);
	}
	
	public void enableProjectSpecific(String login, String path, Long project) {
		this.c.enable(login, "", DicomEdit.ToolName, path, project);
	}
	
	public void disableSiteWide(String login, String path) {
		this.c.disable(login, "", DicomEdit.ToolName, path);
	}
	
	public void disableProjectSpecific(String login, String path, Long project){
		this.c.disable(login, "", DicomEdit.ToolName, path, project);
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
}
