/* 
 * org.nrg.xnat.exceptions.PipelineNotFoundException
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 * 	
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
*/

package org.nrg.xnat.exceptions;

public class PipelineNotFoundException extends Exception {

    public PipelineNotFoundException() {

    }



    /**

     * @param message

     */

    public PipelineNotFoundException(String message) {

        super(message);

    }



    /**

     * @param cause

     */

    public PipelineNotFoundException(Throwable cause) {

        super(cause);

    }



    /**

     * @param message

     * @param cause

     */

    public PipelineNotFoundException(String message, Throwable cause) {

        super(message, cause);

    }


	
}
