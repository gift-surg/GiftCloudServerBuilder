//Copyright 2005 Harvard University / Howard Hughes Medical Institute (HHMI) All Rights Reserved
package org.nrg.xnat.turbine.modules.actions;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.nrg.pipeline.BuildSpecification;
import org.nrg.pipeline.XnatPipelineLauncher;
import org.nrg.pipeline.build.xmlbeans.PipelineParameterData;
import org.nrg.pipeline.build.xmlbeans.PipelineParameterData.Parameter;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.turbine.utils.ArcSpecManager;


/**
 * This class provides a simple set of methods to
 * insert/update/delete records in a database.
 */
public class BuildPipelineActions extends SecureAction
{
	static org.apache.log4j.Logger logger = Logger.getLogger(BuildPipelineActions.class);

   public void doPerform(RunData data, Context context){
       String pipeline = data.getParameters().get("pipeline");
       String reload = data.getParameters().get("reloadbuildspec");
       if (pipeline != null) {
           String rootName = pipeline;
           if (pipeline.endsWith(".xml"))
               rootName = pipeline.substring(0,pipeline.length()-4);
           String templateName = "BuildScreen_"  + rootName + ".vm";
           System.out.println("Looking for template " + "/screens/" + templateName);
           if (Velocity.templateExists("/screens/"+templateName)) 
               data.setScreenTemplate(templateName);
       }else if (reload != null) {
           doReload(data,context);
       }else {
           data.setScreenTemplate("BuildPipelineParameters.vm");
       }
   }
   
   public void doRebuild(RunData data, Context context){
       context.put("rebuild","true");
       doPerform(data,context);
   }
   
   
   public void doReload(RunData data, Context context){
       try {
           BuildSpecification.GetInstance().reload();
       }catch(Exception e) {
           logger.debug("Unable to load the BuildSpec document ", e);
       }
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
           
           String pipelineXml = BuildSpecification.GetInstance().getPathToPipelineForProject(projectId, step);
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
           logger.error(e);
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
       Hashtable<String,String> commonParams = getParametersForKey(data,context,"param:common:",":common");
       if (commonParams.keySet().size() > 0) {
           Hashtable<String,String> trueNameValues = setCommandLineArguments(data,commonParams,projectId,step, xnatPipelineLauncher);
           BuildSpecification.GetInstance().saveBatchParametersForProject(trueNameValues, projectId,step);
       }
       return xnatPipelineLauncher;
   }
   
   private Hashtable<String,String> setCommandLineArguments(RunData data, Hashtable<String,String> paramNameValue, String projectType, String step, XnatPipelineLauncher xnatPipelineLauncher) {
       Hashtable<String,String> trueNameValues = new Hashtable<String,String>(); 
       try {
           PipelineParameterData parameters = BuildSpecification.GetInstance().getParametersForPipeline(projectType, step);
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
                           Parameter aParameter = BuildSpecification.GetInstance().getParameterByName(projectType, step, paramName,true);
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
