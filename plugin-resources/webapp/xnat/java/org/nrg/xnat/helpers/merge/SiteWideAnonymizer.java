package org.nrg.xnat.helpers.merge;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.nrg.config.entities.Configuration;
import org.nrg.dcm.xnat.ScriptTable;
import org.nrg.xdat.model.XnatImagesessiondataI;
import org.nrg.xnat.helpers.editscript.DicomEdit;
import org.nrg.xnat.helpers.editscript.DicomEdit.ResourceScope;

public class SiteWideAnonymizer extends AnonymizerA {
	final boolean located_in_prearchive;
	final XnatImagesessiondataI s;
	final String path;
	public SiteWideAnonymizer(XnatImagesessiondataI s, boolean located_in_prearchive) {
		this.s = s;
		this.located_in_prearchive = located_in_prearchive;
		this.path = DicomEdit.buildScriptPath(ResourceScope.SITE_WIDE, null);
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
	
	
	Configuration getScript() {
		return AnonUtils.getService().getScript(this.path, null);
	}
	
	public List<File> getFilesToAnonymize() throws IOException {
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
		return AnonUtils.getService().isEnabled(this.path, null);
	}
	public java.lang.Void call() throws Exception {
		super.call();
		return null;
	}
}

