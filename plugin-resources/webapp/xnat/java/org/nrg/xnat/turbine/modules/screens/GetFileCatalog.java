//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Jun 20, 2007
 *
 */
package org.nrg.xnat.turbine.modules.screens;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Hashtable;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.apache.turbine.modules.screens.RawScreen;
import org.apache.turbine.util.RunData;
import org.nrg.xdat.bean.CatCatalogBean;
import org.nrg.xdat.security.UserCache;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.security.XDATUser.FailedLoginException;
import org.nrg.xdat.turbine.utils.AccessLogger;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFTItem;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xnat.turbine.utils.CatalogSet;
import org.nrg.xnat.turbine.utils.XNATUtils;

public class GetFileCatalog extends RawScreen {
    static org.apache.log4j.Logger logger = Logger.getLogger(GetFileCatalog.class);
    
    /**
    * Set the content type to Xml. (see RawScreen)
    *
    * @param data Turbine information.
    * @return content type.
    */
    public String getContentType(RunData data)
    {
        return "text/xml";
    };
    
    protected final void doOutput(RunData data) 
    {
        AccessLogger.LogScreenAccess(data);
        HttpServletResponse response = data.getResponse();
        long startTime = Calendar.getInstance().getTimeInMillis();
             XDATUser user = null;
             String log = "";
             byte[] buf = new byte[FileUtils.SMALL_DOWNLOAD];
             try {
                 Hashtable session = null;
                 if (TurbineUtils.getUser(data)!=null){
                     user = TurbineUtils.getUser(data);
                 }

                 if (user==null){
                     response.sendError(401);
                 }
                 
                 TurbineUtils.setUser(data, user);
                 
                 XFTItem item= TurbineUtils.GetItemBySearch(data);
                                  
                 if (item==null){
                     response.sendError(404);
                 }
                
                 CatalogSet catalogSet= XNATUtils.getCatalogBean(data, item);
                 
                 CatCatalogBean catalog = catalogSet.catalog;

                 catalog.setId(data.getParameters().getString("search_value"));
                 
                 try {
                     final String identifier = data.getParameters().getString("search_element") + ":"+ data.getParameters().getString("search_field") + ":"+ data.getParameters().getString("search_value");
                     
                     data.getSession().setAttribute(identifier, catalogSet.hash);
                     response.setContentType("text/xml");
                     ServletOutputStream out = response.getOutputStream();
                     
                     OutputStreamWriter sw = new OutputStreamWriter(out);
                     catalog.toXML(sw, false);
                 
                     sw.flush();
                     sw.close();
                     
                     
                 } catch (IOException e) {
                     logger.error("",e);
                     response.sendError(404);
                 }
             } catch (XFTInitException e) {
                 logger.error("",e);
                 try {
                     response.sendError(500);
                 } catch (IOException e1) {
                     logger.error("",e1);
                 }
             } catch (ElementNotFoundException e) {
                 logger.error("",e);
                 try {
                     response.sendError(500);
                 } catch (IOException e1) {
                     logger.error("",e1);
                 }
             } catch (DBPoolException e) {
                 logger.error("",e);
                 try {
                     response.sendError(500);
                 } catch (IOException e1) {
                     logger.error("",e1);
                 }
             } catch (SQLException e) {
                 logger.error("",e);
                 try {
                     response.sendError(500);
                 } catch (IOException e1) {
                     logger.error("",e1);
                 }
             } catch (FieldNotFoundException e) {
                 logger.error("",e);
                 try {
                     response.sendError(500);
                 } catch (IOException e1) {
                     logger.error("",e1);
                 }
             } catch (FailedLoginException e) {
                 logger.error("",e);
                 try {
                     response.sendError(401);
                 } catch (IOException e1) {
                     logger.error("",e1);
                 }
             } catch (Exception e) {
                 logger.error("",e);
                 try {
                     response.sendError(500);
                 } catch (IOException e1) {
                     logger.error("",e1);
                 }
             }
            
      
    }
//    
//    @Override
//    public void finalProcessing(RunData data, Context context) {
//        CatalogSet catalogSet= XNATUtils.getCatalogBean(data, (XFTItem)item);
//        
//        CatCatalogBean catalog = catalogSet.catalog;
//        
//        try {
//            final String identifier = data.getParameters().getString("search_element") + ":"+ data.getParameters().getString("search_field") + ":"+ data.getParameters().getString("search_value");
//            
//            HttpServletResponse response = data.getResponse();
//            data.getSession().setAttribute(identifier, catalogSet.hash);
//            response.setContentType("text/xml");
//            ServletOutputStream out = response.getOutputStream();
//            
//            OutputStreamWriter sw = new OutputStreamWriter(out);
//            catalog.toXML(sw, false);
//        
//            sw.flush();
//            sw.close();
//            
//            
//        } catch (IOException e) {
//            logger.error("",e);
//        }
//        
//    }

}
