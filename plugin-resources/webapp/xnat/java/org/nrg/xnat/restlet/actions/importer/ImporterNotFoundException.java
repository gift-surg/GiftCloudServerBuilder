/*
 * org.nrg.xnat.restlet.actions.importer.ImporterNotFoundException
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.restlet.actions.importer;

public class ImporterNotFoundException extends Exception {
	private static final long serialVersionUID = 1L;

	public ImporterNotFoundException(String string,
			IllegalArgumentException illegalArgumentException) {
		super(string,illegalArgumentException);
	}

}
