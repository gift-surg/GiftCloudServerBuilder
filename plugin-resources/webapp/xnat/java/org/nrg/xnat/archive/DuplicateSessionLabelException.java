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
	private final static String format = "session label %s already used in project %s";
	
	public DuplicateSessionLabelException() {
		super(status);
	}

	public DuplicateSessionLabelException(String session, String project) {
		super(status, String.format(format, session, project));
	}
}
