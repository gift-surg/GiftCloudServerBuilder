//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Nov 6, 2007
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import java.io.File;
import java.io.FileWriter;
import java.util.Calendar;
import java.util.Hashtable;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.bean.CatCatalogBean;
import org.nrg.xdat.bean.CatEntryBean;
import org.nrg.xdat.search.DisplaySearch;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.ListingAction;
import org.nrg.xdat.turbine.utils.TurbineUtils;

public class DownloadSessionsAction extends ListingAction {

    @Override
    public String getDestinationScreenName(RunData data) {
        return "XDATScreen_download_sessions.vm";
    }

    
    public void finalProcessing(RunData data,Context context) throws Exception {
        DisplaySearch search = TurbineUtils.getSearch(data);
        search.setPagingOn(false);
        //Load search results into a table
        org.nrg.xft.XFTTable table = (org.nrg.xft.XFTTable)search.execute(null,TurbineUtils.getUser(data).getLogin());
        search.setPagingOn(true);
        
        XDATUser user = TurbineUtils.getUser(data);
        if (user == null)
        {
            throw new Exception("Invalid User.");
        }
        
        String sessionIDHeader ="session_id";
        
        if (search.getRootElement().getFullXMLName().equals("xnat:mrSessionData")){
            
        }else{
            
        }
        
        table.resetRowCursor();
        while (table.hasMoreRows()){
            Hashtable row= table.nextRowHash();
            data.getParameters().append("sessions", (String)row.get(sessionIDHeader));
        }
    }
}
