/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.xnat.restlet.actions;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Callable;

import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.status.StatusListenerI;
import org.nrg.status.StatusProducer;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.archive.ArchivingException;
import org.nrg.xnat.archive.PrearcSessionArchiver;
import org.nrg.xnat.helpers.PrearcImporterHelper;
import org.nrg.xnat.helpers.PrearcImporterHelper.PrearcSession;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.nrg.xnat.turbine.utils.ImageUploadHelper;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

public class SessionImporter extends StatusProducer implements Callable<Multimap<String, Object>> {
	public static final String APPEND = "append";

	public static final String DELETE = "delete";

	public static final String RESPONSE_URL = "URL";
	
	private final Boolean allowDataDeletion;
	
	private final Boolean overwrite;
	
	private final Boolean archive;
	
	private final FileWriterWrapperI fw;
	
	private final Object uID;
	
	private final XDATUser user;
	
	private final XnatImagesessiondata expt;
	
	private final String project_id;
	
	private  Map<String,Object> additionalValues;
	
	/**
	 * 
	 * @param listenerControl
	 * @param u
	 * @param session
	 * @param overwriteV:   'prevent' means do not touch (DEFAULT), 
	 *                      'append' means overwrite, but preserve un-modified content (don't delete anything)
	 *                      'delete' means delete the pre-existing content.
	 * @param additionalValues: should include project, subject_ID and label (if session is null)
	 */
	public SessionImporter(final Object listenerControl, final XDATUser u, final String project_id, final XnatImagesessiondata session, final String overwriteV, final FileWriterWrapperI fw, final Map<String,Object> additionalValues, final boolean archive){
		super(listenerControl);
		this.uID=listenerControl;
		
		this.user=u;
		
		this.fw=fw;
		
		if(overwriteV==null){
			this.allowDataDeletion=false;
			this.overwrite=false;
		}else{
			if(overwriteV.equalsIgnoreCase(APPEND)){
				this.allowDataDeletion=false;
				this.overwrite=true;
			}else if(overwriteV.equalsIgnoreCase(DELETE)){
				this.allowDataDeletion=true;
				this.overwrite=true;
			} else{
				this.allowDataDeletion=false;
				this.overwrite=false;
			}
		}
		
		this.expt=session;
		
		this.additionalValues=additionalValues;
		
		this.project_id=project_id;
		
		this.archive=archive;
	}

	public Multimap<String, Object> call() throws ClientException,ServerException{
		try {
			if(expt!=null && !overwrite){
				throw new ClientException("Session already exists.  overwrite=false.");
			}
			
			if(additionalValues==null)additionalValues=new Hashtable<String,Object>();
			
			if(expt!=null){
				additionalValues.put("ID", expt.getId());
				additionalValues.put("subject_ID", ((XnatImagesessiondata)expt).getSubjectId());
				additionalValues.put("project", expt.getProject());
				additionalValues.put("label", expt.getLabel());
			}
			//write file
			final PrearcImporterHelper importer=new PrearcImporterHelper(this.uID, user, this.fw, project_id, additionalValues);
			
			for(final StatusListenerI listener:this.getListeners()){
				importer.addStatusListener(listener);
			}
			
			Multimap<String, Object> results;
			try {
				results = importer.call();
			} catch (IOException e1) {
				throw new ServerException(e1.getMessage(), e1);
			} catch (Exception e1) {
				throw new ServerException(e1.getMessage(), e1);
			}
			
			final Collection<Object> sessions=results.get(ImageUploadHelper.SESSIONS_RESPONSE);
			
			if(sessions.size()>1){
				throw new ClientException("Upload included files for multiple imaging sessions.");
			}
			
			if(sessions.size()==0){
				throw new ClientException("Upload did not include parseable files for session generation.");
			}
			
			final PrearcSession session=((PrearcSession)Iterables.get(sessions, 0));
			
			if(archive){
			
			final PrearcSessionArchiver psa;
			try {
					psa = new PrearcSessionArchiver(session.getSessionDIR(), user, project_id, additionalValues, allowDataDeletion,overwrite);
			} catch (Exception e) {
				throw new ServerException(e.getMessage(), e);
			}

			for(final StatusListenerI listener:this.getListeners()){
				psa.addStatusListener(listener);
			}
			
			try {
				final URL url=psa.call();
				
				//this.completed("Process Complete.");
				
				final Multimap<String,Object> response=LinkedHashMultimap.create();
				response.put(RESPONSE_URL, url);
				return response;
			} catch (ArchivingException e) {
				throw new ServerException(e.getMessage(), e);
			}
			}else{
				final Multimap<String,Object> response=LinkedHashMultimap.create();
				response.put(RESPONSE_URL, session.getUrl());
				return response;
			}
		} catch (ClientException e) {
			this.failed(e.getMessage());
			throw e;
		} catch (ServerException e) {
			this.failed(e.getMessage());
			throw e;
		}
	}
	
	
}
