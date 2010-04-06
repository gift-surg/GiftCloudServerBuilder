//Copyright 2006 Harvard University / Washington University School of Medicine All Rights Reserved
/*
 * Created on Sep 19, 2006
 *
 */
package org.nrg.xnat.exceptions;

/**
 * @author timo
 *
 */
@SuppressWarnings("serial")
public class InvalidArchiveStructure extends Exception {

    /**
     * 
     */
    public InvalidArchiveStructure() {
    }

    /**
     * @param message
     */
    public InvalidArchiveStructure(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public InvalidArchiveStructure(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public InvalidArchiveStructure(String message, Throwable cause) {
        super(message, cause);
    }

}
