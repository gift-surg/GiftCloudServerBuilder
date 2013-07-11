/*
 * org.nrg.xnat.archive.DuplicateSessionLabelException
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.archive;

import org.restlet.data.Status;

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
