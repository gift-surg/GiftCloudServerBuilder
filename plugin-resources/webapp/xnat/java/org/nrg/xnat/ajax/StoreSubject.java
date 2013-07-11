/*
 * org.nrg.xnat.ajax.StoreSubject
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.ajax;

import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class StoreSubject{
    static org.apache.log4j.Logger logger = Logger.getLogger(StoreSubject.class);
    public void execute(HttpServletRequest req, HttpServletResponse response) throws IOException{
        response.setStatus(404);
        return;
    }
}
