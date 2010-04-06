//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Nov 27, 2007
 *
 */
package org.nrg.xnat.ajax;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.nrg.xdat.ajax.StoreXML;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.DBPoolException;

public class RequestProtocolDefinitionGroups {
    static org.apache.log4j.Logger logger = Logger.getLogger(RequestProtocolDefinitionGroups.class);

    public void execute(HttpServletRequest req, HttpServletResponse response) throws IOException{
        XDATUser user = (XDATUser)req.getSession().getAttribute("user");

        response.setContentType("text/xml");
        response.setHeader("Cache-Control", "no-cache");
        
        try {
            String query = "SELECT id,data_type,description FROM xnat_fielddefinitiongroup;";
            XFTTable table = XFTTable.Execute(query, user.getDBName(), user.getLogin());
            response.getWriter().write("<fieldDefinitionGroups>");
            table.resetRowCursor();
            while(table.hasMoreRows()){
                Object[] row=table.nextRow();
                response.getWriter().write("<fieldDefinitionGroup data-type=\"" + row[1] + "\" name=\"" + row[0] + "\" description=\"" + row[2] + "\"/>");
            }
            response.getWriter().write("</fieldDefinitionGroups>");
        } catch (SQLException e) {
            logger.error("",e);
        } catch (DBPoolException e) {
            logger.error("",e);
        }
       
        
    }
}
