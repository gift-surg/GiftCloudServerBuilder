package org.nrg.xnat.security;


public class PasswordExpiredException extends RuntimeException{
	
	
	public PasswordExpiredException(String msg){
		super(msg);
	}
	
	public PasswordExpiredException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
}