package org.nrg.xnat.helpers.prearchive;

public class SessionException extends Throwable {
	private static final long serialVersionUID = 1L;
	public SessionException (String err) {
		super (err);
	}
	public SessionException () {
		super ();
	}
}
