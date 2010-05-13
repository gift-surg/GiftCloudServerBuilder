/**
 * Copyright 2010 Washington University
 */
package org.nrg.xnat.archive;

import org.restlet.data.Status;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public class ValidationException extends ArchivingException {
	private static final long serialVersionUID = 1L;
	
	/**
	 * @param status
	 */
	public ValidationException() {
		super(Status.CLIENT_ERROR_BAD_REQUEST, "session validation failed");
	}
}
