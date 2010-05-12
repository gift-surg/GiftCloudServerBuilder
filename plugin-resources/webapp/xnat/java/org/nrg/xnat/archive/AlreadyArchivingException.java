/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.xnat.archive;

import org.restlet.data.Status;

/**
 * Indicates that the session to be archived is already in the transfer pipeline.
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public class AlreadyArchivingException extends ArchivingException {
	private static final long serialVersionUID = 1L;
	private static final Status status = Status.CLIENT_ERROR_FORBIDDEN;
	private static final String message = "Session archiving already in progress";
	
	public AlreadyArchivingException() {
		super(status, message);
	}
}
