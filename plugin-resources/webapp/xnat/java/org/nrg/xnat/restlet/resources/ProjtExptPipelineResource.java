/*
 *	Copyright Washington University in St Louis 2006
 *	All rights reserved
 *
 * 	@author Mohana Ramaratnam (Email: mramarat@wustl.edu)

*/

package org.nrg.xnat.restlet.resources;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.xmlbeans.XmlOptions;
import org.nrg.pipeline.XnatPipelineLauncher;
import org.nrg.pipeline.utils.FileUtils;
import org.nrg.pipeline.xmlbeans.ParameterData;
import org.nrg.pipeline.xmlbeans.ParameterData.Values;
import org.nrg.pipeline.xmlbeans.ParametersDocument;
import org.nrg.pipeline.xmlbeans.ParametersDocument.Parameters;
import org.nrg.xdat.model.ArcPipelinedataI;
import org.nrg.xdat.model.ArcPipelineparameterdataI;
import org.nrg.xdat.om.ArcPipelinedata;
import org.nrg.xdat.om.ArcPipelineparameterdata;
import org.nrg.xdat.om.ArcProject;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFTItem;
import org.nrg.xnat.exceptions.ValidationException;
import org.nrg.xnat.restlet.actions.FixScanTypes;
import org.nrg.xnat.restlet.actions.PullSessionDataFromHeaders;
import org.nrg.xnat.restlet.actions.TriggerPipelines;
import org.nrg.xnat.restlet.util.XNATRestConstants;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.xml.sax.SAXException;

public class ProjtExptPipelineResource extends SecureResource {
	XnatProjectdata proj=null;
	XnatExperimentdata expt=null;
    String step = null;

	public ProjtExptPipelineResource(Context context, Request request, Response response) {
		super(context, request, response);

		String pID = (String) request.getAttributes().get("PROJECT_ID");
		if (pID != null) {
			proj = XnatProjectdata.getXnatProjectdatasById(pID, user, false);

			step = (String) request.getAttributes().get("STEP_ID");
			if (step != null) {
				String exptID = (String) request.getAttributes().get("EXPT_ID");
				if (exptID != null) {
					expt = XnatExperimentdata.getXnatExperimentdatasById(
							exptID, user, false);

					if (expt == null) {
						expt = XnatExperimentdata.GetExptByProjectIdentifier(
								proj.getId(), exptID, user, false);
					}
				}
				this.getVariants().add(new Variant(MediaType.TEXT_XML));

			} else {
				response.setStatus(Status.CLIENT_ERROR_GONE);
			}
		} else {
			response.setStatus(Status.CLIENT_ERROR_GONE);
		}
	}


	@Override
	public Representation getRepresentation(Variant variant) {
		if(proj!=null && step!=null){
			ArcPipelinedata arcPipeline = null;
			ArcProject arcProject = ArcSpecManager.GetInstance().getProjectArc(proj.getId());
			arcProject.setItem(arcProject.getCurrentDBVersion());
			Form f = getRequest().getResourceRef().getQueryAsForm();
			try {
				if (expt == null) { // Look for Project level pipeline
					arcPipeline = (ArcPipelinedata)arcProject.getPipeline(step);
				}else { //Look for experiment level pipeline
					arcPipeline = (ArcPipelinedata)arcProject.getPipelineForDescendant(expt.getXSIType(), step);
				}
				MediaType mt = overrideVariant(variant);
				if (mt.equals(MediaType.TEXT_XML)) {
					return representItem(arcPipeline.getItem(), mt, null,false, true);
				}else {
					return null;
				}
			}catch(Exception e) {
				e.printStackTrace();
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			}
		}else {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
		}
		return null;
	}

	@Override
	public boolean allowPost() {
		return true;
	}


	@Override
	public void handlePost() {
		if(proj!=null && step!=null && expt != null){
			try {
				if(step.equals(XNATRestConstants.TRIGGER_PIPELINES)){
					if(user.canEdit(expt)){
						FixScanTypes fst=new FixScanTypes(expt,user,proj,true);
						fst.call();
						
						TriggerPipelines tp = new TriggerPipelines(expt,true,this.isQueryVariableTrue(XNATRestConstants.SUPRESS_EMAIL),user);
						tp.call();
						}
				}else if(step.equals(XNATRestConstants.PULL_DATA_FROM_HEADERS) && expt instanceof XnatImagesessiondata){
					if(user.canEdit(expt)){
						try {
							PullSessionDataFromHeaders pull=new PullSessionDataFromHeaders((XnatImagesessiondata)expt, user, this.isQueryVariableTrue("allowDataDeletion"), this.isQueryVariableTrue("overwrite"),false);
							pull.call();
						} catch (SAXException e){
							logger.error("",e);
							this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,e.getMessage());
						} catch (ValidationException e){
							logger.error("",e);
							this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,e.getMessage());
						} catch (Exception e) {
							logger.error("",e);
							this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e.getMessage());
							return;
							}
					}else{
						getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
						}
				}else if(step.equals(XNATRestConstants.FIX_SCAN_TYPES) && expt instanceof XnatImagesessiondata){
					if(user.canEdit(expt)){
						FixScanTypes fst=new FixScanTypes(expt,user,proj,true);
						fst.call();
					}else{
						getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
					}
				}else{
					ArcProject arcProject = ArcSpecManager.GetInstance().getProjectArc(proj.getId());
					arcProject.setItem(arcProject.getCurrentDBVersion());
					Form f = getRequest().getResourceRef().getQueryAsForm();
					String match = null;
					if(f!=null)match=f.getFirstValue("match");
					if (match == null) match = "EXACT";

					try {
						ArrayList<ArcPipelinedataI> arcPipelines = arcProject.getPipelinesForDescendant(expt.getXSIType(), step, match);
						for (int i =0; i < arcPipelines.size(); i++) {
							ArcPipelinedataI arcPipeline = arcPipelines.get(i);
							boolean success = launch(arcPipeline);
							logger.info("Launching pipeline at step " + arcPipeline.getLocation() + File.separator + arcPipeline.getName());
						}
					}catch(Exception e) {
						e.printStackTrace();
						getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
					}
				}
			} catch (Exception e) {
				logger.error(e);
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e.getMessage());
			}
		}else {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
		}
	}

	private boolean launch(ArcPipelinedataI arcPipeline) throws Exception {
		XnatPipelineLauncher xnatPipelineLauncher = new XnatPipelineLauncher(user);
		xnatPipelineLauncher.setSupressNotification(true);
        xnatPipelineLauncher.setParameter("useremail", user.getEmail());
	    xnatPipelineLauncher.setParameter("userfullname", XnatPipelineLauncher.getUserName(user));
	    xnatPipelineLauncher.setParameter("adminemail", AdminUtils.getAdminEmailId());
	    xnatPipelineLauncher.setParameter("mailhost", AdminUtils.getMailServer());
	    xnatPipelineLauncher.setParameter("xnatserver", TurbineUtils.GetSystemName());

	    
	    xnatPipelineLauncher.setPipelineName(arcPipeline.getLocation());
		xnatPipelineLauncher.setId(expt.getId());
		xnatPipelineLauncher.setLabel(expt.getLabel());
		xnatPipelineLauncher.setExternalId(expt.getProject());
		xnatPipelineLauncher.setDataType(expt.getXSIType());

		String buildDir = FileUtils.getBuildDir(expt.getProject(), true);
		buildDir +=   "archive_trigger"  ;
		xnatPipelineLauncher.setBuildDir(buildDir);
		xnatPipelineLauncher.setNeedsBuildDir(false);

		Parameters parameters = Parameters.Factory.newInstance();
		ParameterData param = parameters.addNewParameter();
    	param.setName("xnat_id");
    	param.addNewValues().setUnique(expt.getId());

    	if (expt instanceof XnatImagesessiondata) {
    		String path = ((XnatImagesessiondata)expt).getArchivePath();
    		if (path.endsWith(File.separator)) path = path.substring(0, path.length()-1);
	   		param = parameters.addNewParameter();
	    	param.setName("archivedir");
	    	param.addNewValues().setUnique(path);
    	}


    	param = parameters.addNewParameter();
    	param.setName("sessionId");
    	param.addNewValues().setUnique(expt.getLabel());

    	param = parameters.addNewParameter();
    	param.setName("project");
    	param.addNewValues().setUnique(expt.getProject());

    	XFTItem itemOfExpectedXsiType = expt.getItem();

		List<ArcPipelineparameterdataI> pipelineParameters = arcPipeline.getParameters_parameter();
    	for (int i = 0; i < pipelineParameters.size(); i++) {
    		ArcPipelineparameterdataI pipelineParam = pipelineParameters.get(i);
    		String schemaLink = pipelineParam.getSchemalink();
    		if (schemaLink != null) {
    			Object o = itemOfExpectedXsiType.getProperty(schemaLink, true);
    			if (o != null ) {
	    			try {
	        			ArrayList<XFTItem>  matches = (ArrayList<XFTItem>) o;
	        			if (matches !=  null) {
	        		    	param = parameters.addNewParameter();
	        		    	param.setName(pipelineParam.getName());
	        		    	Values values = param.addNewValues();
	        				if (matches.size() == 1) {
		        		    	values.setUnique(""+matches.get(0));
		        			}else {
			    				for (int j = 0; j < matches.size(); j++) {
			    					values.addList(""+matches.get(j));
			        			}
		        			}
	        			}
	    			}catch(ClassCastException  cce) {
        		    	param = parameters.addNewParameter();
        		    	param.setName(pipelineParam.getName());
        		    	Values values = param.addNewValues();
        		    	values.setUnique(""+o);
	    			}
    			}
    		}else {
    			String pValues = pipelineParam.getCsvvalues();
    			String[] pValuesSplit = pValues.split(",");
		    	param = parameters.addNewParameter();
		    	param.setName(pipelineParam.getName());
		    	Values values = param.addNewValues();
		    	if (pValuesSplit.length == 1) {
		    		values.setUnique(pValuesSplit[0]);
		    	}else
	    			for (int j = 0; j < pValuesSplit.length; j++) {
	    				values.addList(pValuesSplit[j]);
	    			}
    		}
    	}
    	Date date = new Date();
    	SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
	    String s = formatter.format(date);
		String paramFileName = expt.getLabel() + "_params_" + s + ".xml";
		String paramFilePath = saveParameters(buildDir+File.separator + expt.getLabel(),paramFileName,parameters);
	    xnatPipelineLauncher.setParameterFile(paramFilePath);
	    return xnatPipelineLauncher.launch();
	}

	protected String saveParameters(String rootpath, String fileName, Parameters parameters) throws Exception{
        File dir = new File(rootpath);
        if (!dir.exists()) dir.mkdirs();
        File paramFile = new File(rootpath + File.separator + fileName);
        ParametersDocument paramDoc = ParametersDocument.Factory.newInstance();
        paramDoc.addNewParameters().set(parameters);
        paramDoc.save(paramFile,new XmlOptions().setSavePrettyPrint().setSaveAggressiveNamespaces());
        return paramFile.getAbsolutePath();
    }
}
