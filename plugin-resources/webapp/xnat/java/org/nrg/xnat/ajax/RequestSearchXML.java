//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Sep 5, 2007
 *
 */
package org.nrg.xnat.ajax;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.log4j.Logger;
import org.apache.turbine.services.rundata.RunDataService;
import org.apache.turbine.services.rundata.TurbineRunDataFacade;
import org.apache.turbine.services.velocity.TurbineVelocity;
import org.apache.turbine.util.RunData;
import org.apache.turbine.util.TurbineException;
import org.apache.velocity.context.Context;
import org.nrg.xdat.exceptions.IllegalAccessException;
import org.nrg.xdat.om.XnatAbstractprotocol;
import org.nrg.xdat.search.CriteriaCollection;
import org.nrg.xdat.search.DisplaySearch;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.security.XdatStoredSearch;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFTTableI;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.schema.Wrappers.XMLWrapper.SAXWriter;

public class RequestSearchXML {


    static org.apache.log4j.Logger logger = Logger.getLogger(RequestSearchXML.class);

    public void execute(HttpServletRequest req, HttpServletResponse response,ServletConfig sc) throws IOException{
        String bundle = req.getParameter("bundleID");

        response.setContentType("text/xml");
        response.setHeader("Cache-Control", "no-cache");
        
        HttpSession session = req.getSession();
        XDATUser user = ((XDATUser)session.getAttribute("user"));
        

        if (user!=null){
            XdatStoredSearch xss = XdatStoredSearch.GetPreLoadedSearch(bundle,true);
            if (xss!=null){
                try {
                    SAXWriter writer = new SAXWriter(response.getOutputStream(),false);
                    writer.setAllowSchemaLocation(false);
                    writer.write(xss.getItem(),false);
                } catch (Throwable e) {
                    logger.error("",e);
                }
            }
        }
        
        //System.out.print("Monitor Progress " + uploadID + "... ");
    }
}
