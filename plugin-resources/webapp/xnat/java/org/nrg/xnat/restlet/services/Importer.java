package org.nrg.xnat.restlet.services;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.util.GoogleUtils;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.restlet.actions.SessionImporter;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

import com.google.common.collect.Multimap;

public class Importer extends SecureResource {
	public Importer(Context context, Request request, Response response) {
		super(context, request, response);
				
	}

	@Override
	public boolean allowPost() {
		return true;
	}

	@Override
	public void handlePost() {		
		//build fileWriters
		try {
			final List<FileWriterWrapperI> fw=this.getFileWriters();
			
			if(fw.size()==0){
				this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Unable to identify upload format.");
				return;
			}
			
			if(fw.size()>1){
				this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Importer is limited to one uploaded resource at a time.");
				return;
			}
			
			final Map<String,Object> additionalValues=new Hashtable<String,Object>();
			
			String project_id=null;
			String subject_id=null;
			String session_id=null;
			String overwriteV=null;
			String listenerControl=null;
			
			//maintain parameters
			final Form f = getQueryVariableForm();
			for(final String key:f.getNames()){
				if(key.equals("project")){
					project_id=f.getFirstValue("project");
					additionalValues.put("project", project_id);
				}else if(key.equals("subject")){
					subject_id=f.getFirstValue("subject");
					additionalValues.put("subject_ID", subject_id);
				}else if(key.equals("session")){
					session_id=f.getFirstValue("session");
					additionalValues.put("label", session_id);
				}else if(key.equals("overwrite")){
					overwriteV=f.getFirstValue("overwrite");
				}else if(key.equals("listener_id")){
					overwriteV=f.getFirstValue("listener_id");
				}else{
					additionalValues.put(key,f.getFirstValue(key));
				}
				
			}
			
			XnatImagesessiondata session=null;
			
			if(session_id!=null){
				session=XnatImagesessiondata.getXnatImagesessiondatasById(session_id, user, false);
			}
			
			if(session==null){
				if(project_id!=null){
					session=(XnatImagesessiondata)XnatExperimentdata.GetExptByProjectIdentifier(project_id, session_id, user, false);
				}
			}
			
			if(session==null){
				if(project_id==null || subject_id==null || session_id==null){
					this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "New sessions require a project, subject and session id.");
					return;
				}
			}
			
			
			SessionImporter importer= new SessionImporter(listenerControl, user, project_id, session, overwriteV, fw.get(0), additionalValues);
			final Multimap<String,Object> response=importer.call();
			
			this.returnSuccessfulCreateFromList(GoogleUtils.getFirstParam(response, SessionImporter.RESPONSE_URL).toString());
		} catch (ClientException e) {
			this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage());
		} catch (ServerException e) {
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
		}
	}

	
}
