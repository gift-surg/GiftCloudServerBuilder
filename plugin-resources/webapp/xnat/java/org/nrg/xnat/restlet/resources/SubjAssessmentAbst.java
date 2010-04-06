// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;
import org.nrg.dcm.xnat.SessionBuilder;
import org.nrg.dcm.xnat.XnatAttrDef;
import org.nrg.ecat.xnat.PETSessionBuilder;
import org.nrg.pipeline.XnatPipelineLauncher;
import org.nrg.session.SessionBuilder.MultipleSessionException;
import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFTItem;
import org.nrg.xft.db.DBAction;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xft.schema.Wrappers.XMLWrapper.SAXReader;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xft.utils.ValidationUtils.ValidationResults;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.xml.sax.SAXException;

public class SubjAssessmentAbst extends QueryOrganizerResource {
    static Logger logger = Logger.getLogger(SubjAssessmentAbst.class);
    
	public SubjAssessmentAbst(Context context, Request request,
			Response response) {
		super(context, request, response);
	}

	public static boolean triggerPipelines(XnatExperimentdata expt, boolean clearExistingWorkflows,boolean supressEmail,XDATUser user) {
		XnatPipelineLauncher xnatPipelineLauncher = new XnatPipelineLauncher((XDATUser)user);
        xnatPipelineLauncher.setAdmin_email(AdminUtils.getAdminEmailId());
        xnatPipelineLauncher.setAlwaysEmailAdmin(ArcSpecManager.GetInstance().getEmailspecifications_pipeline());
        String pipelineName = "xnat_tools/AutoRun.xml";
        xnatPipelineLauncher.setPipelineName(pipelineName);
        xnatPipelineLauncher.setNeedsBuildDir(false);
        xnatPipelineLauncher.setSupressNotification(true);
        xnatPipelineLauncher.setId(expt.getId());
        xnatPipelineLauncher.setLabel(expt.getLabel());
        xnatPipelineLauncher.setDataType(expt.getXSIType());
        xnatPipelineLauncher.setExternalId(expt.getProject());
        xnatPipelineLauncher.setParameter("supressEmail", (new Boolean(supressEmail)).toString());
        xnatPipelineLauncher.setParameter("session", expt.getId());
        xnatPipelineLauncher.setParameter("sessionLabel", expt.getLabel());
        xnatPipelineLauncher.setParameter("useremail", user.getEmail());
        xnatPipelineLauncher.setParameter("userfullname", XnatPipelineLauncher.getUserName(user));
        xnatPipelineLauncher.setParameter("adminemail", AdminUtils.getAdminEmailId());
        xnatPipelineLauncher.setParameter("xnatserver", TurbineUtils.GetSystemName());
        xnatPipelineLauncher.setParameter("mailhost", AdminUtils.getMailServer());
        xnatPipelineLauncher.setParameter("sessionType", expt.getXSIType());
        xnatPipelineLauncher.setParameter("xnat_project", expt.getProject());
        
        if (clearExistingWorkflows)
        {
            try {
				ArrayList<WrkWorkflowdata> workflows = WrkWorkflowdata.getWrkWorkflowdatasByField("wrk:workFlowData.ID", expt.getId(), user, false);
				
				for (WrkWorkflowdata wrk : workflows){
				    DBAction.DeleteItem(wrk.getItem(),user);
				}
			} catch (SQLException e) {
				logger.error("",e);
			} catch (Exception e) {
				logger.error("",e);
			}
        }
   
        return xnatPipelineLauncher.launch(null);
    
	}
	
	public boolean triggerPipelines(XnatExperimentdata expt, boolean clearExistingWorkflows, String stepId) {
		boolean success = true;
		//REST CALL TO START THE AUTO-ARCHIVE PIPELINE
		//POST to trigger the AUTO-ARCHIVE PIPELINE
		try {
			String uriStr = "/REST/projects/" +expt.getProject() + "/pipelines/" +  stepId + "/experiments/" + expt.getId();
			URI uri = new URI(this.getRequest().getHostRef().getHostDomain() + uriStr);
			
			if (clearExistingWorkflows)
	        {
	            try {
					ArrayList<WrkWorkflowdata> workflows = WrkWorkflowdata.getWrkWorkflowdatasByField("wrk:workFlowData.ID", expt.getId(), user, false);
					
					for (WrkWorkflowdata wrk : workflows){
					    DBAction.DeleteItem(wrk.getItem(),user);
					}
				} catch (SQLException e) {
					logger.error("",e);
				} catch (Exception e) {
					logger.error("",e);
				}
	        }
	
			Client client = new Client(this.getRequest().getProtocol());
			Reference reference = new Reference(uri.toString());
			
			Request restRequest = new Request(Method.POST, reference);
		 	restRequest.setCookies(this.getRequest().getCookies());
			
		 	Response response = client.handle(restRequest);
		 	success = response.getStatus().isSuccess();
		}catch(URISyntaxException ure) {
			success = false;
		}
        return success;
	}

/*	public boolean triggerPipelines(XnatExperimentdata expt, boolean clearExistingWorkflows) {
		return triggerPipelines(expt, clearExistingWorkflows, PipelineUtils.AUTO_ARCHIVE);
	} */



	public static void pullDataFromHeaders(XnatImagesessiondata tempMR, XDATUser user, boolean allowDataDeletion, boolean overwrite)
	throws IOException,SAXException,Exception {
		final Date d = Calendar.getInstance().getTime();
        
		final String derivedSessionDir=tempMR.deriveSessionDir();
		
		if (derivedSessionDir==null){
			throw new Exception("Unable to derive session directory");
		}
		
		final File sessionDir=new File(derivedSessionDir);
		
        final SimpleDateFormat formatter = new SimpleDateFormat ("yyyyMMdd_HHmmss");
        final String timestamp=formatter.format(d);
		final File xml = new File(sessionDir,tempMR.getLabel()+ "_"+ timestamp+".xml");
		try {
			final SessionBuilder builder = new SessionBuilder(sessionDir,
					new FileWriter(xml),
					new XnatAttrDef.Constant("project", tempMR.getProject()));
			try {
				builder.run();
			} finally {
				builder.dispose();
			}
		} catch (MultipleSessionException e) {
			logger.debug(sessionDir + " does not contain a single DICOM study", e);
		} catch (IOException e) {
			logger.warn("unable to process session directory " + sessionDir, e);
		} catch (SQLException e) {
			logger.error("unable to process session directory " + sessionDir, e);
		} catch (Throwable e) {
			logger.error(e);
		}
	    
	    if (!xml.exists() || xml.length()==0) {
	    	new PETSessionBuilder(sessionDir,new FileWriter(xml),tempMR.getProject()).run();
	    }
	      
	    if (!xml.exists() || xml.length()==0) {
	    	new Exception("Unable to locate DICOM or ECAT files");
	    }
	    
		SAXReader reader = new SAXReader(user);
		XFTItem temp2 = reader.parse(xml.getAbsolutePath());
		XnatImagesessiondata newmr = (XnatImagesessiondata)BaseElement.GetGeneratedItem(temp2);
        
        if(overwrite)
        {
        	allowDataDeletion=false;
        	newmr.setId(tempMR.getId());
        }else{
            newmr.copyValuesFrom(tempMR);

            for (final XnatImagescandata newscan : newmr.getSortedScans()){
            	final XnatImagescandata oldScan = tempMR.getScanById(newscan.getId());
            	if(oldScan!=null){
                	newscan.setXnatImagescandataId(oldScan.getXnatImagescandataId());
                	
        		    if(!allowDataDeletion){
        		    	if(newscan.getFile().size()>0){
        		    		XnatResource newcat=(XnatResource)newscan.getFile().get(0);
        		    		
        		    		XnatAbstractresource oldCat=oldScan.getFile().get(0);
        		    		if(oldCat instanceof XnatResource){
        		    			if(StringUtils.IsEmpty(((XnatResource)oldCat).getContent()) && !StringUtils.IsEmpty(newcat.getContent()))
        		    				((XnatResource)oldCat).setContent(newcat.getContent());
        		    			if(StringUtils.IsEmpty(((XnatResource)oldCat).getFormat()) && !StringUtils.IsEmpty(newcat.getFormat()))
        		    				((XnatResource)oldCat).setFormat(newcat.getFormat());
        		    			if(StringUtils.IsEmpty(((XnatResource)oldCat).getDescription()) && !StringUtils.IsEmpty(newcat.getDescription()))
        		    				((XnatResource)oldCat).setDescription(newcat.getDescription());
        		    		}
        		    		
        		    		while(newscan.getFile().size()>0)newscan.removeFile(0);
                    		
                    		newscan.setFile(oldCat);
        		    	}else{
        		    		while(newscan.getFile().size()>0)newscan.removeFile(0);
                    		
        		    	}
                	}
            	}
    		}
            
        	newmr.setId(tempMR.getId());
        }
        
        newmr.fixScanTypes();        
        
        final ValidationResults vr = newmr.validate();        
        
        if (vr != null && !vr.isValid())
        {
            throw new Exception(vr.toString());
        }else{
        	XnatProjectdata proj = newmr.getProjectData();
        	if(newmr.save(user,false,allowDataDeletion)){
				MaterializedView.DeleteByUser(user);

				if(proj.getArcSpecification().getQuarantineCode()!=null && proj.getArcSpecification().getQuarantineCode().equals(1)){
					newmr.quarantine(user);
				}
			}
            
            try {
  				WrkWorkflowdata workflow = new WrkWorkflowdata((UserI)user);
  				workflow.setDataType(newmr.getXSIType());
  				workflow.setExternalid(proj.getId());
  				workflow.setId(newmr.getId());
  				workflow.setPipelineName("Header Mapping");
  				workflow.setStatus("Complete");
  				workflow.setLaunchTime(Calendar.getInstance().getTime());
  				workflow.save(user, false, false);
  			} catch (Throwable e) {
  				logger.error("",e);
  			}
        }
    
	}
	
	@Override
	public ArrayList<String> getDefaultFields(GenericWrapperElement e) {
		ArrayList<String> al=new ArrayList<String>();
		
		al.add("ID");
		al.add("project");
		al.add("date");
		al.add("xsiType");
		al.add("label");
		al.add("insert_date");
		
		return al;
	}

	public String getDefaultElementName(){
		return "xnat:subjectAssessorData";
	}
}