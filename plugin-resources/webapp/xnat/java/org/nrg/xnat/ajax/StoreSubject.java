//Copyright Washington University School of Medicine All Rights Reserved
/*
 * Created on Dec 15, 2006
 *
 */
package org.nrg.xnat.ajax;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class StoreSubject{
    static org.apache.log4j.Logger logger = Logger.getLogger(StoreSubject.class);
    public void execute(HttpServletRequest req, HttpServletResponse response) throws IOException{
        response.setStatus(404);
        return;
    }
}
