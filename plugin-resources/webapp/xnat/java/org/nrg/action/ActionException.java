package org.nrg.action;

import org.restlet.data.Status;

public class ActionException extends Exception {

	public Status status = null;

	public ActionException() {
		super();
	}

	public ActionException(String message) {
		super(message);
	}

	public ActionException(Throwable cause) {
		super(cause);
	}

	public ActionException(String message, Throwable cause) {
		super(message, cause);
	}

}