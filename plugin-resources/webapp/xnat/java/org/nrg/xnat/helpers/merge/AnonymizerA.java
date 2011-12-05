package org.nrg.xnat.helpers.merge;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.nrg.config.entities.Configuration;
import org.nrg.dcm.Anonymize;
import org.nrg.dcm.AnonymizerI;
import org.nrg.dcm.edit.AttributeException;
import org.nrg.dcm.edit.ScriptEvaluationException;
import org.nrg.dcm.xnat.ScriptTable;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.model.XnatImagesessiondataI;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.om.base.BaseXnatImagesessiondata;

/**
 * Base class that actually does the work of applying the edit scripts to files.
 * @author aditya
 *
 */
public abstract class AnonymizerA implements Callable<java.lang.Void> {
	AnonymizerA next = null;
	abstract String getSubjectId();
	abstract String getLabel();
	
	public void setNext(AnonymizerA a) {
		this.next = a;
	}
	
	public void anonymize(File f) throws AttributeException,
	                                     ScriptEvaluationException, 
	                                     FileNotFoundException, 
	                                     IOException {
		Configuration script = this.getScript();
		if (script != null) {
			if (this.isEnabled()) {
				Anonymize.anonymize(f,
									this.getProjectName(),
									this.getSubjectId(),
									this.getLabel(),
									true,
									script.getId(),
									script.getContents());
				if (this.next != null) {
					this.next.anonymize(f);
				}
			}
			else {
				// anonymization is disabled.
			}
		}
		else {
			// this project does not have an anon script
		}
	}

	/**
	 * Get the appropriate edit script. 
	 * @return
	 */
	abstract Configuration getScript();
	/**
	 * Check if editing is enabled.
	 * @return
	 */
	abstract boolean isEnabled();
	
	/**
	 * Sometimes the session passed in isn't associated with a project, 
	 * for instance if the session is in the prearchive so 
	 * subclasses must specify how to get the project name. 
	 * @return
	 */
	abstract String getProjectName();
	
	/**
	 * Get the list of files that need to be anonymized.
	 * @return
	 * @throws IOException
	 */
	abstract List<File> getFilesToAnonymize() throws IOException;
	
	public java.lang.Void call() throws Exception {
		List<File> fs = this.getFilesToAnonymize();
		for (File f : fs) {
			this.anonymize(f);
		}
		return null;
	}
}