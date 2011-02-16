//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Nov 6, 2007
 *
 */
package org.nrg.xnat.turbine.modules.screens;

import java.util.ArrayList;
import java.util.List;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.security.ElementSecurity;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFTTable;
import org.nrg.xft.utils.StringUtils;

public class XDATScreen_download_sessions extends SecureScreen {

    @Override
    protected void doBuildTemplate(RunData data, Context context)
            throws Exception {
        String[] sessions = data.getParameters().getStrings("sessions");
        if (sessions==null)
        {
            
        }else{
            List<String> sessionList =java.util.Arrays.asList(sessions);
            
            String sessionString = "";
            int counter = 0;
            for(String s : sessionList)
            {
            	if(s!=null){
                    if (counter++>0){
                        sessionString+=",";
                    }
                    if(s.contains("'"))
                    {
                    	s= StringUtils.RemoveChar(s, '\'');
                    }
                    sessionString+="'" + s + "'";
            	}
            }
            
            String project = data.getParameters().getString("project");
            
            String query=null;
            if (project==null)
            {
            	query= "SELECT expt.id,COALESCE(expt.label,expt.id) AS IDS,modality"+
            	" FROM xnat_imageSessionData isd"+
            	" LEFT JOIN xnat_experimentData expt ON expt.id=isd.id"+
            	" LEFT JOIN xnat_experimentData_share pp ON expt.id=pp.sharing_share_xnat_experimentda_id"+
            	" WHERE isd.ID IN (" + sessionString +") " +
            	                    ";";
            }else{
            	query= "SELECT expt.id,COALESCE(pp.label,expt.label,expt.id) AS IDS,modality"+
            	" FROM xnat_imageSessionData isd"+
            	" LEFT JOIN xnat_experimentData expt ON expt.id=isd.id"+
            	" LEFT JOIN xnat_experimentData_share pp ON expt.id=pp.sharing_share_xnat_experimentda_id AND pp.project='" + project + "'"+
            	" WHERE isd.ID IN (" + sessionString +") " +
            	                    ";";
            }
                      
            XFTTable table = XFTTable.Execute(query, TurbineUtils.getUser(data).getDBName(), TurbineUtils.getUser(data).getLogin());
            
            ArrayList<List> sessionSummary =table.toArrayListOfLists();
            
            context.put("sessionSummary", sessionSummary);
            
            //SELECT SCANS

            query= "SELECT type,COUNT(*) FROM xnat_imagescandata " +
                    " WHERE xnat_imagescandata.image_session_id IN (" + sessionString +") GROUP BY type;";
            
            table = XFTTable.Execute(query, TurbineUtils.getUser(data).getDBName(), TurbineUtils.getUser(data).getLogin());
            
            ArrayList<List> scans =table.toArrayListOfLists();
            context.put("scans", scans);
            
            //SELECT RECONS

            query= "SELECT type,COUNT(*) FROM xnat_reconstructedimagedata " +
                    " WHERE xnat_reconstructedimagedata.image_session_id IN (" + sessionString +") GROUP BY type;";
            
            table = XFTTable.Execute(query, TurbineUtils.getUser(data).getDBName(), TurbineUtils.getUser(data).getLogin());
            
            scans =table.toArrayListOfLists();
            context.put("recons", scans);
            
            //SELECT ASSESSORS

            query= "SELECT element_name,COUNT(*) " +
                    "FROM (SELECT xnat_imageassessordata_id, COUNT(*) FROM img_assessor_out_resource GROUP BY xnat_imageassessordata_id) img_ass_count  " +
                    "LEFT JOIN xnat_imageassessorData img_ass ON img_ass_count.xnat_imageassessordata_id=img_ass.id  " +
                    "LEFT JOIN xnat_experimentData expt ON img_ass.id=expt.id " +
                    "LEFT JOIN xdat_meta_element xme ON expt.extension=xme.xdat_meta_element_id  " +
                    " WHERE img_ass.imagesession_id IN (" + sessionString +") " +
                    "GROUP BY element_name;";
            
            table = XFTTable.Execute(query, TurbineUtils.getUser(data).getDBName(), TurbineUtils.getUser(data).getLogin());
            
            scans =table.toArrayListOfLists();
            
            ArrayList<ArrayList> assessors = new ArrayList<ArrayList>();
            for(List assessor : scans){
                ArrayList sub = new ArrayList();
                sub.addAll(assessor);
                sub.add(ElementSecurity.GetPluralDescription((String)assessor.get(0)));
                assessors.add(sub);
            }
            
            context.put("assessors", assessors);
            
            //SELECT SCAN_FORMATS

            query= "SELECT label,COUNT(*) FROM xnat_imagescandata JOIN xnat_abstractResource ON xnat_imagescandata.xnat_imagescandata_id=xnat_abstractResource.xnat_imagescandata_xnat_imagescandata_id " +
                    " WHERE xnat_imagescandata.image_session_id IN (" + sessionString +") GROUP BY label;";
            
            table = XFTTable.Execute(query, TurbineUtils.getUser(data).getDBName(), TurbineUtils.getUser(data).getLogin());
            
            ArrayList<List> formats =table.toArrayListOfLists();
            context.put("scan_formats", formats);
            
            //SELECT RESOURCES

            query= "SELECT label,COUNT(*) FROM xnat_experimentData_resource JOIN xnat_abstractResource ON xnat_experimentData_resource.xnat_abstractresource_xnat_abstractresource_id=xnat_abstractResource.xnat_abstractresource_id " +
                    " WHERE xnat_experimentData_resource.xnat_experimentdata_id IN (" + sessionString +") GROUP BY label;";
            
            table = XFTTable.Execute(query, TurbineUtils.getUser(data).getDBName(), TurbineUtils.getUser(data).getLogin());
            
            ArrayList<List> resources =table.toArrayListOfLists();
            context.put("resources", resources);
        }
    }

}
