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
