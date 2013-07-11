/*
 * org.nrg.xnat.helpers.merge.AnonymizerA
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.helpers.merge;

import org.apache.commons.lang.BooleanUtils;
import org.nrg.config.entities.Configuration;
import org.nrg.config.exceptions.ConfigServiceException;
import org.nrg.dcm.Anonymize;
import org.nrg.dcm.edit.AttributeException;
import org.nrg.dcm.edit.ScriptEvaluationException;
import org.nrg.xdat.XDAT;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

public abstract class AnonymizerA implements Callable<java.lang.Void> {
	AnonymizerA next = null;
	abstract String getSubject();
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
									this.getSubject(),
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
        try {
            if(XDAT.getSiteConfiguration().containsKey("data.anonymize") && !(BooleanUtils.toBoolean(XDAT.getSiteConfiguration().getProperty("data.anonymize")))){
			return null;
		}
        } catch (ConfigServiceException exception) {
            throw new Exception("An error occurred trying to retrieve the data.anonymize configuration setting.", exception);
        }
		if (this.getScript() != null) {
			List<File> fs = this.getFilesToAnonymize();
			for (File f : fs) {
				this.anonymize(f);
			}
		}
		else {
			// there is no anon script
		}
		return null;
	}
}