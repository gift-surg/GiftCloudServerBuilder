package org.nrg.xnat.helpers.merge;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.dcm4che2.iod.module.macro.Code;
import org.nrg.dcm.Anonymize;
import org.nrg.dcm.xnat.ScriptTable;

public class SingleFileAnonymizer extends AnonymizerA {
	public final File f;
	public final String project;
	public final String subject;
	public final String label;
	public final String anonProject;
	private final boolean reanonymize;
	
	public SingleFileAnonymizer(File f, String project, String subject, String label, String anonProject, boolean reanonymize) {
		this.f = f;
		this.project = project;
		this.subject = subject;
		this.label = label;
		this.anonProject = anonProject;
		this.reanonymize = reanonymize;
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
	ScriptTable getScript() {
		return AnonUtils.getInstance().getScript(ProjectAnonymizer.getDBId(anonProject));
	}

	@Override
	boolean isEnabled() {
		return AnonUtils.getInstance().isEnabled(ProjectAnonymizer.getDBId(anonProject));
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
			
		}
		return null;
	}
}
