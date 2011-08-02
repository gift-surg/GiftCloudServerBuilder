package org.nrg.xnat.helpers.merge;

import java.io.File;
import java.io.FileNotFoundException;

import org.nrg.dcm.xnat.EditTable;
import org.nrg.dcm.xnat.EditTableDAO;
import org.nrg.dcm.xnat.ScriptTable;
import org.nrg.dcm.xnat.ScriptTableDAO;
import org.nrg.framework.services.ContextService;
import org.nrg.xft.XFT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnonUtils {
	private static AnonUtils _instance;
	private static final Logger logger = LoggerFactory.getLogger(AnonUtils.class);
	
	private static final String DEFAULT_ANON_SCRIPT = "id.das";
	private final ScriptTableDAO st;
	private final EditTableDAO et;
	
	public AnonUtils() throws Exception {
        if (_instance != null) {
            throw new Exception("The ContextService is already initialized, try calling getInstance() instead.");
        }
        ContextService _c = ContextService.getInstance();
        this.st = _c.getBean(ScriptTableDAO.class);
        this.et = _c.getBean(EditTableDAO.class);
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
	public ScriptTable getScript(Long project) {
		ScriptTable s = this.st.get(project);
		return s == null? null : s;
	}
	
	public boolean isEnabled(Long project) {
		EditTable e = this.et.get(project);
		return e.getEdit();
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
