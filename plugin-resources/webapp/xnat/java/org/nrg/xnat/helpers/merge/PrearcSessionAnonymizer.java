package org.nrg.xnat.helpers.merge;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.dcm4che2.iod.module.macro.Code;
import org.nrg.config.entities.Configuration;
import org.nrg.dcm.Anonymize;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.model.XnatImagesessiondataI;
import org.nrg.xdat.om.XnatResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Anonymize sessions in the prearchive with respect to a given project. 
 * The site-wide script is applied elsewhere.
 * 
 * Since this file may have been uploaded via the Upload Applet which applies
 * the project-specific anon scripts before sending to XNAT, before anonymization
 * each file is checked to make sure that the edit script applied to this file
 * did *not* to this project. 
 * 
 * @author aditya
 *
 */

public class PrearcSessionAnonymizer extends AnonymizerA {
	private static final Logger logger = LoggerFactory.getLogger(PrearcSessionAnonymizer.class);
	final ProjectAnonymizer p; 
	final String prearcPath;
	final XnatImagesessiondataI s;
	
	/**
	 * 
	 * @param s A session object
	 * @param projectId The project id, eg. xnat_E*
	 * @param prearcPath The location of the root prearchive directory
	 */
	public PrearcSessionAnonymizer(XnatImagesessiondataI s, String projectId, String prearcPath){
		this.prearcPath = prearcPath;
		p = new ProjectAnonymizer(s,projectId, prearcPath);
		this.s = s;
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
	
	Configuration getScript() {
		return this.p.getScript();
	}
	
	boolean isEnabled() {
		return this.p.isEnabled();
	}
	
	/**
	 * A DICOM file doesn't need anonymization if this project's script 
	 * was the last thing applied to it. 
	 * 
	 * Make sure this is only called if this project has a script or it will
	 * throw a NullPointerException
	 * @param f
	 * @return
	 * @throws IOException
	 */
	boolean needsAnonymization(File f) throws IOException {
		Code[] codes = Anonymize.getCodes(f);
		boolean needsAnonymization = true;
		if (codes != null && codes.length != 0) {
			Code last = codes[codes.length - 1];
			Configuration c = this.getScript();
			if (last.getCodeValue().equals(new Long(c.getId()).toString())) {
				needsAnonymization = false;
			}
		}
		return needsAnonymization;
	}
	
	/**
	 * Retrieve a list of files to anonymize, but in the prearchive 
	 * if the files have already been anonymized by the upload applet we don't do it
	 * again. To that end for each file we check it's last Code to ensure that it
	 * doesn't correspond to the project-specific script that is about to be 
	 * applied.
	 * 
	 */
	@Override
	public List<File> getFilesToAnonymize() throws IOException {
		List<File> ret = new ArrayList<File>();
		// anonymize everything in srcRootPath
		for(final XnatImagescandataI scan: s.getScans_scan()) {
			for (final XnatAbstractresourceI res:scan.getFile()) {
				if (res instanceof XnatResource) {
					final XnatResource abs=(XnatResource)res;
					if (abs.getFormat().equals("DICOM")){
						for (final File f: abs.getCorrespondingFiles(this.prearcPath)){
							if (this.needsAnonymization(f)) {
								ret.add(f);
							}
							else {
								// no anonymization needed so don't include this file in the list.
							}
						}
					}
				}
			}
		}
		return ret;
	}
	
	public java.lang.Void call() throws Exception {
		super.call();
		return null;
	}
}
