/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.action;

public class ClientException extends Exception {
	public ClientException(String msg,Exception e){
		super(msg,e);
	}
	public ClientException(String msg){
		super(msg);
}
}
