/*
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 *
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

*/

package org.nrg.pipeline;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
import org.nrg.xdat.model.ArcProjectDescendantI;
import org.nrg.xdat.model.ArcProjectDescendantPipelineI;
import org.nrg.xdat.model.ArcProjectI;
import org.nrg.xdat.model.ArcProjectPipelineI;
import org.nrg.xdat.model.PipePipelinedetailsElementI;
import org.nrg.xdat.model.XnatAbstractprotocolI;
import org.nrg.xdat.om.ArcArchivespecification;
import org.nrg.xdat.om.ArcPipelinedata;
import org.nrg.xdat.om.ArcProject;
import org.nrg.xdat.om.ArcProjectDescendant;
import org.nrg.xdat.om.ArcProjectDescendantPipeline;
import org.nrg.xdat.om.ArcProjectPipeline;
import org.nrg.xdat.om.PipePipelinedetails;
import org.nrg.xdat.om.PipePipelinedetailsElement;
import org.nrg.xdat.om.PipePipelinedetailsParameter;
import org.nrg.xdat.om.PipePipelinerepository;
import org.nrg.xdat.om.XnatAbstractprotocol;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xft.db.DBAction;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

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


	 public synchronized static void RemoveReferenceToPipelineFromProject(PipePipelinedetails pipeline, UserI user, String pID) {
		 //Check to see if the project has some study protocol data corresponding to the pipeline
		 //If it does do nothing
		 //If no data has been generated so far remove the reference to the study protocol generated by the pipeline
		 XnatProjectdata proj = XnatProjectdata.getXnatProjectdatasById(pID, user, false);
		 List<PipePipelinedetailsElementI> elementsGenerated =  pipeline.getGenerateselements_element();
		 if (proj != null) {
			 ArcProject arcProject = proj.getArcSpecification();
			 removePipeline(pipeline.getPath(), arcProject, user);
			 for (int i = 0; i < elementsGenerated.size(); i++) {
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
									 SaveItemHelper.authorizedDelete(protocol.getCurrentDBVersion(), user);
								 }catch(Exception e) {
									 logger.error("Couldnt delete pipelines located at " + pipeline.getPath() + " for project " + pID );
								 }
							 }
						 }
					 }
				 }
			 }
		 }
	 }

	 private static void removePipeline(String pipelinePath, ArcProject aProject, UserI user) {
		 List<ArcProjectPipelineI> projectPipelines = aProject.getPipelines_pipeline();
		 for (int j = 0; j < projectPipelines.size(); j++) {
			 ArcProjectPipeline projectPipeline = (ArcProjectPipeline)projectPipelines.get(j);
			 ArcPipelinedata pipeline = (ArcPipelinedata) projectPipeline.getPipelinedata();
			 String path = pipeline.getLocation() ;
			 if (pipelinePath.equals(path)) {
				 try {
					 SaveItemHelper.authorizedDelete(pipeline.getItem(), user);
					 break;
				 }catch(Exception e) {
					 logger.error("Couldnt delete pipelines located at " + pipelinePath + " for project " + aProject.getId() );
				 }
			 }
		 }
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
						 SaveItemHelper.authorizedDelete(pipeline.getCurrentDBVersion(), user);
						 break;
					 }catch(Exception e) {
						 logger.error("Couldnt delete pipelines located at " + pipelinePath + " for project " + aProject.getId() + " descendant  " + projectDesc.getXsitype() );
					 }
					 }
			 }
		 }
		 ArcSpecManager.Reset();
	 }

	 public synchronized static void RemoveReferenceToPipelineFromProjects(String pipelinePath, UserI user) {
		 ArcArchivespecification arcSpec = ArcSpecManager.GetInstance();
		 List<ArcProjectI> arcProjects = arcSpec.getProjects_project();
		 for (int i = 0; i < arcProjects.size(); i++) {
			 ArcProject aProject = (ArcProject) arcProjects.get(i);
			 removePipeline(pipelinePath,  aProject,  user);
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
