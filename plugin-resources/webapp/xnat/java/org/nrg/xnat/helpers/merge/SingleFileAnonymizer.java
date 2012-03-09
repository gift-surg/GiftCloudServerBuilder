package org.nrg.xnat.helpers.merge;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.nrg.config.entities.Configuration;
import org.nrg.xnat.helpers.editscript.DicomEdit;

public class SingleFileAnonymizer extends AnonymizerA {
	public final File f;
	public final String project;
	public final String subject;
	public final String label;
	public final String anonProject;
	private final boolean reanonymize;
	final String path;
	
	public SingleFileAnonymizer(File f, String project, String subject, String label, String anonProject, boolean reanonymize) {
		this.f = f;
		this.project = project;
		this.subject = subject;
		this.label = label;
		this.anonProject = anonProject;
		this.reanonymize = reanonymize;
		if (anonProject != null) {
			this.path = DicomEdit.buildScriptPath(DicomEdit.ResourceScope.PROJECT, anonProject);	
		}
		else {
			this.path = DicomEdit.buildScriptPath(DicomEdit.ResourceScope.SITE_WIDE, null);	
		}
		
	}
	
	@Override
	String getSubjectId() {
		return this.subject;
	}

	@Override
	String getLabel() {
		return this.label;
	}

	@Override
	Configuration getScript() {
		return AnonUtils.getService().getScript(this.path, ProjectAnonymizer.getDBId(anonProject));
	}

	@Override
	boolean isEnabled() {
		return AnonUtils.getService().isEnabled(this.path, ProjectAnonymizer.getDBId(anonProject));
	}

	@Override
	String getProjectName() {
		return this.anonProject;
	}

	@Override
	List<File> getFilesToAnonymize() {
		List<File> ret = new ArrayList<File>(1);
		ret.add(this.f);
		return ret;
	}
	
	public boolean alreadyAnonymized() {
		return false;
	}
	
	@Override
	public java.lang.Void call () throws Exception {
		if (this.reanonymize) {
			super.call();
		}
		else {
			// do nothing
		}
		return null;
	}
}
