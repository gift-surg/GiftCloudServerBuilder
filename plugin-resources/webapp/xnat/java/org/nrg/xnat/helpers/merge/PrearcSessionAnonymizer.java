package org.nrg.xnat.helpers.merge;

import java.io.File;
import java.util.List;

import org.nrg.dcm.xnat.ScriptTable;
import org.nrg.xdat.model.XnatImagesessiondataI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrearcSessionAnonymizer extends AnonymizerA {
	private static final Logger logger = LoggerFactory.getLogger(PrearcSessionAnonymizer.class);
	final ProjectAnonymizer p; 
	final String prearcPath;
	public PrearcSessionAnonymizer(XnatImagesessiondataI s, String projectId, String prearcPath){
		this.prearcPath = prearcPath;
		p = new ProjectAnonymizer(s,projectId, prearcPath);
	}
	
	@Override
	String getProjectName() {
		return this.p.projectId;
	}
	
	@Override
	String getLabel() {
		return this.p.getLabel();
	}
	
	@Override
	String getSubjectId() {
		return this.p.getSubjectId();
	}
	
	Long getDBId (String project) {
		return this.p.getDBId(project);
	}
	
	ScriptTable getScript() {
		return this.p.getScript();
	}
	
	boolean isEnabled() {
		return this.p.isEnabled();
	}
	
	@Override
	public List<File> getFilesToAnonymize() {
		return this.p.getFilesToAnonymize();
	}
	
	public java.lang.Void call() throws Exception {
		super.call();
		return null;
	}
}
