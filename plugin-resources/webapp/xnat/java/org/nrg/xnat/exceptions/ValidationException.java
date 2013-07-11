/*
 * org.nrg.xnat.exceptions.ValidationException
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.exceptions;


/**
 * @author Timothy R. Olsen <olsent@wustl.edu>
 *
 */
public class ValidationException extends Exception {
	public ValidationException(String s){
		super(s);
	}
}
