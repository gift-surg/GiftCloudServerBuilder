//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Nov 9, 2007
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import java.io.File;
import java.io.FileWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.bean.CatCatalogBean;
import org.nrg.xdat.bean.CatEntryBean;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.TurbineUtils;

public class DownloadSessionsAction2 extends SecureAction {

    /* (non-Javadoc)
     * @see org.apache.turbine.modules.actions.VelocitySecureAction#doPerform(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    @Override
    public void doPerform(RunData data, Context context) throws Exception {
        String [] session_ids=data.getParameters().getStrings("sessions");
        
        String [] scanTypes=data.getParameters().getStrings("scan_type");
        String [] recons=data.getParameters().getStrings("recon");
        String [] assessors=data.getParameters().getStrings("assessors");
        
//IOWA customization: to allow project and subject included in path
        boolean projectIncludedInPath = "true".equalsIgnoreCase(data.getParameters().getString("projectIncludedInPath"));
        boolean subjectIncludedInPath = "true".equalsIgnoreCase(data.getParameters().getString("subjectIncludedInPath"));
        String extraParam = "";
        if(projectIncludedInPath){
            extraParam += "&projectIncludedInPath=true";
        }
        if(subjectIncludedInPath){
            extraParam += "&subjectIncludedInPath=true";
        }

        String server = TurbineUtils.GetFullServerPath();
        if (!server.endsWith("/")){
            server +="/";
        }
        
        List<String> l=new ArrayList<String>();
       CatCatalogBean cat = new CatCatalogBean();
        
        for(String session : session_ids){
            CatCatalogBean sessionCatalog = new CatCatalogBean();
            sessionCatalog.setId(session);
            
            if (scanTypes!=null && scanTypes.length>0){
                CatCatalogBean scansCatalog = new CatCatalogBean();
                scansCatalog.setId("RAW");
                for(String scanType : scanTypes){
                    CatEntryBean entry = new CatEntryBean();
                    entry.setFormat("ZIP");
                    String uri=server + "data/experiments/" + session + "/scans/" + URLEncoder.encode(scanType) + "/files?format=zip" + extraParam;
                    entry.setUri(uri);
                    l.add(uri);
                    scansCatalog.addEntries_entry(entry);
                }
                sessionCatalog.addSets_entryset(scansCatalog);
            }
            
            if (recons!=null && recons.length>0){
                CatCatalogBean scansCatalog = new CatCatalogBean();
                scansCatalog.setId("RECONSTRUCTED");
                for(String scanType : recons){
                    CatEntryBean entry = new CatEntryBean();
                    entry.setFormat("ZIP");
                    String uri=server + "data/experiments/" + session + "/reconstructions/" + URLEncoder.encode(scanType) + "/files?format=zip" + extraParam;
                    entry.setUri(uri);
                    l.add(uri);
                    scansCatalog.addEntries_entry(entry);
                }
                sessionCatalog.addSets_entryset(scansCatalog);
            }
            
            if (assessors!=null && assessors.length>0){
                CatCatalogBean scansCatalog = new CatCatalogBean();
                scansCatalog.setId("ASSESSORS");
                for(String scanType : assessors){
                    CatEntryBean entry = new CatEntryBean();
                    entry.setFormat("ZIP");
                    String uri=server + "data/experiments/" + session + "/assessors/" + URLEncoder.encode(scanType) + "/files?format=zip" + extraParam;
                    entry.setUri(uri);
                    l.add(uri);
                    scansCatalog.addEntries_entry(entry);
                }
                sessionCatalog.addSets_entryset(scansCatalog);
            }
            
            cat.addSets_entryset(sessionCatalog);       
        }
        
        String id = Calendar.getInstance().getTimeInMillis() + "";
        File f = TurbineUtils.getUser(data).getCachedFile("catalogs/" + id + ".xml");
        
        f.getParentFile().mkdirs();
        
        FileWriter fw = new FileWriter(f);
        cat.toXML(fw, true);
        fw.flush();
        fw.close();
        
        String catalogXML = server + "archive/catalogs/stored/" + id + ".xml";
        if (data.getParameters().getString("download_option").equals("applet")){
            context.put("catalogXML", catalogXML);
            context.put("sessions", l);
            
            data.setScreenTemplate("DownloadApplet.vm");
        }else{
            data.setRedirectURI(catalogXML);
        }     
    }

}
