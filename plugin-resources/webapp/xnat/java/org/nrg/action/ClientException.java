/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.action;

import org.restlet.data.Status;

public class ClientException extends Exception {
	public Status status =null;
	public ClientException(String msg,Exception e){
		super(msg,e);
	}
	public ClientException(String msg){
		super(msg);
}
	public ClientException(Status s, String msg,Exception e){
		super(msg,e);
}
}
