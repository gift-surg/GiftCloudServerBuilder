package org.nrg.xnat.turbine.modules.screens;

import java.io.File;
import java.util.Date;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.bean.XnatImagesessiondataBean;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.helpers.prearchive.PrearcTableBuilder;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;

public abstract class PrearchiveSessionScreen extends SecureScreen {

	public PrearchiveSessionScreen() {
		super();
	}

	@Override
	protected void doBuildTemplate(RunData data, Context context) throws Exception {
		final String folder = (String)TurbineUtils.GetPassedParameter("folder",data);
	    final String timestamp = (String)TurbineUtils.GetPassedParameter("timestamp",data);
	    final String project = (String)TurbineUtils.GetPassedParameter("project",data);	// can we final this?
	    final XDATUser user = TurbineUtils.getUser(data);
	    
	    final File sessionDir=PrearcUtils.getPrearcSessionDir(user, project, timestamp, folder,false);
	    
	    final File sessionXML = new File(sessionDir.getPath() + ".xml");
		final XnatImagesessiondataBean sessionBean;
		try {
			sessionBean = PrearcTableBuilder.parseSession(sessionXML);
		} catch (Exception e) {
			error(e, data);
			return;
		}
		
		Date upload=PrearcUtils.parseTimestampDirectory(timestamp);

		context.put("uploadDate",upload);
		context.put("session",sessionBean);
		context.put("url", String.format("/prearchive/projects/%s/%s/%s",project,timestamp,folder));
		
		finalProcessing(sessionBean, data,context);
	}

	public abstract void finalProcessing(XnatImagesessiondataBean session, RunData data, Context context) throws Exception;
}