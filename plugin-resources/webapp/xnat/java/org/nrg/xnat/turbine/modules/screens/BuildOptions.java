//Copyright 2005 Harvard University / Howard Hughes Medical Institute (HHMI) All Rights Reserved
/*
 * BuildOptions.java
 *
 * Created on May 15, 2002, 9:11 AM
 */

package org.nrg.xnat.turbine.modules.screens;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.pipeline.BuildSpecification;
import org.nrg.xdat.turbine.modules.screens.SecureReport;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFT;
/**
 *
 * @author  dan
 * @version
 */
public class BuildOptions extends SecureReport {

    public void preProcessing(RunData data, Context context)
    {
        //TurbineUtils.InstanciatePassedItemForScreenUse(data,context);
    }
	
    public void doBuildTemplate(RunData data, Context context) {
        finalProcessing(data,context);
    }
    
	public void finalProcessing(RunData data, Context context){
        
        if (data.getParameters().get("sessions") == null) {
            /*if (context.get("om")==null)
            {*/
                String errorString = "<img src=\"/fcon/images/error.gif\">An error has occurred.";
    			errorString += "<p>Please contact the <a href=\"" + XFT.GetAdminEmail() + "?subject=archive submission error\">NRG techdesk</a> to resolve the error.</p>";
    			data.setMessage(errorString);
    			data.setScreenTemplate("Error.vm");
    			return;	
            /*}        
            XnatMrsessiondata mr = (XnatMrsessiondata)context.get("om");
            sessions.add(mr); */
        }
              String csv_sessions = data.getParameters().get("sessions");
              System.out.println("Recd Sessions " + csv_sessions);
              String[] projectSessions = csv_sessions.split(":");
              if (projectSessions == null || projectSessions.length < 2) {
                  data.setMessage("Missing required project ID and/or Session ID");
                  data.setScreenTemplate("Error.vm");
                  return;
              }
              String projectID = projectSessions[0];
              System.out.println("Project ID " + projectID);
        
        LinkedHashMap pipelines = null;
        try {
            pipelines = BuildSpecification.GetInstance().getPipelinesForProject(projectID, TurbineUtils.getUser(data));
            if (pipelines == null) {
                String errorString = "<img src=\"/fcon/images/error.gif\">No builds specified for this project";
                errorString += "<p>Please contact the <a href=\"mailto:"+XFT.GetAdminEmail()+"?subject=Missing Builds for Proejct ID " + projectID + "\">NRG techdesk</a> to resolve the error.</p>";
                data.setMessage(errorString);
                data.setScreenTemplate("Error.vm");
                return; 
            }
            context.put("pipelines",pipelines);
            context.put("sessions",csv_sessions);
        }catch(Exception e) {
            String errorString = "<img src=\"/fcon/images/error.gif\">An error has occurred. Couldnt find the Build Specification File";
            errorString += "<p>Please contact the <a href=\"mailto:"+XFT.GetAdminEmail()+"?subject=Session Build Error - Spec file not found\">NRG techdesk</a> to resolve the error.</p>";
            data.setMessage(errorString);
            data.setScreenTemplate("Error.vm");
            return; 
        }
    }
    
        
}
