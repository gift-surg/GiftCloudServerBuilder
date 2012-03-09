package org.nrg.xnat.helpers.merge;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.nrg.config.entities.Configuration;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.model.XnatImagesessiondataI;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xnat.helpers.editscript.DicomEdit;

/**
 * Apply a project-specific script to this session. The PrearcSessionAnonymizer class
 * operates somewhat similarly the only difference being here the edit scripts 
 * are *always* applied. 
 * 
 * Indeed the PrearcSessionAnonymizer delegates a lot of it's functionality to this class.
 * @author aditya
 *
 */
public class ProjectAnonymizer extends AnonymizerA implements Callable<java.lang.Void>{
	final String projectId;
	final String sessionPath;
	final String label;
	final XnatImagesessiondataI s;
	final String path;
	/**
	 * 
	 * @param s The session object.
	 * @param projectId The project Id, eg. xnat_E*
	 * @param sessionPath The root path of this project's session directory
	 */
	public ProjectAnonymizer(XnatImagesessiondataI s, String projectId, String sessionPath){
		this.s = s;
		this.projectId= projectId;
		this.sessionPath = sessionPath;
		this.label = s.getLabel();
		this.path = DicomEdit.buildScriptPath(DicomEdit.ResourceScope.PROJECT, projectId);
	}
	
	public ProjectAnonymizer(String label, XnatImagesessiondataI s, String projectId, String sessionPath) {
		this.s = s;
		this.projectId = projectId;
		this.sessionPath = sessionPath;
		this.label = label;
		this.path = DicomEdit.buildScriptPath(DicomEdit.ResourceScope.PROJECT, projectId);
	} 
	
	@Override
	String getSubjectId() {
		return s.getSubjectId();
	}
	
	@Override
	String getLabel() {
		return this.label;
	}
	
	@Override
	String getProjectName() {
		return this.projectId;
	}
	
	/**
	 * Retrieve a list of files that need to be anonymized.
	 * By default the files are retrieved from the project's archive space.
	 * @return
	 */
	@Override
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
	
	@Override
	Configuration getScript() {
		return AnonUtils.getService().getScript(this.path, getDBId(projectId));
	}
	
	@Override
	boolean isEnabled() {
		return AnonUtils.getService().isEnabled(this.path, getDBId(projectId));
	}
	
	public java.lang.Void call() throws Exception {
		super.call();
		return null;
	}
}