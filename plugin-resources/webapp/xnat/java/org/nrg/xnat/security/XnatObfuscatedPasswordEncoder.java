/*
 * org.nrg.xnat.security.XnatObfuscatedPasswordEncoder
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.security;

import org.nrg.xdat.security.XDATUser;
import org.springframework.security.authentication.encoding.BasePasswordEncoder;

public class XnatObfuscatedPasswordEncoder extends
        BasePasswordEncoder {

	private final boolean sha2Encode;
	
    public XnatObfuscatedPasswordEncoder() {
        this (false);
    }

    public XnatObfuscatedPasswordEncoder(boolean sha2) throws IllegalArgumentException {
        this.sha2Encode = sha2;
    }

    public String encodePassword(String rawPass, Object salt) {
    	String obfuscated = XDATUser.EncryptString(rawPass,"obfuscate");
    	if(sha2Encode){
    		return XDATUser.EncryptString(obfuscated,"SHA-256");
    	}
    	else{
    		return obfuscated;
    	}
    }
    
    public boolean isPasswordValid(String encPass, String rawPass,
            Object salt) {
    	boolean isPasswordValid = false;
    	if(encPass!=null && rawPass!=null){
	        String pass1 = encPass;
	        String pass2 = encodePassword(rawPass, salt);
	        isPasswordValid = pass1.equals(pass2);
    	}
        return isPasswordValid;
    }
}