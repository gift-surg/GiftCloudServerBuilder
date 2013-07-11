/*
 * org.nrg.xnat.helpers.prearchive.SessionException
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.helpers.prearchive;

public class SessionException extends Exception {
	private static final long serialVersionUID = 1L;
	public SessionException (String err) {
		super (err);
	}
	public SessionException () {
		super ();
	}
}
