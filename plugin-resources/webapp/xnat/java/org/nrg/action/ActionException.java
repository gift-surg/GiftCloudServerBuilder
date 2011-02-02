package org.nrg.action;

import org.restlet.data.Status;

public abstract class ActionException extends Exception {

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
	
	public ActionException(Status s, String msg,Throwable e){
		super(msg,e);
		status=s;
	}
	
	public ActionException(Status s, Throwable e){
		super(e);
		status=s;
	}

	public abstract Status getStatus();
}