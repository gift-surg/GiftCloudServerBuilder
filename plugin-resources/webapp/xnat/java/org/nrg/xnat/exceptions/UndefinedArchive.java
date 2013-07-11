/*
 * org.nrg.xnat.exceptions.UndefinedArchive
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
 * @author timo
 *
 */
@SuppressWarnings("serial")
public class UndefinedArchive extends Exception {

    /**
     * 
     */
    public UndefinedArchive() {
    }

    /**
     * @param message
     */
    public UndefinedArchive(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public UndefinedArchive(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public UndefinedArchive(String message, Throwable cause) {
        super(message, cause);
    }

}
