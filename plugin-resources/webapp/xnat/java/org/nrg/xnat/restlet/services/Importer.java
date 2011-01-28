package org.nrg.xnat.restlet.services;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.status.StatusList;
import org.nrg.util.GoogleUtils;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xnat.helpers.transactions.HTTPSessionStatusManagerQueue;
import org.nrg.xnat.helpers.transactions.PersistentStatusQueueManagerI;
import org.nrg.xnat.restlet.actions.SessionImporter;
import org.nrg.xnat.restlet.actions.importer.ImporterHandlerA;
import org.nrg.xnat.restlet.actions.importer.ImporterNotFoundException;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.nrg.xnat.restlet.util.XNATRestConstants;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

import com.google.common.collect.Multimap;

public class Importer extends SecureResource {
	private static final String CRLF = "\r\n";
	private static final String HTTP_SESSION_LISTENER = "http-session-listener";
	public Importer(Context context, Request request, Response response) {
		super(context, request, response);
				
	}

	public boolean allowGet(){
		return false;
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
			
			final Map<String,Object> params=new Hashtable<String,Object>();
			
			String handler=null;
			String listenerControl=null;
			boolean httpSessionListener=false;
			
						
			//maintain parameters
			final Form f = getQueryVariableForm();
			for(final String key:f.getNames()){
				if(key.equals(ImporterHandlerA.IMPORT_HANDLER_ATTR)){
					handler=f.getFirstValue(ImporterHandlerA.IMPORT_HANDLER_ATTR);
				}else if(key.equals(XNATRestConstants.TRANSACTION_RECORD_ID)){
					listenerControl=f.getFirstValue(XNATRestConstants.TRANSACTION_RECORD_ID);
				}else if(key.equals("src")){
					for(String src:f.getValuesArray("src")){
						fw.add(retrievePrestoreFile(src));
					}
				}else if(key.equals(HTTP_SESSION_LISTENER)){
					listenerControl=f.getFirstValue(HTTP_SESSION_LISTENER);
					httpSessionListener=true;
				}else{
					params.put(key,f.getFirstValue(key));
				}
			}				
			

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
				}else if(key.equals("transaction_id")){
					listenerControl=f.getFirstValue("transaction_id");
				}else if(key.equals(PREARCHIVE)){
					
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
			
			final boolean archive=!(isQueryVariableTrue(PREARCHIVE));
			
			final SessionImporter importer= new SessionImporter(listenerControl, user, project_id, session, overwriteV, fw.get(0), additionalValues,archive);
			
			if(!StringUtils.isEmpty(listenerControl)){
				final StatusList sq = new StatusList();
				importer.addStatusListener(sq);
				
				storeStatusList(listenerControl, sq);
			}
			
			final Multimap<String,Object> response=importer.call();
			
			this.returnSuccessfulCreateFromList(GoogleUtils.getFirstParam(response, SessionImporter.RESPONSE_URL).toString());
		} catch (ClientException e) {
			this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage());
		} catch (ServerException e) {
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
		}catch (IllegalArgumentException e) {
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
		}
	}

	private void storeStatusList(final String transaction_id,final StatusList sl) throws IllegalArgumentException{
		this.retrieveSQManager().storeStatusQueue(transaction_id, sl);
	}
	
	private PersistentStatusQueueManagerI retrieveSQManager(){
		return new HTTPSessionStatusManagerQueue(this.getHttpSession());
	}

	
}
