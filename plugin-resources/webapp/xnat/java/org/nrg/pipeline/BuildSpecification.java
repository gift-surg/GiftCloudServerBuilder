
/*
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 *
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

*/

package org.nrg.pipeline;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.xml.transform.sax.SAXSource;

import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.sxpath.XPathEvaluator;
import net.sf.saxon.sxpath.XPathExpression;
import net.sf.saxon.trans.IndependentContext;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.nrg.pipeline.build.xmlbeans.BuildData;
import org.nrg.pipeline.build.xmlbeans.BuildDocument;
import org.nrg.pipeline.build.xmlbeans.ImageSessionData;
import org.nrg.pipeline.build.xmlbeans.PipelineData;
import org.nrg.pipeline.build.xmlbeans.PipelineParameterData;
import org.nrg.pipeline.build.xmlbeans.ImageSessionData.Pipeline;
import org.nrg.pipeline.build.xmlbeans.PipelineParameterData.Parameter;
import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemWrapper;
import org.nrg.xft.XFTItem;
import org.nrg.xft.meta.XFTMetaManager;
import org.nrg.xft.schema.Wrappers.XMLWrapper.XMLWrapperElement;
import org.nrg.xft.schema.Wrappers.XMLWrapper.XMLWrapperFactory;
import org.nrg.xft.search.CriteriaCollection;
import org.xml.sax.InputSource;

public class BuildSpecification {

    
    String specFilePath ;
    private BuildSpecification() {
        
    }
    
    
    
    
    public static BuildSpecification GetInstance() {
        if (self == null) {
            self = new BuildSpecification(); 
         }
         return self;
    }
    
    public  void init(String settingsDirectory) throws Exception {
        if (!settingsDirectory.endsWith(File.separator))
            settingsDirectory += File.separator;
        specFilePath = settingsDirectory + "BuildSpec.xml";
        load();
        logger.info("BuildSpec File Loaded");
    }

    private void load() throws Exception {
        File xmlFile = new File(specFilePath);
        try {
            buildDoc = (BuildDocument)XmlObject.Factory.parse(xmlFile);
            BuildDocument tmpBuildDoc = BuildDocument.Factory.newInstance();
            BuildData tmpBuild = tmpBuildDoc.addNewBuild();
            int nprojects = buildDoc.getBuild().sizeOfProjectArray();
            for (int i = 0; i < nprojects; i++) {
                if (buildDoc.getBuild().getProjectArray(i).getType().contains(",")) {
                    String[] types = buildDoc.getBuild().getProjectArray(i).getType().split(",");
                    for (int j = 0; j < types.length; j++) {
                       ImageSessionData tmpProject =  tmpBuild.addNewProject();
                       tmpProject.set(buildDoc.getBuild().getProjectArray(i));
                       tmpProject.setType(types[j].trim());
                    }
                }else {
                    tmpBuild.addNewProject().set(buildDoc.getBuild().getProjectArray(i));
                }
            }
            buildDoc = tmpBuildDoc;
        }catch(Exception e) {
            logger.error("Couldnt parse BuildSpec file " , e);
            throw new Exception("Invalid content found in " + specFilePath + " expecting a Build document");
        }

    }
    
    public void reload() throws Exception {
        load();
        logger.info("BuildSpec File re-loaded");
   }
    
    public  ArrayList getWorkFlowStatus(String id, String data_type, org.nrg.xft.security.UserI user) {
        org.nrg.xft.search.CriteriaCollection cc = new CriteriaCollection("AND");
        cc.addClause("wrk:workflowData.ID",id);
        cc.addClause("wrk:workflowData.data_type",data_type);
        return WrkWorkflowdata.getWrkWorkflowdatasByField(cc,user,false);
        
    }
    
    public ArrayList getWorkFlowsOrderByLaunchTimeDesc(String id, String dataType, org.nrg.xft.security.UserI user) {
        ArrayList workflows = new ArrayList();
        org.nrg.xft.search.CriteriaCollection cc = new CriteriaCollection("AND");
        cc.addClause("wrk:workflowData.ID",id);
        cc.addClause("wrk:workflowData.data_type",dataType);
        //Sort by Launch Time
        try {
            org.nrg.xft.collections.ItemCollection items = org.nrg.xft.search.ItemSearch.GetItems(cc,user,false);
            ArrayList workitems = items.getItems("wrk:workflowData.launch_time","DESC");
            Iterator iter = workitems.iterator();
            while (iter.hasNext())
            {
                WrkWorkflowdata vrc = new WrkWorkflowdata((XFTItem)iter.next());
                workflows.add(vrc);
            }
        }catch(Exception e) {
            logger.debug("",e);
        }
       logger.info("Workflows by Ordered by Launch Time " + workflows.size());
        return workflows;
    }

    public String getWorkFlowStatus(String id, String data_type, String pipelinePath, org.nrg.xft.security.UserI user) {
        String rtn = null;
        org.nrg.xft.search.CriteriaCollection cc = new CriteriaCollection("AND");
        cc.addClause("wrk:workflowData.ID",id);
        cc.addClause("wrk:workflowData.data_type",data_type);
        cc.addClause("wrk:workflowData.pipeline_name",pipelinePath);
        ArrayList rtns = WrkWorkflowdata.getWrkWorkflowdatasByField(cc,user,false,"wrk:workflowData.launch_time","desc");
        if (rtns != null && rtns.size() > 0) {
                rtn = ((WrkWorkflowdata)rtns.get(0)).getStatus();
        }
        return rtn;
    }
    
    public ImageSessionData getProjectElement(String projectType) {
        ImageSessionData rtn = null;
        ImageSessionData projects[] = buildDoc.getBuild().getProjectArray();
        for (int i = 0; i < projects.length; i++) {
            if (projects[i].getType().equals("^ALL^")) {
                rtn = projects[i];
            }
            if (projects[i].getType().equals(projectType)) {
                rtn = projects[i];
                break;
            }
        }
        return rtn;
    }
    
    public boolean hasSpecificProjectElement(String projectType) {
        boolean rtn = false;
        ImageSessionData projects[] = buildDoc.getBuild().getProjectArray();
        for (int i = 0; i < projects.length; i++) {
            if (projects[i].getType().equals(projectType)) {
                rtn = true;
                break;
            }
        }
        return rtn;
    }
    
    public Pipeline[] getPipelinesForProject(String projectType) {
        ImageSessionData project = getProjectElement(projectType);
        return project.getPipelineArray();
    }
    
    public Pipeline getPipelineForProject(String projectType, String stepId) {
        Pipeline rtn = null;
        Pipeline[] pipelines = getPipelinesForProject(projectType);
        for (int i = 0; i < pipelines.length; i++) {
            if (pipelines[i].getStepId().equals(stepId)) {
                rtn = pipelines[i];
                break;
            }
        }
        return rtn;
    }

    public Pipeline getPipelineForProject(String projectType, int stepNo) {
        Pipeline rtn = null;
        Pipeline[] pipelines = getPipelinesForProject(projectType);
        if (stepNo > pipelines.length) {
            return rtn;
        }
        rtn = pipelines[stepNo];
        return rtn;
    }
    
    public String getPathToPipelineForProject(String projectType, String stepId) {
        String rtn = "";
        Pipeline pipeline = getPipelineForProject(projectType, stepId);
        if (pipeline != null) {
            if (pipeline.getLocation().endsWith(File.separator)) {
                pipeline.setLocation(pipeline.getLocation().substring(0,pipeline.getLocation().length()-1));
            }
            rtn = pipeline.getLocation() + File.separator + pipeline.getName();
        }
        if (!rtn.endsWith(".xml")) rtn += ".xml";
        return rtn;
    }
    

    private  ArrayList getProjectElementForProjectType(String projectType) throws Exception{
        return getProjectElementForProjectType(projectType, false);
    }
    
    private  ArrayList getProjectElementForProjectType(String sessionType, boolean exact) throws Exception{
        ArrayList imageSessions = new ArrayList();
        for (int i = 0; i < buildDoc.getBuild().getProjectArray().length; i++) {
            //System.out.println("Scanning type " + buildDoc.getBuild().getProjectArray(i).getType());
            if (buildDoc.getBuild().getProjectArray(i).getType().equals(sessionType)) {
                imageSessions.add(buildDoc.getBuild().getProjectArray(i));
                if (exact) return imageSessions;
            }
            if (!exact) {
                if (buildDoc.getBuild().getProjectArray(i).getType().equals("^ALL^") ) {
                    imageSessions.add(buildDoc.getBuild().getProjectArray(i));
                }
            }
        }
        return imageSessions;
    }

    public  LinkedHashMap getResolvedParametersForPipeline(String stepId, String projectType, ItemWrapper item) throws Exception {
        LinkedHashMap parametersHash = new LinkedHashMap();
        PipelineParameterData parameters = getParametersForPipeline(projectType,stepId);
        if (parameters == null) {
            return parametersHash;
        }
        for (int i = 0; i < parameters.sizeOfParameterArray(); i++) {
            Parameter aParameter = parameters.getParameterArray(i);
            if (aParameter.isSetCsvValues()) {
                String csvValues = aParameter.getCsvValues().trim();
                ArrayList<String> values = new ArrayList<String>(Arrays.asList(csvValues.split(",")));
                parametersHash.put(aParameter,values);
            }else if (aParameter.isSetSchemaLink()){
                // ArrayList values = this.resolveXPath(aParameter.getSchemaLink(), item);
                // if (values != null) parametersHash.put(aParameter,values);
                //Saxon seems to have problem evaluating recursively. 
                // SXXP0003 Premature end of file
            }else {
                ArrayList<String> values = new ArrayList(); values.add("");
                parametersHash.put(aParameter,values);
            }
        }
        return parametersHash;
    }

    public  PipelineParameterData getParametersForPipeline(String projectType, String stepId) throws Exception{
        PipelineParameterData rtn = null;
        ArrayList projectElements = getProjectElementForProjectType(projectType);
        if (projectElements == null)  return rtn;
        ImageSessionData project = (ImageSessionData) projectElements.get(0);
        if (project == null)  return rtn;
        rtn = getParametersForPipeline(project,stepId);
        return rtn;
    } 

    private  PipelineParameterData getParametersForPipeline(ImageSessionData project,String stepId) {
        PipelineParameterData rtn = null;
        for (int i = 0; i < project.getPipelineArray().length; i++) {
            if (project.getPipelineArray(i).getStepId().equals(stepId)) {
                rtn = project.getPipelineArray(i).getParameters();
                break;
            }
        }
        return rtn;
    }

/*    private  PipelineParameterData getParametersForPipeline(ImageSessionData imageSession,String pipelineName) {
        PipelineParameterData rtn = null;
        for (int i = 0; i < imageSession.getPipelineArray().length; i++) {
            if (imageSession.getPipelineArray(i).getName().equals(pipelineName)) {
                rtn = imageSession.getPipelineArray(i).getParameters();
                break;
            }
        }
        return rtn;
    }
*/
    
    public  String getPipelinePath(String projectType, String stepId) throws Exception{
        String rtn = null;
        ArrayList projects = getProjectElementForProjectType(projectType, true);
        if (projects == null || projects.size() <1) {
            return null;
        }
        ImageSessionData project = (ImageSessionData)projects.get(0);
        if (project != null) {
            for (int i = 0; i < project.getPipelineArray().length; i++) {
                if (project.getPipelineArray(i).getStepId().equals(stepId)) {
                    rtn = project.getPipelineArray(i).getLocation();
                    break;
                }
            }
        }
        if (rtn == null) throw new Exception("Couldnt find pipeline with Step Id " + stepId + " associated with " + projectType);
        return rtn;
    }

    
    
    
    public LinkedHashMap getBatchParametersForPipeline(String projectType, String stepId) {
            LinkedHashMap rtn = new LinkedHashMap();
            try {
                PipelineParameterData params = this.getParametersForPipeline(projectType, stepId);
                if (params != null) {
                    for (int i = 0; i < params.sizeOfParameterArray(); i++) {
                        if (params.getParameterArray(i).isSetBatchParam()) {
                            Parameter batchParam = params.getParameterArray(i);
                            if (batchParam.isSetCsvValues()) {
                                String csvValues = batchParam.getCsvValues().trim();
                                ArrayList<String> values = new ArrayList<String>(Arrays.asList(csvValues.split(",")));
                                rtn.put(batchParam,values);
                            }else {
                                ArrayList<String> values = new ArrayList(); values.add("");
                                rtn.put(batchParam,values);
                            }
                        }
                    }
                }
            }catch(Exception e){}
            return rtn;
    }
    
    public LinkedHashMap getExactBatchParametersForPipeline(String projectType, String stepId) {
        LinkedHashMap rtn = new LinkedHashMap();
        try {
            ArrayList projectElements = getProjectElementForProjectType(projectType, true);
            if (projectElements == null || projectElements.size() == 0)  return rtn;
            ImageSessionData project = (ImageSessionData) projectElements.get(0);
            if (project == null)  return rtn;
            PipelineParameterData params  = getParametersForPipeline(project,stepId);
            if (params != null) {
                for (int i = 0; i < params.sizeOfParameterArray(); i++) {
                    if (params.getParameterArray(i).isSetBatchParam()) {
                        Parameter batchParam = params.getParameterArray(i);
                        if (batchParam.isSetCsvValues()) {
                            String csvValues = batchParam.getCsvValues().trim();
                            ArrayList<String> values = new ArrayList<String>(Arrays.asList(csvValues.split(",")));
                            rtn.put(batchParam,values);
                        }else {
                            ArrayList<String> values = new ArrayList(); values.add("");
                            rtn.put(batchParam,values);
                        }
                    }
                }
            }
        }catch(Exception e){e.printStackTrace();}
        return rtn;
    }
    
    public LinkedHashMap getBestGuessBatchParametersForPipeline(String projectType, String stepId) {
        LinkedHashMap rtn = new LinkedHashMap();
        rtn = getExactBatchParametersForPipeline(projectType, stepId);
        if (rtn == null || rtn.size()==0) {
            rtn = getBatchParametersForPipeline(projectType, stepId);
        }
        return rtn;
    }    
    
    public boolean saveBatchParametersForProject(Hashtable<String,String> commonParams,String projectId,String step) {
        boolean rtn = false;
        boolean hasOwnSpecification = hasSpecificProjectElement(projectId);
        ImageSessionData projectBuildSpec = null;
        if (!hasOwnSpecification) {
            ImageSessionData projectImageSessionData = getProjectElement(projectId);
            projectBuildSpec =  buildDoc.getBuild().addNewProject();
            projectBuildSpec.set(projectImageSessionData);
            projectBuildSpec.setType(projectId);
        }else {
            try {
                projectBuildSpec = (ImageSessionData)(getProjectElementForProjectType(projectId).get(0));
            }catch(Exception e) {
                logger.debug("Unable to get specific project element for project " + projectId + " " + e.getMessage() );
                return rtn;
            }
        }
        PipelineParameterData pipelineParameters = getParametersForPipeline(projectBuildSpec, step);
        if (commonParams != null && commonParams.size() > 0 ) {
            if (pipelineParameters != null) {
                for (int i = 0; i < pipelineParameters.sizeOfParameterArray(); i++) {
                    if (pipelineParameters.getParameterArray(i).isSetBatchParam()) {
                        Parameter batchParam = pipelineParameters.getParameterArray(i);
                        if (commonParams.containsKey(batchParam.getName())) {
                            if (batchParam.isSetCsvValues()) {
                                String csvValues = commonParams.get(batchParam.getName());
                                batchParam.setCsvValues(csvValues);
                            }
                        }
                    }
                }
            }
        }
        save();
        rtn = true;
        return rtn;
    }
    
    private void save() {
        try {
            buildDoc.save(new File(specFilePath),new XmlOptions().setSavePrettyPrint().setSaveAggressiveNamespaces());
        }catch(Exception e) {
            logger.debug(e);
        }
    }
    
    public  LinkedHashMap getPipelinesForProject(String projectID,  org.nrg.xft.security.UserI user) throws Exception {
        LinkedHashMap pipelines = new LinkedHashMap();
        ArrayList imageSessions = getProjectElementForProjectType(projectID);
        if (imageSessions == null || imageSessions.size()==0) return null;
        for (int j = 0; j < imageSessions.size(); j++) {
            ImageSessionData imageSession = (ImageSessionData) imageSessions.get(j);
            for (int i = 0; i < imageSession.getPipelineArray().length; i++) {
                PipelineData pipeline = imageSession.getPipelineArray(i);
                String pipelinePath = pipeline.getLocation();
                if (!pipelinePath.endsWith(File.separator))
                        pipelinePath += File.separator;
                pipelinePath += pipeline.getName();
                if (!pipeline.getName().endsWith(".xml")) pipelinePath += ".xml";
                pipelines.put(imageSession.getPipelineArray(i), imageSession.getType());
            }
        }
        return pipelines;
        
    }
    
    /*public  LinkedHashMap getPipelinesForSessionType(String sessionType, String data_type, String id,  org.nrg.xft.security.UserI user, boolean checkDependency) throws Exception {
        String transferStatus = getWorkFlowStatus(id,data_type,"Transfer",user);
        if (transferStatus == null || !transferStatus.equals("Complete"))
            return new LinkedHashMap();
        LinkedHashMap linkedHash = new LinkedHashMap();
        ArrayList imageSessions = getImageSessionForSessionType(sessionType);
        if (imageSessions == null) return null;
        for (int j = 0; j < imageSessions.size(); j++) {
            ImageSessionData imageSession = (ImageSessionData) imageSessions.get(j);
            boolean previousStepComplete = true;
            boolean disabled = true;
            for (int i = 0; i < imageSession.getPipelineArray().length; i++) {
                disabled = true;
                PipelineData pipeline = imageSession.getPipelineArray(i);
                if (previousStepComplete) disabled = false;
                String pipelinePath = pipeline.getLocation();
                if (!pipelinePath.endsWith(File.separator))
                        pipelinePath += File.separator;
                pipelinePath += pipeline.getName();
                if (!pipeline.getName().endsWith(".xml")) pipelinePath += ".xml";
                System.out.println("Looking for Path " + pipelinePath);
                String workFlowStatus = getWorkFlowStatus(id,data_type,pipelinePath,user);
                if (workFlowStatus == null) workFlowStatus ="";
                if (checkDependency) {
                   if (pipeline.isSetIndependent() && pipeline.getIndependent()) {
                       disabled = false;
                   }
                }
                linkedHash.put(imageSession.getPipelineArray(i),new BuildStatus(workFlowStatus,disabled));
                previousStepComplete = workFlowStatus.equals("Complete");
            }
        }
        return linkedHash;
    }*/



    public  ArrayList resolveXPath(String xpathStmt, ItemWrapper itemW) throws Exception {
        ArrayList rtn = new ArrayList();

        XPathEvaluator xpe = new XPathEvaluator();
        XMLWrapperElement element = (XMLWrapperElement)XFTMetaManager.GetWrappedElementByName(XMLWrapperFactory.GetInstance(),itemW.getXSIType());

        if (element.getSchemaTargetNamespacePrefix() != null && element.getSchemaTargetNamespacePrefix().equals("") && element.getSchemaTargetNamespaceURI() != null) {
            ((IndependentContext)xpe.getStaticContext()).declareNamespace("", element.getSchemaTargetNamespaceURI());
        }else if (element.getSchemaTargetNamespacePrefix() != null && !element.getSchemaTargetNamespacePrefix().equals("") && element.getSchemaTargetNamespaceURI() != null) {
            ((IndependentContext)xpe.getStaticContext()).declareNamespace(element.getSchemaTargetNamespacePrefix(), element.getSchemaTargetNamespaceURI());
        }

        SAXSource ss = new SAXSource(new  InputSource(new ByteArrayInputStream(itemW.toXML_BOS(TurbineUtils.GetFullServerPath() + "/schemas").toByteArray())));
        XPathExpression xExpr =  xpe.createExpression(xpathStmt);
        SequenceIterator rtns = xExpr.rawIterator(ss);
        Item xpathObj = rtns.next();
        while (xpathObj != null) {
            xpathObj = rtns.current();
            rtn.add(xpathObj.getStringValue());
            xpathObj = rtns.next();
        }
        return rtn;
    }
    
    public ArrayList resolveXPath(String xpathStmt, SAXSource ss,XMLWrapperElement element ) throws Exception {
        ArrayList rtn = new ArrayList();

        XPathEvaluator xpe = new XPathEvaluator();
        if (element.getSchemaTargetNamespacePrefix() != null && element.getSchemaTargetNamespacePrefix().equals("") && element.getSchemaTargetNamespaceURI() != null) {
            ((IndependentContext)xpe.getStaticContext()).declareNamespace("", element.getSchemaTargetNamespaceURI());
        }else if (element.getSchemaTargetNamespacePrefix() != null && !element.getSchemaTargetNamespacePrefix().equals("") && element.getSchemaTargetNamespaceURI() != null) {
            ((IndependentContext)xpe.getStaticContext()).declareNamespace(element.getSchemaTargetNamespacePrefix(), element.getSchemaTargetNamespaceURI());
        }
        XPathExpression xExpr =  xpe.createExpression(xpathStmt);
        SequenceIterator rtns = xExpr.rawIterator(ss);
        Item xpathObj = rtns.next();
        while (xpathObj != null) {
            xpathObj = rtns.current();
            rtn.add(xpathObj.getStringValue());
            xpathObj = rtns.next();
        }
        return rtn;
    }


    public  Parameter getParameterByName(LinkedHashMap parametersHash, String name, boolean ignorecase) {
        Iterator paramIter = parametersHash.keySet().iterator();
        Parameter rtn = null;
        while (paramIter.hasNext()) {
            Parameter param = (Parameter)paramIter.next();
            if ((ignorecase?param.getName().equalsIgnoreCase(name):param.getName().equals(name))) {
                rtn = param;
                break;
            }
        }
        return rtn;
    }

    public  Parameter getParameterByName(String projectType, String step,String nameToMatch, boolean ignoreCase) throws Exception{
        PipelineParameterData  params = getParametersForPipeline(projectType,step);
        Parameter rtn = null;
        if (params == null) return null;
        for (int i = 0; i < params.sizeOfParameterArray(); i++) {
            if ((ignoreCase?params.getParameterArray(i).getName().equalsIgnoreCase(nameToMatch):params.getParameterArray(i).getName().equals(nameToMatch))) {
                rtn = params.getParameterArray(i);
                break;
            }
        }
        return rtn;
    } 

   
    public static void main(String args[]) {
        try {
            BuildSpecification.GetInstance().init("C:\\eclipse\\workspace\\xdat_release\\projects\\cnda_xnat");
            System.out.println("All done");
            String firstStepPipelinePath =  BuildSpecification.GetInstance().getPathToPipelineForProject("Pilot PIB (NP633)","1");
            System.out.println("First Step Pipeline Path " + firstStepPipelinePath);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    private static BuildSpecification self;
    private  BuildDocument buildDoc;
    static org.apache.log4j.Logger logger = Logger.getLogger(BuildSpecification.class);

}
