/* 
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 * 	
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

*/

package org.nrg.xnat.turbine.modules.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.pipeline.PipelineManager;
import org.nrg.pipeline.XnatPipelineLauncher;
import org.nrg.xdat.om.ArcPipelineparameterdataI;
import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.db.DBAction;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

public class PipelineActions extends SecureAction{
    static org.apache.log4j.Logger logger = Logger.getLogger(PipelineActions.class);

    public void doPerform(RunData data, Context context){
        data.setScreenTemplate("PipelineScreen.vm");
    }
    
    public void doSkip(RunData data, Context context) throws Exception {
        data.setScreenTemplate("PipelineScreen.vm");        
    }
    
    public void doLaunch(RunData data, Context context)  throws Exception {
            String project = data.getParameters().get("project");
            String step = data.getParameters().get("pipelineStep");
            boolean isDescendant = data.getParameters().getBoolean("isdescendant");
            ItemI data_item = TurbineUtils.GetItemBySearch(data);
            XnatPipelineLauncher xnatPipelineLauncher = getGenericCommonParameters(data,context, project, step, data_item);
            LinkedHashMap<ArcPipelineparameterdataI,ArrayList> paramHash = null;
            String launcherPrefix = data.getParameters().get("launcherPrefix");
            org.nrg.xft.search.CriteriaCollection cc = new CriteriaCollection("AND");
            cc.addClause("wrk:workflowData.ID",data_item.getProperty("ID"));
            cc.addClause("wrk:workflowData.data_type",data_item.getXSIType());
    
            if (isDescendant) {
                String pipelineXml = PipelineManager.getPathToPipelineForProject(project, step, data_item.getXSIType());
                String pipelineName = PipelineManager.getPipelineNameForProject(project, step, data_item.getXSIType());
                xnatPipelineLauncher.setPipelineName(pipelineXml);
                cc.addClause("wrk:workflowData.pipeline_name",pipelineName);
                paramHash = PipelineManager.getResolvedParametersForDescendantPipeline(step,project, data_item);
            }else {
                String pipelineXml = PipelineManager.getPathToPipelineForProject(project, step);
                String pipelineName = PipelineManager.getPipelineNameForProject(project, step);
                xnatPipelineLauncher.setPipelineName(pipelineXml);
                cc.addClause("wrk:workflowData.pipeline_name",pipelineName);
                paramHash = PipelineManager.getResolvedParametersForPipeline(step,project, data_item);
            }
            final String paramStr = "param:";
            if (paramHash != null) {
                Iterator paramIter = paramHash.keySet().iterator();
                while (paramIter.hasNext()) {
                    ArcPipelineparameterdataI aParameter = (ArcPipelineparameterdataI)paramIter.next();
                    String parameterTrueName = data.getParameters().get(paramStr + aParameter.getName() +":truename");
                    int valueCnt = paramHash.get(aParameter).size();
                    if (valueCnt > 1) {
                        for (int i=0; i <valueCnt;i++) {
                            String dataParam = paramStr + aParameter.getName() + ":"+i;
                            if (TurbineUtils.HasPassedParameter(dataParam, data)){
                               xnatPipelineLauncher.setParameter(parameterTrueName, data.getParameters().getString(dataParam));
                            }
                        }
                    }else {
                        String dataParam = paramStr + aParameter.getName();
                        if (TurbineUtils.HasPassedParameter(dataParam, data)){
                           xnatPipelineLauncher.setParameter(parameterTrueName, data.getParameters().getString(dataParam));
                        }
                    }
                }
            }
            //Remove all preexisting workflow entries for the given item and the pipeline
            ArrayList<WrkWorkflowdata> workflows = WrkWorkflowdata.getWrkWorkflowdatasByField(cc, TurbineUtils.getUser(data), false);
            if (workflows != null && workflows.size() > 0) {
            	WrkWorkflowdata workFlow = workflows.get(0);
            	if (workFlow.getStatus().equals(org.nrg.xdat.om.base.BaseWrkWorkflowdata.AWAITING_ACTION)) {
            		xnatPipelineLauncher.setStartAt(workFlow.getNextStepId());
            	}
                for (WrkWorkflowdata wrk : workflows){
                    DBAction.DeleteItem(wrk.getItem(),TurbineUtils.getUser(data));
                }
            }
            xnatPipelineLauncher.launch(launcherPrefix);
            data.setMessage("<p><b>The build process was successfully launched.  Status email will be sent upon its completion.</b></p>");
            data.setScreenTemplate("ClosePage.vm");
    }
    
    private XnatPipelineLauncher getGenericCommonParameters(RunData data, Context context, String projectId,  String step, ItemI item) throws Exception {
        XnatPipelineLauncher xnatPipelineLauncher = new XnatPipelineLauncher(data,context);
        xnatPipelineLauncher.setAdmin_email(AdminUtils.getAdminEmailId());
        xnatPipelineLauncher.setAlwaysEmailAdmin(ArcSpecManager.GetInstance().getEmailspecifications_pipeline());
        XDATUser user = TurbineUtils.getUser(data);
        xnatPipelineLauncher.setNeedsBuildDir(true);
        xnatPipelineLauncher.setSupressNotification(true);
        xnatPipelineLauncher.setId((String)item.getProperty("ID"));
        xnatPipelineLauncher.setDataType(item.getXSIType());
        xnatPipelineLauncher.setExternalId(projectId);
        xnatPipelineLauncher.setParameter("useremail", user.getEmail());
        xnatPipelineLauncher.setParameter("userfullname", XnatPipelineLauncher.getUserName(user));
        xnatPipelineLauncher.setParameter("adminemail", ArcSpecManager.GetInstance().getSiteAdminEmail());
        xnatPipelineLauncher.setParameter("xnatserver", ArcSpecManager.GetInstance().getSiteId());
        xnatPipelineLauncher.setParameter("mailhost", ArcSpecManager.GetInstance().getSmtpHost());

        String emailsStr =  data.getParameters().get("emailField");
        if (emailsStr != null) {
            String[] emails = emailsStr.trim().split(",");
            for (int i = 0 ; i < emails.length; i++)
                if (emails[i] != null && !emails[i].equals("")) xnatPipelineLauncher.notify(emails[i]);
        }
        return xnatPipelineLauncher;
    }
    
    public void doBuild(RunData data, Context context) throws Exception{
        String projectId = data.getParameters().get("projectId");
        int totalSessionsToBuild = data.getParameters().getInt("param:control:total");
        String step = data.getParameters().get("step");
        data.getParameters().remove("step");
        try {
            int selectedCount = 0;
            for (int i = 1; i <= totalSessionsToBuild; i++) {
                String sessionParamCode = ":session" + i;
                if (TurbineUtils.HasPassedParameter("param" + sessionParamCode + ":sessionId", data)){
                    selectedCount++;
                }    
            }
            
            String pipelineXml = PipelineManager.getPathToPipelineForProject(projectId, step);
            int selectedCountLast = 0;
            for (int i = 1; i <= totalSessionsToBuild; i++) {
                XnatPipelineLauncher xnatPipelineLauncher = getCommonParameters(data,context, projectId, pipelineXml,step);
                if (xnatPipelineLauncher == null) throw new Exception("Unable to construct the Xnat Pipeline Launcher");
                String sessionParamCode = ":session" + i;
                if (TurbineUtils.HasPassedParameter("param" + sessionParamCode + ":sessionId", data)){
                    Hashtable<String,String> sessionParams = getParametersForKey(data,context,"param" + sessionParamCode + ":",sessionParamCode);
                    String sessionId = sessionParams.get("param:sessionid");
                    String xnat_sessionId = sessionParams.get("param:xnat_sessionid");
                    xnatPipelineLauncher.setId(sessionId);
                    if (sessionParams.keySet().size() > 0) {
                        xnatPipelineLauncher.setParameter("xnat_sessionId",xnat_sessionId);
                        setCommandLineArguments(data,sessionParams,projectId,step, xnatPipelineLauncher);
                    }
                    selectedCountLast++;
                    if (selectedCountLast==selectedCount) xnatPipelineLauncher.setParameter("isLast","1");
                    xnatPipelineLauncher.setParameter("projectId",projectId);
                    xnatPipelineLauncher.launch("arc-qadd ");
                }
            }
            String destinationPage = data.getParameters().get("destinationpage");
            System.out.println("BuildPipelineActions::doBuild Destination page is " + destinationPage);
            System.out.println(data.getParameters().get("search_value"));
            System.out.println(data.getParameters().get("search_element"));
            System.out.println(data.getParameters().get("search_field"));
            System.out.println("BuildPipelineActions::doBuild END");

            if (destinationPage != null) {
                data.setRedirectURI(TurbineUtils.GetRelativeServerPath(data)+ "/app/template/" + destinationPage + "/search_field/" + data.getParameters().get("search_field") +  "/search_value/" +  data.getParameters().get("search_value")  + "/search_element/" +  data.getParameters().get("search_element"));
                //data.setScreenTemplate(destinationPage);
            }else {
                String msg = "<p><b>The build process was successfully launched.  Status email will be sent upon its completion.</b></p>";
                context.put("msg",msg);
                String breadCrumbs = "\"&nbsp;>&nbsp;<a href='/app/template/BrowseProjects.vm'>Studies</a>\"";
                context.put("breadcrumbLinks",breadCrumbs);
                data.setScreenTemplate("GenericMessage.vm");
            }
        } catch (Exception e){
        	logger.error("",e);
            data.setMessage("<p><img src=\"/fcon/images/error.gif\">The build process failed to launch. Please contact the <a href=\"mailto:" + AdminUtils.getAdminEmailId() + "?subject=Failed to launch build \">NRG techdesk</a>");
            data.setScreenTemplate("Error.vm");
        }
    }
    
    
    private XnatPipelineLauncher getCommonParameters(RunData data, Context context, String projectId, String pipelineName, String step) throws Exception {
        XnatPipelineLauncher xnatPipelineLauncher = new XnatPipelineLauncher(data,context);
        xnatPipelineLauncher.setAdmin_email(AdminUtils.getAdminEmailId());
        xnatPipelineLauncher.setAlwaysEmailAdmin(ArcSpecManager.GetInstance().getEmailspecifications_pipeline());
        xnatPipelineLauncher.setPipelineName(pipelineName);
        XDATUser user = TurbineUtils.getUser(data);
        xnatPipelineLauncher.setNeedsBuildDir(true);
        xnatPipelineLauncher.setExternalId(projectId);
        xnatPipelineLauncher.setSupressNotification(true);
        xnatPipelineLauncher.setDataType("xnat:mrSessionData");
        xnatPipelineLauncher.setParameter("useremail", user.getEmail());
        xnatPipelineLauncher.setParameter("userfullname", XnatPipelineLauncher.getUserName(user));
        xnatPipelineLauncher.setParameter("adminemail", AdminUtils.getAdminEmailId());
        xnatPipelineLauncher.setParameter("xnatserver", TurbineUtils.GetSystemName());
        xnatPipelineLauncher.setParameter("mailhost", AdminUtils.getMailServer());

        String emailsStr =  data.getParameters().get("emailField");
        if (emailsStr != null) {
            String[] emails = emailsStr.trim().split(",");
            for (int i = 0 ; i < emails.length; i++)
                if (emails[i] != null && !emails[i].equals("")) xnatPipelineLauncher.notify(emails[i]);
        }
        return xnatPipelineLauncher;
    }
    
    private Hashtable<String,String> setCommandLineArguments(RunData data, Hashtable<String,String> paramNameValue, String projectId, String step, XnatPipelineLauncher xnatPipelineLauncher) {
        Hashtable<String,String> trueNameValues = new Hashtable<String,String>(); 
        try {
            ArrayList parameters = PipelineManager.getParametersForPipeline(projectId, step);
            if (parameters != null) {
                LinkedHashMap<String,String> parametersHash = new LinkedHashMap<String,String>();
                //TrueName is required as the data parameter names are case insensitive
                Hashtable<String, String> trueName = new Hashtable<String, String>();
                ArrayList<String> paramKeys = new ArrayList<String>();
                Iterator iter = paramNameValue.keySet().iterator();
                while (iter.hasNext()) {
                    String dataKey = (String)iter.next();
                    if (dataKey.startsWith("param"))
                        paramKeys.add(dataKey);
                }
                Collections.sort(paramKeys, new Comparator<Object>() {
                    public int compare(Object o1, Object o2) {
                        int rtn = 0;
                        String[] o1_parts = ((String)o1).split(":");
                        String[] o2_parts = ((String)o2).split(":");
                        if (o1_parts != null && o1_parts.length > 1 && o2_parts != null && o2_parts.length > 1 ) {
                            if (o1_parts.length != o2_parts.length) {
                              return ((String)o1).compareTo((String)o2);
                            }
                            if (!o1_parts[1].equals(o2_parts[1])) {
                              return((String)o1).compareTo((String)o2);
                            }
                            if (o1_parts[1].equals(o2_parts[1]) && (o1_parts.length == 3 && o2_parts.length == 3)) {
                              Integer i1 =  Integer.parseInt(o1_parts[2]);
                              Integer i2 =  Integer.parseInt(o2_parts[2]);
                              return i1.compareTo(i2);
                            }
                            rtn = ((String)o1).compareTo((String)o2);
                        }
                        return rtn;
                    }
                }    
                );
                for (int i =0; i < paramKeys.size(); i++) {
                   String[] parts = ((String)paramKeys.get(i)).split(":");
                    if (parts != null && parts.length > 1 && parts[0].equals("param")) {
                        String paramName = parts[1];
                        if (!trueName.containsKey(paramName)) {
                            ArcPipelineparameterdataI aParameter = PipelineManager.getParameterByName(projectId, step, paramName,true);
                            if (aParameter != null) 
                                trueName.put(paramName,aParameter.getName());
                        }
                        if (parametersHash.containsKey(paramName)) {
                            parametersHash.put(paramName,((String)parametersHash.get(paramName))+"," + paramNameValue.get((String)paramKeys.get(i)) );     
                        }else {
                            parametersHash.put(paramName, paramNameValue.get((String)paramKeys.get(i)));
                        }
                    }
                }
                iter = parametersHash.keySet().iterator();
                while (iter.hasNext()) {
                    String paramName = (String)iter.next();
                    String trueParamName = (String)trueName.get(paramName);
                    String paramValues = (String)parametersHash.get(paramName);
                    if (paramValues.endsWith(",")) paramValues = paramValues.substring(0,paramValues.length()-1);
                    //System.out.println("Adding parameter values for " + trueParamName + " " + paramValues);
                    xnatPipelineLauncher.setParameter(trueParamName, paramValues);
                    trueNameValues.put(trueParamName,paramValues);
                }
            }
        }catch(Exception e) {
            logger.debug("Unable to construct the build parameters for step " + step + " " + e.getMessage() + " " + e.getCause(),e);
            //AdminUtils.sendErrorEmail(data,"Unable to construct the Build statement for "  + " Step " + step);
        }
        return trueNameValues;
    }

    private Hashtable<String,String> getParametersForKey(RunData data,Context context,String pattern, String replace) {
        Hashtable rtn = new Hashtable();
        Iterator keys = data.getParameters().keySet().iterator();
        while (keys.hasNext()) {
            String key = (String)keys.next();
            if (key.startsWith(pattern)) {
                if (replace != null)
                    rtn.put(org.apache.commons.lang.StringUtils.replace(key,replace,""),data.getParameters().get(key));
                else 
                    rtn.put(key,data.getParameters().get(key));
            }
        }
        return rtn;
    }

    
}
