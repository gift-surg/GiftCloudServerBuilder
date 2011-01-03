/*
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 *
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

*/

package org.nrg.xnat.turbine.modules.actions;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

import org.apache.turbine.modules.ScreenLoader;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.apache.xmlbeans.XmlOptions;
import org.nrg.pipeline.PipelineRepositoryManager;
import org.nrg.pipeline.XnatPipelineLauncher;
import org.nrg.pipeline.utils.FileUtils;
import org.nrg.pipeline.utils.PipelineAdder;
import org.nrg.pipeline.utils.PipelineUtils;
import org.nrg.pipeline.xmlbeans.ParameterData;
import org.nrg.pipeline.xmlbeans.ParametersDocument;
import org.nrg.pipeline.xmlbeans.ParameterData.Values;
import org.nrg.pipeline.xmlbeans.ParametersDocument.Parameters;
import org.nrg.xdat.om.ArcPipelinedataI;
import org.nrg.xdat.om.ArcProject;
import org.nrg.xdat.om.ArcProjectDescendant;
import org.nrg.xdat.om.ArcProjectDescendantPipeline;
import org.nrg.xdat.om.ArcProjectPipeline;
import org.nrg.xdat.om.PipePipelinedetails;
import org.nrg.xdat.om.PipePipelinerepository;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.modules.screens.EditScreenA;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.PopulateItem;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFTItem;
import org.nrg.xft.db.DBAction;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.InvalidValueException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.utils.ValidationUtils.ValidationResults;
import org.nrg.xnat.exceptions.PipelineNotFoundException;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

public class ManagePipeline extends SecureAction {
	static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ManagePipeline.class);

	public void doPerform(RunData data, Context context) throws Exception {
		String task = data.getParameters().get("task");
		if (task != null) {
			if (task.equalsIgnoreCase("DELETE")) {
				doDelete(data,context);
				return;
			}else if (task.equalsIgnoreCase("EDIT")) {
				doEdit(data,context);
				return;
			}else if (task.equalsIgnoreCase("RESET")) {
				PipelineRepositoryManager.Reset();
				data.setMessage("Pipeline Repository Reset");
				return;
			}else if (task.equalsIgnoreCase("DELETEFROMPROJECT")) {
				doDeletefromproject(data,context);
				return;
			}else if (task.equalsIgnoreCase("projectpipeline")) {
				doAddpipeline(data,context);
				return;
			}
		}
	}


	private void doEdit(RunData data, Context context) {

	}

	
	public void doAddpipeline(RunData data, Context context) throws Exception {
		PipelineAdder pipelineAdder = new PipelineAdder();
		pipelineAdder.prepareScreen(data, context);
	}
	
	private void doDeletefromproject(RunData data, Context context) {
		XDATUser user = TurbineUtils.getUser(data);
		String projectId = data.getParameters().get("project");
		try {
			XFTItem pline = TurbineUtils.GetItemBySearch(data);
			if (pline != null) {
	            PipePipelinedetails pipeline = new PipePipelinedetails(pline);
				data.setMessage("Item deleted");
				PipelineRepositoryManager.RemoveReferenceToPipelineFromProject(pipeline, user, projectId);
				PipelineRepositoryManager.Reset();
			}
		}catch(Exception e) {
    		logger.error("Error deleting "  + data.getParameters().get("search_value") ,e);
    		data.setMessage("Error Deleting item.");
		}
        return;
	}


	private void doDelete(RunData data, Context context) {
		XDATUser user = TurbineUtils.getUser(data);
		try {
			XFTItem pipeline = TurbineUtils.GetItemBySearch(data);
			if (pipeline != null) {
				DBAction.DeleteItem(pipeline.getCurrentDBVersion(), user);
				logger.info("Deleted " + pipeline.getProperty("path"));
				data.setMessage("Pipeline removed from site repository");
				PipelineRepositoryManager.RemoveReferenceToPipelineFromProjects( (String)pipeline.getProperty("path"), user);
				PipelineRepositoryManager.Reset();
			}
		}catch(Exception e) {
    		logger.error("Error deleting "  + data.getParameters().get("search_value") ,e);
    		data.setMessage("Error Deleting item.");
		}
		data.setScreenTemplate("ClosePageAndRefresh.vm");
        return;
	}

	public void doRedirect(RunData data, Context context) throws Exception {
		XDATUser user = TurbineUtils.getUser(data);
		try {
	        String projectId = data.getParameters().get("project");
	        String pipelinePath = data.getParameters().get("pipeline");
	        String schemaType = data.getParameters().get("schema_type");
	        context.put("pipelinePath", pipelinePath);
	        String customWebPage = "PipelineScreen_default_launcher.vm";
	        try {
		        ArcProject arcProject = ArcSpecManager.GetInstance().getProjectArc(projectId);
		        if (schemaType.equals(XnatProjectdata.SCHEMA_ELEMENT_NAME)) {
		        	ArcProjectPipeline pipelineData = (ArcProjectPipeline)arcProject.getPipelineByPath(pipelinePath);
		        	if (pipelineData.getCustomwebpage() != null)
		        	 customWebPage = pipelineData.getCustomwebpage();
		        }else {
		        	ArcPipelinedataI pipelineData = arcProject.getPipelineForDescendantByPath(schemaType, pipelinePath);
		        	if (pipelineData.getCustomwebpage() != null)
		        		customWebPage = pipelineData.getCustomwebpage();
		        }
	        }catch(PipelineNotFoundException pne) {
	        	//Could be an additional pipeline not yet defined for the project
	        	PipePipelinedetails pipelineDetails = PipelineRepositoryManager.GetInstance().getPipeline(pipelinePath);
	        	if (pipelineDetails.getCustomwebpage() != null)
	        		customWebPage = pipelineDetails.getCustomwebpage();
	        }
	        data.setScreenTemplate(customWebPage);
		}catch(Exception e) {
            data.setMessage("Unknown Error.");
        	logger.error("", e);
		}

	}

	private String getStepId(String templateSuppliedStepId, boolean launchedAtAutoArchive, String nextStepId, String displayText ) {
		String rtn = templateSuppliedStepId;
		if (templateSuppliedStepId != null) {
			if (launchedAtAutoArchive) {
				if (templateSuppliedStepId.startsWith(PipelineUtils.AUTO_ARCHIVE)) {
					rtn = templateSuppliedStepId;
				}else 
					rtn = nextStepId;
			}else if (!templateSuppliedStepId.startsWith(PipelineUtils.AUTO_ARCHIVE))
				rtn = templateSuppliedStepId;
			else
				rtn = displayText;
		}else { 
			if (launchedAtAutoArchive) {
				rtn = nextStepId;
			}else {
				rtn = displayText;
			}
		}
		return rtn;
	}
	
	public void doAddprojectpipeline(RunData data, Context context) throws Exception {
		XDATUser user = TurbineUtils.getUser(data);
        XFTItem found = null;
        try {
            String projectId = data.getParameters().get("project");
            String pipelinePath = data.getParameters().get("pipeline_path");
            String dataType = data.getParameters().get("dataType");
            String schemaType = data.getParameters().get("schemaType");

            boolean edit = data.getParameters().getBoolean("edit");

        	XFTItem newItem = XFTItem.NewItem(schemaType,TurbineUtils.getUser(data));
            TurbineUtils.OutputDataParameters(data);

            //Get the pipeline identified by the pipeline_path
            //Get the ArcProject element identified by the projectId
            //Set the pipeline for the data-type and send it to the screen for the user to add parameters
            PopulateItem populater = PopulateItem.Populate(data,schemaType,true,newItem);
            found = populater.getItem();

            boolean launchedAtAutoArchive = data.getParameters().getBoolean("auto_archive");
			String templateSuppliedStepId = found.getStringProperty("stepid");
	   		boolean saved = false;
	   	 
            //A set of pipelines can be launched on auto archive.This set will have
            //AUTO_ARCHIVE_<SEQUENTIAL NUMBER> unless site wants to setup the stepid at the template level
            //If the step is provided at the template level, it must be of the kind AUTO_<SOMETHING>
            //It is assumed that the sequence will be independent

    		ArcProject arcProject = ArcSpecManager.GetInstance().getProjectArc(projectId);
    		if (dataType.equals(XnatProjectdata.SCHEMA_ELEMENT_NAME)) { //Its a project level pipeline
    			ArcProjectPipeline newPipeline = new ArcProjectPipeline(found);
    			if (edit) {
    				ArcProjectPipeline existing = arcProject.getPipelineEltByPath(pipelinePath);
    				copy (data, existing);
    				if (existing.getStepid().startsWith(PipelineUtils.AUTO_ARCHIVE) && !launchedAtAutoArchive) {
    					existing.setStepid(existing.getDisplaytext());
    				}
    				saved = existing.save(user, false, false);
    			}else {
    				String stepId = getStepId(templateSuppliedStepId,launchedAtAutoArchive, PipelineUtils.getNextAutoArchiveStepId(arcProject), newPipeline.getDisplaytext() );
    				newPipeline.setStepid(stepId);
    			}
    				arcProject.setPipelines_pipeline(newPipeline.getItem());	
    		}else {
    			ArcProjectDescendant existingDesc = arcProject.getDescendant(dataType);
    			ArcProjectDescendant newDesc = new ArcProjectDescendant();
    			ArcProjectDescendantPipeline newPipeline = new ArcProjectDescendantPipeline(found);
    			if (existingDesc == null) {
    				newDesc.setXsitype(dataType);
       				String stepId = getStepId(templateSuppliedStepId,launchedAtAutoArchive, PipelineUtils.getNextAutoArchiveStepId(existingDesc), newPipeline.getDisplaytext() );
    				newPipeline.setStepid(stepId);
 	 				newDesc.setPipeline(newPipeline.getItem());
   					arcProject.setPipelines_descendants_descendant(newDesc.getItem());
    			}else {
       				if (edit) {
       					ArcProjectDescendantPipeline existingPipeline = existingDesc.getPipeline(pipelinePath);
       					copy(data,existingPipeline);
        				if (existingPipeline.getStepid().startsWith(PipelineUtils.AUTO_ARCHIVE) && !launchedAtAutoArchive) {
        					existingPipeline.setStepid(existingPipeline.getDisplaytext());
        				}
       					saved = existingPipeline.save(user, false, false);
    				}else {
    	   				String stepId = getStepId(templateSuppliedStepId,launchedAtAutoArchive, PipelineUtils.getNextAutoArchiveStepId(existingDesc), newPipeline.getDisplaytext() );
        				newPipeline.setStepid(stepId);
    					existingDesc.setPipeline(newPipeline.getItem());
       				    newDesc.setItem(existingDesc.getItem());
       				    arcProject.setPipelines_descendants_descendant(newDesc.getItem());
    				}
    			}
    		}
    		if (!edit) {
    			saved = arcProject.save(user, false, false);
    		}
    		ArcSpecManager.Reset();
    		String msg = "<p><b>The pipelines for the project could NOT be modified.</b></p>";
            if (saved) {
            	msg = "<p><b>The pipelines for the project were successfully modified.</b></p>";
            }
            data.setMessage(msg);
            data.setScreenTemplate("ClosePage.vm");
    		// ItemI project = TurbineUtils.GetItemBySearch(data,false);
            //this.redirectToReportScreen((String)TurbineUtils.GetPassedParameter("destination", data), project, data);
    		// data.setAction("DisplayAction");
        }catch(Exception e) {
            data.setMessage("Unknown Error.");
        	logger.error("", e);
        }
	}

	  private String getName(String path) {
	    	String rtn = path;
	    	int index = path.lastIndexOf(File.separator);
	    	if (index != -1) {
	    		rtn = path.substring(index + 1);
	        	index = rtn.lastIndexOf(".xml");
	        	if (index != -1) {
	        		rtn = rtn.substring(0, index);
	        	}
	    	}
	    	return rtn;
	    }

	private void copy (RunData data, ArcProjectDescendantPipeline existingPipeline) throws XFTInitException, ElementNotFoundException,FieldNotFoundException, InvalidValueException {
		  Hashtable hash = (Hashtable) TurbineUtils.GetDataParameterHash(data);
		  existingPipeline.getItem().setProperties(hash, true);
	}

	private void copy (RunData data, ArcProjectPipeline existingPipeline) throws XFTInitException, ElementNotFoundException,FieldNotFoundException, InvalidValueException {
		  Hashtable hash = (Hashtable) TurbineUtils.GetDataParameterHash(data);
		  existingPipeline.getItem().setProperties(hash, true);
	}

	//FInal step of adding a pipeline to the Site
	public void doAdd(RunData data, Context context) throws Exception {
		XDATUser user = TurbineUtils.getUser(data);
        XFTItem found = null;

        try {
            EditScreenA screen = (EditScreenA) ScreenLoader.getInstance().getInstance("XDATScreen_add_pipeline");
            XFTItem newItem = (XFTItem)screen.getEmptyItem(data);
            TurbineUtils.OutputDataParameters(data);
            PopulateItem populater = PopulateItem.Populate(data,"pipe:pipelineDetails",true,newItem);
            found = populater.getItem();

            PipePipelinedetails pipelineDetails = new PipePipelinedetails(found);
            PipelineRepositoryManager.SetInfo(pipelineDetails);

            ValidationResults vr = null;

            ValidationResults temp = pipelineDetails.getItem().validate();
            if (! pipelineDetails.getItem().isValid())
            {
               vr = temp;
            }

            if (vr != null)
            {
                TurbineUtils.SetEditItem(pipelineDetails.getItem(),data);
                context.put("vr",vr);
                if (data.getParameters().getString("edit_screen") !=null)
                {
                    data.setScreenTemplate(data.getParameters().getString("edit_screen"));
                }
            }else{
            	try {
            		PipePipelinerepository pipelineRepository = PipelineRepositoryManager.GetInstance();
            		pipelineRepository.setPipeline(pipelineDetails);
            		pipelineRepository.save(user, false, true);
            		PipelineRepositoryManager.Reset();
            		data.setMessage("Pipeline " + pipelineDetails.getPath() + " has been successfully added to the repository");
            		data.setScreenTemplate("ClosePageAndRefresh.vm");
            	} catch (Exception e) {
            		logger.error("Error Storing " + found.getXSIType(),e);
            		data.setMessage("Error Saving item.");
                    TurbineUtils.SetEditItem(found,data);
                    if (data.getParameters().getString("edit_screen") !=null)
                    {
                        data.setScreenTemplate(data.getParameters().getString("edit_screen"));
                    }
                    return;
            	}
            }
        }catch(Exception e) {
            logger.error("",e);
            data.setMessage("Unknown Error.");
            TurbineUtils.SetEditItem(found,data);
            if (data.getParameters().getString("edit_screen") !=null)
            {
                data.setScreenTemplate(data.getParameters().getString("edit_screen"));
            }
        }
	}


	public void doLaunchpipeline(RunData data, Context context) throws Exception {
		try {
			XDATUser user = TurbineUtils.getUser(data);
			XFTItem item = TurbineUtils.GetItemBySearch(data);
			String pipeline_path = data.getParameters().get("pipeline_path");
			XnatPipelineLauncher xnatPipelineLauncher = new XnatPipelineLauncher(user);
			xnatPipelineLauncher.setSupressNotification(true);
	        xnatPipelineLauncher.setParameter("useremail", user.getEmail());
		    xnatPipelineLauncher.setParameter("userfullname", XnatPipelineLauncher.getUserName(user));
		    xnatPipelineLauncher.setParameter("adminemail", AdminUtils.getAdminEmailId());
		    xnatPipelineLauncher.setParameter("mailhost", AdminUtils.getMailServer());
		    xnatPipelineLauncher.setParameter("xnatserver", TurbineUtils.GetSystemName());
		    xnatPipelineLauncher.setPipelineName(pipeline_path);
		    String exptLabel = item.getStringProperty("label");
		    String project = item.getStringProperty("project");
			xnatPipelineLauncher.setId(item.getStringProperty("ID"));
			xnatPipelineLauncher.setLabel(exptLabel);
			xnatPipelineLauncher.setExternalId(project);
			xnatPipelineLauncher.setDataType(item.getXSIType());
			Parameters parameters = extractParameters(data, context);
			Date date = new Date();
	    	SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
		    String s = formatter.format(date);
			String paramFileName = exptLabel + "_params_" + s + ".xml";
			String buildDir = FileUtils.getBuildDir(project, true);
			xnatPipelineLauncher.setBuildDir(buildDir);
			String paramFilePath = saveParameters(buildDir+File.separator + exptLabel,paramFileName,parameters);
		    xnatPipelineLauncher.setParameterFile(paramFilePath);
		    xnatPipelineLauncher.launch();

		    data.setMessage("<p><b>The pipeline has been scheduled.  Status email will be sent upon its completion.</b></p>");
	        data.setScreenTemplate("ClosePage.vm");
		}catch(Exception e) {
			e.printStackTrace();
			logger.debug(e);
		}

	}


	private Parameters extractParameters(RunData data, Context context){
		Parameters parameters = Parameters.Factory.newInstance();
		int totalParams = data.getParameters().getInt("param_cnt");
		for (int i =0; i < totalParams; i++) {
			String name = data.getParameters().get("param[" + i + "].name");
			int rowcount = new Integer(data.getParameters().get("param[" + i + "].name.rowcount")).intValue();
			ArrayList<String> formvalues = new ArrayList<String>();
			for (int j=0; j < rowcount; j++) {
				String formfieldname = "param[" + i + "][" + j + "].value";
				if (TurbineUtils.HasPassedParameter(formfieldname,data))
				   formvalues.add(data.getParameters().get(formfieldname));
			}

			if (formvalues.size()>0) {
				ParameterData param = parameters.addNewParameter();
				param.setName(name);
				if (formvalues.size()==1) {
					Values values = param.addNewValues();
					values.setUnique(formvalues.get(0));
				}else {
					Values values = param.addNewValues();
					for (int k=0; k<formvalues.size(); k++) {
						values.addList(formvalues.get(k));
					}
				}
			}
		}
		return parameters;
	}


	private String saveParameters(String rootpath, String fileName, Parameters parameters) throws Exception{
        File dir = new File(rootpath);
        if (!dir.exists()) dir.mkdirs();
        File paramFile = new File(rootpath + File.separator + fileName);
        ParametersDocument paramDoc = ParametersDocument.Factory.newInstance();
        paramDoc.addNewParameters().set(parameters);
        paramDoc.save(paramFile,new XmlOptions().setSavePrettyPrint().setSaveAggressiveNamespaces());
        return paramFile.getAbsolutePath();
    }

	@Deprecated
	//Step 1 of Adding a pipeline
	public void doNext(RunData data, Context context) throws Exception {
		XDATUser user = TurbineUtils.getUser(data);
        XFTItem found = null;

        try {
            EditScreenA screen = (EditScreenA) ScreenLoader.getInstance().getInstance("XDATScreen_add_pipeline");
            XFTItem newItem = (XFTItem)screen.getEmptyItem(data);
            TurbineUtils.OutputDataParameters(data);
            PopulateItem populater = PopulateItem.Populate(data,"pipe:pipelineDetails",true,newItem);
            found = populater.getItem();

            PipePipelinedetails pipelineDetails = new PipePipelinedetails(found);

            ValidationResults vr = null;

            ValidationResults temp = pipelineDetails.getItem().validate();
            if (! pipelineDetails.getItem().isValid())
            {
               vr = temp;
            }

            if (vr != null)
            {
                TurbineUtils.SetEditItem(pipelineDetails.getItem(),data);
                context.put("vr",vr);
                if (data.getParameters().getString("edit_screen") !=null)
                {
                    data.setScreenTemplate(data.getParameters().getString("edit_screen"));
                }
            }else{
            	context.put("pipeline", pipelineDetails);
            	data.setScreenTemplate("PipelineScreen_set_site_parameters.vm");
            }
        }catch(Exception e) {
            logger.error("",e);
            data.setMessage("Unknown Error.");
            TurbineUtils.SetEditItem(found,data);
            if (data.getParameters().getString("edit_screen") !=null)
            {
                data.setScreenTemplate(data.getParameters().getString("edit_screen"));
            }
        }
	}


}
