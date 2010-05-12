/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.xnat.archive;

import org.restlet.data.Status;

/**
 * Indicates that the label of a session to be archived has already been
 * used for an archived session in the same project.
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public class DuplicateSessionLabelException extends ArchivingException {
	private final static long serialVersionUID = 1L;
	private final static Status status = Status.CLIENT_ERROR_FORBIDDEN;
	
	public DuplicateSessionLabelException() {
		super(status);
	}

	public DuplicateSessionLabelException(String message) {
		super(status, message);
	}

	public DuplicateSessionLabelException(Throwable cause) {
		super(status, cause);
	}

	public DuplicateSessionLabelException(String message, Throwable cause) {
		super(status, message, cause);
	}
}
