package org.nrg.xnat.helpers.merge;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.nrg.dcm.xnat.ScriptTable;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.model.XnatImagesessiondataI;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.om.base.BaseXnatExperimentdata;
import org.nrg.xdat.om.base.BaseXnatImagesessiondata;

public class ProjectAnonymizer extends AnonymizerA implements Callable<java.lang.Void>{
	final String projectId;
	final String sessionPath;
	final XnatImagesessiondataI s;
	public ProjectAnonymizer(XnatImagesessiondataI s, String projectId, String sessionPath){
		this.s = s;
		this.projectId= projectId;
		this.sessionPath = sessionPath;
	}
	
	String getSubjectId() {
		return s.getSubjectId();
	}
	
	String getLabel() {
		return s.getLabel();
	}
	
	String getProjectName() {
		return this.projectId;
	}
	
	/**
	 * Retrieve a list of files that need to be anonymized.
	 * By default the files are retrieved from the project's archive space.
	 * @return
	 */
	public List<File> getFilesToAnonymize() {
		List<File> ret = new ArrayList<File>();
		// anonymize everything in srcRootPath
		for(final XnatImagescandataI scan: s.getScans_scan()) {
			for (final XnatAbstractresourceI res:scan.getFile()) {
				if (res instanceof XnatResource) {
					final XnatResource abs=(XnatResource)res;
					if (abs.getFormat().equals("DICOM")){
						for (final File f: abs.getCorrespondingFiles(this.sessionPath)){
							ret.add(f);
						}
					}
				}
			}
		}
		return ret;
	}
	
	static Long getDBId (String project) {
		if (project != null){
			XnatProjectdata p = XnatProjectdata.getXnatProjectdatasById(project, null, false);
			return Long.parseLong(p.getItem().getProps().get("projectdata_info").toString());
		}
		else {
			return null;
		}
	}
	
	ScriptTable getScript() {
		return AnonUtils.getInstance().getScript(getDBId(projectId));
	}
	
	boolean isEnabled() {
		return AnonUtils.getInstance().isEnabled(getDBId(projectId));
	}
	
	public java.lang.Void call() throws Exception {
		super.call();
		return null;
	}
}