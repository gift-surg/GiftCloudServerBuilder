/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.action;

import org.restlet.data.Status;

public class ServerException extends ActionException {
	public ServerException(String msg,Exception e){
		super(msg,e);
	}
	public ServerException(String msg){
		super(msg);
	}
	public ServerException(Status s, String msg,Exception e){
		super(msg,e);
	}
}