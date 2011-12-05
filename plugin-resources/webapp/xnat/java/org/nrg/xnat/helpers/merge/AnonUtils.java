package org.nrg.xnat.helpers.merge;

import java.io.File;
import java.io.FileNotFoundException;

import org.nrg.config.entities.Configuration;
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

public class AnonUtils {
	private static AnonUtils _instance;
	private static final Logger logger = LoggerFactory.getLogger(AnonUtils.class);
	
	private static final String DEFAULT_ANON_SCRIPT = "id.das";
	private static ConfigService c;
	
	public AnonUtils() throws Exception {
        if (_instance != null) {
            throw new Exception("The ContextService is already initialized, try calling getInstance() instead.");
        }
        this.c = XDAT.getConfigService();
        _instance = this;
    }
	
	public static AnonUtils getInstance() {
		if (_instance == null) {
            try {
                _instance = new AnonUtils();
            } catch (Exception e) {
            	logger.error("Unable to get an instance of AnonUtils.class");
                // Do nothing. This should never happen, since the exception is only thrown when the service is already initialized.
            }
        }
        return _instance;
	}
	public Configuration getScript(String path, Long project) {
		Configuration config = this.c.getConfig(DicomEdit.ToolName, 
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
