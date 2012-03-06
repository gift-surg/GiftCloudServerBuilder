package org.nrg.xnat.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
        String pass1 = encPass;
        String pass2 = encodePassword(rawPass, salt);

        return pass1.equals(pass2);
    }
}