/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.action;

import org.restlet.data.Status;

public class ClientException extends ActionException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public ClientException(String msg,Throwable e){
		super(msg,e);
	}
	public ClientException(String msg){
		super(msg);
	}
	public ClientException(Status s, String msg,Throwable e){
		super(s,msg,e);
	}
	public ClientException(Status s, Throwable e){
		super(s,e);
	}
	public ClientException(Throwable e){
		super(e);
	}
	
	@Override
	public Status getStatus() {
		return (status==null)?Status.CLIENT_ERROR_BAD_REQUEST:status;
	}
}
