/* 
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 * 	
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

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
