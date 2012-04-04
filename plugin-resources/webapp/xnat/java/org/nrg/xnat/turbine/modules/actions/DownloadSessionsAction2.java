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
        String [] session_ids=((String[])org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedObjects("sessions",data));
        
        String [] scanFormats=((String[])org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedObjects("scan_format",data));
        
        String [] scanTypes=((String[])org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedObjects("scan_type",data));
        String [] recons=((String[])org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedObjects("recon",data));
        String [] assessors=((String[])org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedObjects("assessors",data));

        String [] resources=((String[])org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedObjects("resources",data));
		//BEGIN:IOWA customization: to allow project and subject included in path
        boolean projectIncludedInPath = "true".equalsIgnoreCase(((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("projectIncludedInPath",data)));
        boolean subjectIncludedInPath = "true".equalsIgnoreCase(((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("subjectIncludedInPath",data)));
        String extraParam = "";
        if(projectIncludedInPath){
            extraParam += "&projectIncludedInPath=true";
        }
        if(subjectIncludedInPath){
            extraParam += "&subjectIncludedInPath=true";
        }
		//END:
        
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
                	if(scanFormats!=null && scanFormats.length>0){
                		for(String scanFormat : scanFormats){
                            CatEntryBean entry = new CatEntryBean();
                            entry.setFormat("ZIP");
                            String uri=server + "data/experiments/" + session + "/scans/" + URLEncoder.encode(scanType) + "/resources/" + URLEncoder.encode(scanFormat) + "/files?format=zip" + extraParam;
                            entry.setUri(uri);
                            l.add(uri);
                            scansCatalog.addEntries_entry(entry);
                		}
                	}else{
                        CatEntryBean entry = new CatEntryBean();
                        entry.setFormat("ZIP");
                        String uri=server + "data/experiments/" + session + "/scans/" + URLEncoder.encode(scanType) + "/files?format=zip" + extraParam;
                        entry.setUri(uri);
                        l.add(uri);
                        scansCatalog.addEntries_entry(entry);
                	}
                }
                sessionCatalog.addSets_entryset(scansCatalog);
            }
            
            if (resources!=null && resources.length>0){
                final CatCatalogBean scansCatalog = new CatCatalogBean();
                scansCatalog.setId("RESOURCES");
                for(final String res : resources){
                    final CatEntryBean entry = new CatEntryBean();
                    entry.setFormat("ZIP");
                    String uri=server + "data/experiments/" + session + "/resources/" + URLEncoder.encode(res) + "/files?format=zip" + extraParam;
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
        if (((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("download_option",data)).equals("applet")){
            context.put("catalogXML", catalogXML);
            context.put("sessions", l);
            
            data.setScreenTemplate("DownloadApplet.vm");
        }else{
            data.setRedirectURI(catalogXML);
        }     
    }

}
