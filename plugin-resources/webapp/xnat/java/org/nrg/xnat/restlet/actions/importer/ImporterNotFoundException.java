package org.nrg.xnat.restlet.actions.importer;

public class ImporterNotFoundException extends Exception {
	private static final long serialVersionUID = 1L;

	public ImporterNotFoundException(String string,
			IllegalArgumentException illegalArgumentException) {
		super(string,illegalArgumentException);
	}

}
