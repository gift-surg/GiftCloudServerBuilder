/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.action;

public class ServerException extends Exception {
	public ServerException(String msg,Exception e){
		super(msg,e);
	}
}