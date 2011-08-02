package org.nrg.xnat.helpers.merge;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.nrg.dcm.xnat.ScriptTable;
import org.nrg.xdat.model.XnatImagesessiondataI;

public class SiteWideAnonymizer extends AnonymizerA {
	final boolean located_in_prearchive;
	final XnatImagesessiondataI s;
	public SiteWideAnonymizer(XnatImagesessiondataI s, boolean located_in_prearchive) {
		this.s = s;
		this.located_in_prearchive = located_in_prearchive;
	}
	public SiteWideAnonymizer(XnatImagesessiondataI s){
		this(s,false);
	}
	
	@Override
	String getProjectName() {
		return null;
	}
	@Override
	String getSubjectId() {
		return this.s.getSubjectId();
	}
	
	@Override
	String getLabel() {
		return this.s.getLabel();
	}
	
	
	ScriptTable getScript() {
		return AnonUtils.getInstance().getScript(null);
	}
	
	public List<File> getFilesToAnonymize() {
		List<File> fs = new ArrayList<File>();
		if (located_in_prearchive) {
			PrearcSessionAnonymizer p = new PrearcSessionAnonymizer(s, s.getProject(), s.getPrearchivepath());
			fs.addAll(p.getFilesToAnonymize());
		}
		else {
			ProjectAnonymizer p = new ProjectAnonymizer(this.s, this.getProjectName(), s.getPrearchivepath());
			fs.addAll(p.getFilesToAnonymize());
		}
		return fs;
	}
	
	boolean isEnabled() {
		return AnonUtils.getInstance().isEnabled(null);
	}
	public java.lang.Void call() throws Exception {
		super.call();
		return null;
	}
}

