package org.nrg.xnat.restlet.actions.importer;

public class ImporterNotFoundException extends Exception {

	public ImporterNotFoundException(String string,
			IllegalArgumentException illegalArgumentException) {
		super(string,illegalArgumentException);
	}

}
