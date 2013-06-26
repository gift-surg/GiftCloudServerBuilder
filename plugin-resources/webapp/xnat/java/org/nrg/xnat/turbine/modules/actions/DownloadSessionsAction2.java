//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Nov 9, 2007
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import java.io.File;
import java.io.FileWriter;
import java.net.URLEncoder;
import java.util.*;

import org.apache.commons.collections.ListUtils;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.bean.CatCatalogBean;
import org.nrg.xdat.bean.CatEntryBean;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFTTable;

public class DownloadSessionsAction2 extends SecureAction {

    /* (non-Javadoc)
     * @see org.apache.turbine.modules.actions.VelocitySecureAction#doPerform(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    @Override
    public void doPerform(RunData data, Context context) throws Exception {
        String [] session_ids=((String[])org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedObjects("sessions",data));

        String [] requestScanTypes=((String[])org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedObjects("scan_type",data));
        String [] scanFormats=((String[])org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedObjects("scan_format",data));
        String [] recons=((String[])org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedObjects("recon",data));
        String [] assessors=((String[])org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedObjects("assessors",data));
        String [] resources=((String[])org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedObjects("resources",data));

		//BEGIN:IOWA customization: to allow project and subject included in path
        boolean projectIncludedInPath = "true".equalsIgnoreCase((String)TurbineUtils.GetPassedParameter("projectIncludedInPath",data));
        boolean subjectIncludedInPath = "true".equalsIgnoreCase((String)TurbineUtils.GetPassedParameter("subjectIncludedInPath",data));
        boolean simplified = "true".equalsIgnoreCase((String) TurbineUtils.GetPassedParameter("simplified", data));
        
        String extraParam = "";
        if(projectIncludedInPath){
            extraParam += "&projectIncludedInPath=true";
        }
        if(subjectIncludedInPath){
            extraParam += "&subjectIncludedInPath=true";
        }
        if(simplified){
            extraParam += "&structure=simplified";
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

            // narrow down the range of scan types to only the ones relevant to this session
            String [] sessionScanTypes;
            String query= "SELECT DISTINCT type FROM xnat_imagescandata WHERE image_session_id = '" + session + "' AND type IN (" + sqlList(requestScanTypes) + ")";
            XFTTable table = XFTTable.Execute(query, TurbineUtils.getUser(data).getDBName(), TurbineUtils.getUser(data).getLogin());
            List<String> list = table.convertColumnToArrayList("type");
            sessionScanTypes = list.toArray(new String[0]);

            if (sessionScanTypes!=null && sessionScanTypes.length>0){
                CatCatalogBean scansCatalog = new CatCatalogBean();
                scansCatalog.setId("RAW");
                for(String scanType : sessionScanTypes){
                	if(scanType.indexOf("/")>-1){
                    	scanType=scanType.replace("/","[SLASH]");//this is such an ugly hack.  If a slash is included in the scan type and thus in the URL, it breaks the GET command.  Even if it is properly escaped.  So, I'm adding this alternative encoding of slash to allow us to work around the issue.  Hopefully Spring MVC will eliminate it.
                    }
                	if(scanType.indexOf(",")>-1){
                    	scanType=scanType.replace(",","[COMMA]");
                    }
                	
                	if(scanFormats!=null && scanFormats.length>0){
                		for(String scanFormat : scanFormats){
                            CatEntryBean entry = new CatEntryBean();
                            entry.setFormat("ZIP");                            
                            String uri=server + "data/experiments/" + session + "/scans/" + URLEncoder.encode(scanType, "UTF-8") + "/resources/" + URLEncoder.encode(scanFormat, "UTF-8") + "/files?format=zip" + extraParam;
                            entry.setUri(uri);
                            l.add(uri);
                            scansCatalog.addEntries_entry(entry);
                		}
                	}else{
                        CatEntryBean entry = new CatEntryBean();
                        entry.setFormat("ZIP");
                        String uri=server + "data/experiments/" + session + "/scans/" + URLEncoder.encode(scanType, "UTF-8") + "/files?format=zip" + extraParam;
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
                    String uri=server + "data/experiments/" + session + "/resources/" + URLEncoder.encode(res, "UTF-8") + "/files?format=zip" + extraParam;
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
                    String uri=server + "data/experiments/" + session + "/reconstructions/" + URLEncoder.encode(scanType, "UTF-8") + "/files?format=zip" + extraParam;
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
                    String uri=server + "data/experiments/" + session + "/assessors/" + URLEncoder.encode(scanType, "UTF-8") + "/files?format=zip" + extraParam;
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
        if (((String)TurbineUtils.GetPassedParameter("download_option",data)).equals("applet")){
            context.put("catalogXML", catalogXML);
            context.put("sessions", l);
            
            data.setScreenTemplate("DownloadApplet.vm");
        }else{
            data.setRedirectURI(catalogXML);
        }     
    }

    private String sqlList(String[] list) {
        StringBuilder sb = new StringBuilder();
        for (String item : list) {
            if (sb.length() > 0) sb.append(',');
            sb.append("'").append(item).append("'");
        }
        return sb.toString();
    }

}
