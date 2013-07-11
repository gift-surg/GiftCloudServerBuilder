/*
 * org.nrg.pipeline.PipelineRepositoryManager
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
*/

package org.nrg.pipeline;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.nrg.pipeline.xmlbeans.PipelineData;
import org.nrg.pipeline.xmlbeans.PipelineData.Documentation;
import org.nrg.pipeline.xmlbeans.PipelineData.Documentation.InputParameters;
import org.nrg.pipeline.xmlbeans.PipelineData.Documentation.InputParameters.Parameter;
import org.nrg.pipeline.xmlbeans.PipelineData.XnatInfo.GeneratesElements;
import org.nrg.pipeline.xmlbeans.PipelineDocument;
import org.nrg.xdat.model.*;
import org.nrg.xdat.om.*;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.EventUtils.CATEGORY;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.event.persist.PersistentWorkflowUtils.EventRequirementAbsent;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PipelineRepositoryManager {

	public static final String ALL_DATA_TYPES="All Datatypes";
	private  static PipePipelinerepository pipelineRepository = null;
	private static Logger logger = Logger.getLogger(PipelineRepositoryManager.class);

	public synchronized static PipePipelinerepository GetInstance() {
		if (pipelineRepository == null) {
            System.out.print("Initializing PipelineRepository...");
            ArrayList<PipePipelinerepository> pipelineRepos = PipePipelinerepository.getAllPipePipelinerepositorys(null,true);
            if (pipelineRepos.size()>0) {
            	pipelineRepository = pipelineRepos.get(0);
            }else { //No pipelines have been set for the site so far.
            	UserI user = null;
            	pipelineRepository = new PipePipelinerepository(user);
            }
		}
		if (pipelineRepository != null) {
			System.out.println("Finished loading the pipeline repository!");
		}
		return pipelineRepository;
	}

	public synchronized static void SetInfo(PipePipelinedetails pipelineDetails) throws Exception{
		String pipelineXml = pipelineDetails.getPath();
		File xmlFile = new File(pipelineXml);
		if (pipelineXml == null) throw new Exception("Path to the pipeline not set");
		if (!xmlFile.exists() ) throw new Exception("File at " + pipelineXml + " not found");
			   ArrayList errors = new ArrayList();
		        XmlOptions xopt = new XmlOptions();
		        xopt.setErrorListener(errors);
		         XmlObject xo = XmlObject.Factory.parse(xmlFile, xopt);
		         if (errors.size() != 0) {
		             throw new XmlException(errors.toArray().toString());
		         }
		         PipelineDocument pipelineDoc = (PipelineDocument)xo;
		         PipelineData pipeline = pipelineDoc.getPipeline();
		         pipelineDetails.setDescription(pipeline.getDescription());
		         boolean hasXnatInfo = pipeline.isSetXnatInfo();
		         if (hasXnatInfo) {
		         String appliesTo = pipeline.getXnatInfo().getAppliesTo();
		         if (appliesTo != null) {
		        	 pipelineDetails.setAppliesto(appliesTo);
		         }else {
		        	 pipelineDetails.setAppliesto(ALL_DATA_TYPES);
		         }
		         if (pipeline.getXnatInfo().isSetGeneratesElements()) {
		        	 GeneratesElements elementsGenerated = pipeline.getXnatInfo().getGeneratesElements();
		        	 for (int i = 0; i < elementsGenerated.sizeOfElementArray(); i++) {
		        		 String elementGenerated = elementsGenerated.getElementArray(i);
		        		 PipePipelinedetailsElement element = new PipePipelinedetailsElement();
		        		 element.setElement(elementGenerated);
		        		 pipelineDetails.setGenerateselements_element(element);
		        	 }
		         }
		         }else {
		        	 pipelineDetails.setAppliesto(ALL_DATA_TYPES);
		         }
		         if (pipeline.isSetDocumentation()) { //Extract the input parameters for the pipeline
		        	 Documentation  documentation = pipeline.getDocumentation();
		        	 if (documentation.isSetInputParameters()) {
		        		 InputParameters inputParameters = documentation.getInputParameters();
		        		 for (int i=0; i < inputParameters.sizeOfParameterArray(); i++) {
		        			 Parameter param = inputParameters.getParameterArray(i);
		        			 PipePipelinedetailsParameter pipeParam = new PipePipelinedetailsParameter();
		        			 pipeParam.setName(param.getName());
		        			 pipeParam.setDescription(param.getDescription());
		        			 if (param.isSetValues()) {
		        				 if (param.getValues().isSetSchemalink()) {
		        					 pipeParam.setValues_schemalink(param.getValues().getSchemalink());
		        				 }else if (param.getValues().isSetCsv()) {
		        					 pipeParam.setValues_csvvalues(param.getValues().getCsv());
		        				 }
		        			 }else {
		        				 pipeParam.setValues_csvvalues("");
		        			 }
		        			 pipelineDetails.setParameters_parameter(pipeParam);
		        		 }
		        	 }
		         }
				pipeline =null;
				pipelineDoc = null;
	}

	   public synchronized static  void Reset(){
		   pipelineRepository=null;
	    }


	 public synchronized static void RemoveReferenceToPipelineFromProject(PipePipelinedetails pipeline, UserI user, String pID, final EventUtils.TYPE type, String reason) {
		 //Check to see if the project has some study protocol data corresponding to the pipeline
		 //If it does do nothing
		 //If no data has been generated so far remove the reference to the study protocol generated by the pipeline
		 XnatProjectdata proj = XnatProjectdata.getXnatProjectdatasById(pID, user, false);
		 
		 PersistentWorkflowI wrk;
		try {
			wrk = PersistentWorkflowUtils.buildOpenWorkflow((XDATUser)user, proj.SCHEMA_ELEMENT_NAME,proj.getId(),proj.getId(),EventUtils.newEventInstance(CATEGORY.PROJECT_ADMIN,type,EventUtils.MODIFY_CONFIGURED_PIPELINE,reason,null));
		} catch (EventRequirementAbsent e1) {
			throw new NullPointerException(e1.getMessage());
		}
		 EventMetaI c = wrk.buildEvent();
	    	
		 List<PipePipelinedetailsElementI> elementsGenerated =  pipeline.getGenerateselements_element();
		 if (proj != null) {
			 ArcProject arcProject = ArcSpecManager.GetFreshInstance().getProjectArc(proj.getId());
			 removePipeline(pipeline.getPath(), arcProject, user,c);
			 /*for (int i = 0; i < elementsGenerated.size(); i++) {
				 String xsiType = elementsGenerated.get(i).getElement();
				 ArrayList experiments = proj.getExperimentsByXSIType(xsiType);
				 if (experiments == null || experiments.size() == 0) {
					 //No data has been generated of this datatype so remove from the study protocl list
					 List<XnatAbstractprotocolI> studyProtocols =  proj.getStudyprotocol();
					 if (studyProtocols.size() > 0 ) {
						 for (int j = 0; j < studyProtocols.size(); j++) {
							 XnatAbstractprotocol protocol = (XnatAbstractprotocol) studyProtocols.get(j);
							 if (protocol.getDataType().equals(xsiType)) {
								 try {
									 DBAction.DeleteItem(protocol.getCurrentDBVersion(), user,c);
									 SaveItemHelper.authorizedDelete(protocol.getCurrentDBVersion(), user);
									 WorkflowUtils.complete(wrk, c);
								 }catch(Exception e) {
									 logger.error("Couldnt delete pipelines located at " + pipeline.getPath() + " for project " + pID );
								 }
							 }
						 }
					 }
				 }
			 }*/
		 }
	 }

	 private static void removePipeline(String pipelinePath, ArcProject aProject, UserI user, EventMetaI c) {
		 List<ArcProjectPipelineI> projectPipelines = aProject.getPipelines_pipeline();
		 boolean deleted = false;
		 for (int j = 0; j < projectPipelines.size(); j++) {
			 ArcProjectPipeline projectPipeline = (ArcProjectPipeline)projectPipelines.get(j);
			 ArcPipelinedata pipeline = (ArcPipelinedata) projectPipeline.getPipelinedata();
			 String path = pipeline.getLocation() ;
			 if (pipelinePath.equals(path)) {
				 try {
                     SaveItemHelper.authorizedRemoveChild(projectPipeline.getCurrentDBVersion(),null,pipeline.getCurrentDBVersion(),user,c);
                     projectPipeline.getItem().removeItem(pipeline.getItem());
					 //DBAction.DeleteItem(pipeline.getItem(), user);
					 deleted = true;
					 break;
				 }catch(Exception e) {
					 logger.error("Couldnt delete pipelines located at " + pipelinePath + " for project " + aProject.getId() );
				 }
			 }
		 }
		 /*if (deleted) {
	       try {
			 ValidationResults vr = aProject.getCurrentDBVersion().validate();
	         if (vr.isValid()){
	                 aProject.getCurrentDBVersion().save(user,false,false);
	         }
           }catch(Exception e) {
          	 logger.error("",e);
           }
		 }*/

		 List<ArcProjectDescendantI> projectDescs = aProject.getPipelines_descendants_descendant();
		 for (int j = 0; j < projectDescs.size(); j++) {
			 ArcProjectDescendant projectDesc = (ArcProjectDescendant)projectDescs.get(j);
			 List<ArcProjectDescendantPipelineI> pipelines = projectDesc.getPipeline();
			 for (int k = 0; k < pipelines.size(); k++) {
				 ArcProjectDescendantPipeline descPipeline = (ArcProjectDescendantPipeline)pipelines.get(k);
				 ArcPipelinedata pipeline = (ArcPipelinedata) descPipeline.getPipelinedata();
				 String path = pipeline.getLocation() ;
				 if (pipelinePath.equals(path)) {
					 try {
	                     SaveItemHelper.authorizedRemoveChild(projectDesc.getCurrentDBVersion(),null,descPipeline.getCurrentDBVersion(),user,c);
	                     projectDesc.getItem().removeItem(descPipeline.getItem());
	                     
						 deleted = true;
						 //DBAction.DeleteItem(pipeline.getCurrentDBVersion(), user);
						 break;
					 }catch(Exception e) {
						 logger.error("Couldnt delete pipelines located at " + pipelinePath + " for project " + aProject.getId() + " descendant  " + projectDesc.getXsitype() );
					 }
					 }
			 }
		 }
		 /*if (deleted) {
		       try {
				 ValidationResults vr = aProject.getCurrentDBVersion().validate();
		         if (vr.isValid()){
		                 aProject.getCurrentDBVersion().save(user,false,false);
		         }
	           }catch(Exception e) {
	          	 logger.error("",e);
	           }
			 }*/

		 ArcSpecManager.Reset();
	 }

	 public synchronized static void RemoveReferenceToPipelineFromProjects(String pipelinePath, UserI user, EventMetaI c) {
		 ArcArchivespecification arcSpec = ArcSpecManager.GetFreshInstance();
		 List<ArcProjectI> arcProjects = arcSpec.getProjects_project();
		 for (int i = 0; i < arcProjects.size(); i++) {
			 ArcProject aProject = (ArcProject) arcProjects.get(i);
			 removePipeline(pipelinePath,  aProject,  user,c);
		 }
	 }

	 public static String GetPathToPipeline(String partialPipelinePath, ArcProject aProject) {
		 String rtn = null;
		 List<ArcProjectPipelineI> projectPipelines = aProject.getPipelines_pipeline();
		 for (int j = 0; j < projectPipelines.size(); j++) {
			 ArcProjectPipeline projectPipeline = (ArcProjectPipeline)projectPipelines.get(j);
			 ArcPipelinedata pipeline = (ArcPipelinedata) projectPipeline.getPipelinedata();
			 String path = pipeline.getLocation() ;
			 if (path.endsWith(partialPipelinePath)) {
			  rtn = path;
			  break;
			 }
		 }
		 if (rtn == null) {
			 List<ArcProjectDescendantI> projectDescs = aProject.getPipelines_descendants_descendant();
			 for (int j = 0; j < projectDescs.size(); j++) {
				 ArcProjectDescendant projectDesc = (ArcProjectDescendant) projectDescs.get(j);
				 List<ArcProjectDescendantPipelineI> pipelines = projectDesc.getPipeline();
				 for (int k = 0; k < pipelines.size(); k++) {
					 ArcProjectDescendantPipeline descPipeline = (ArcProjectDescendantPipeline) pipelines.get(k);
					 ArcPipelinedata pipeline = (ArcPipelinedata) descPipeline.getPipelinedata();
					 String path = pipeline.getLocation() ;
					 if (path.endsWith(partialPipelinePath)) {
						 rtn = path;
						 break;
					 }
		        }
				if (rtn != null) break;
	       }
		 }
	 return rtn;
	 }

}
