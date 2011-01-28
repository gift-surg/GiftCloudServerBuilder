package org.nrg.xnat.restlet.services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nrg.action.ActionException;
import org.nrg.action.ClientException;
import org.nrg.status.StatusListenerI;
import org.nrg.status.StatusMessage;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xnat.archive.PrearcSessionArchiver;
import org.nrg.xnat.helpers.CallablesThread;
import org.nrg.xnat.helpers.PrearcImporterHelper;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.nrg.xnat.helpers.uri.UriParserUtils;
import org.nrg.xnat.helpers.uri.UriParserUtils.ArchiveURI;
import org.nrg.xnat.helpers.uri.UriParserUtils.DataURIA;
import org.nrg.xnat.helpers.uri.UriParserUtils.PrearchiveURI;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.nrg.xnat.restlet.util.RequestUtil;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class Archiver extends SecureResource {
	private static final String REDIRECT2 = "redirect";
	private static final String FOLDER = "folder";
	private static final String URL = "URL";
	private static final String ADDITIONAL_VALUES = "additionalValues";
	private static final String SRC = "src";
	private static final String OVERWRITE = "overwrite";
	private static final String PROJECT = "project";
	private static final String CRLF = "\r\n";
	private static final String DEST = "dest";
	private final Logger logger = LoggerFactory.getLogger(Archiver.class);
	public Archiver(Context context, Request request, Response response) {
		super(context, request, response);
				
	}
	
	public boolean allowGet(){
		return false;
	}

	@Override
	public boolean allowPost() {
		return true;
	}

			final Map<String,Object> additionalValues=new Hashtable<String,Object>();
			
			String project_id=null;
			String overwriteV=null;
			String timestamp=null;
			String[] sessionFolder=null;
			String dest=null;
	String redirect=null;
			String[] srcs=null;
			
	public void loadParams(Form f) {
			for(final String key:f.getNames()){
			if(f.getFirstValue(key)!=null){
				if(key.equals(PROJECT)){
					additionalValues.put("project",project_id);
				}else if(key.equals(PrearcUtils.PREARC_TIMESTAMP)){
					timestamp=f.getFirstValue(PrearcUtils.PREARC_TIMESTAMP);
				}else if(key.equals(PrearcUtils.PREARC_SESSION_FOLDER)){
					sessionFolder=f.getValuesArray(PrearcUtils.PREARC_SESSION_FOLDER);
				}else if(key.equals(OVERWRITE)){
					overwriteV=f.getFirstValue(OVERWRITE);
				}else if(key.equals(DEST)){
					dest=f.getFirstValue(DEST);
				}else if(key.equals(SRC)){
					srcs=f.getValuesArray(SRC);
				}else if(key.equals(REDIRECT2)){
					redirect=f.getFirstValue(REDIRECT2);
				}else{
					additionalValues.put(key,f.getFirstValue(key));
				}
			}
		}
	}

	@Override
	public void handlePost() {		
		//build fileWriters
		try {					
			Representation entity = this.getRequest().getEntity();
													
			if (RequestUtil.isMultiPartFormData(entity)) {
				loadParams(new Form(entity));
			}
			
			loadParams(getQueryVariableForm());			
			
			
			boolean allowDataDeletion=false;
			boolean overwrite=false;
			
			if(overwriteV==null){
				allowDataDeletion=false;
				overwrite=false;
			}else{
				if(overwriteV.equalsIgnoreCase(PrearcUtils.APPEND)){
					allowDataDeletion=false;
					overwrite=true;
				}else if(overwriteV.equalsIgnoreCase(PrearcUtils.DELETE)){
					allowDataDeletion=true;
					overwrite=true;
				} else{
					allowDataDeletion=false;
					overwrite=false;
				}
			}
			
			final List<Map<String,Object>> sessions=new ArrayList<Map<String,Object>>();
						
			project_id=PrearcImporterHelper.identifyProject(additionalValues);
			
			if((project_id==null || timestamp==null || sessionFolder==null) && (srcs==null)){
				this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Unknown prearchive session.");
				return;
			}else if(srcs!=null){
				for(final String src: srcs){
					final Map<String,Object> session= new HashMap<String,Object>();
					DataURIA data;
					try {
						data = UriParserUtils.parseURI(src);
					} catch (MalformedURLException e) {
						this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage());
						return;
					}
					if(data instanceof ArchiveURI){
						this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid src URI (" + src +")");
						return;
					}
					session.putAll(data.getProps());
					session.put(ADDITIONAL_VALUES,additionalValues);
					session.put(URL, src);
					sessions.add(session);
				}
			}else if(dest!=null){
				DataURIA data;
				try {
					data = UriParserUtils.parseURI(dest);
				} catch (MalformedURLException e) {
					this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage());
					return;
				}
				if(data instanceof PrearchiveURI){
					this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid dest URI (" + dest +")");
					return;
				}
				additionalValues.putAll(data.getProps());
			}else{
				for(final String s:sessionFolder){
					final Map<String,Object> session= new HashMap<String,Object>();
					session.put(PROJECT,project_id);
					session.put(PrearcUtils.PREARC_TIMESTAMP,timestamp);
					session.put(PrearcUtils.PREARC_SESSION_FOLDER,s);
					session.put(URL, "/prearchive/projects/"+ project_id + "/" + timestamp + "/" + s);

					session.put(ADDITIONAL_VALUES,additionalValues);
					sessions.add(session);
				}
			}
			
			//validate specified folders
			for(final Map<String,Object> map: sessions){
				final String p=PrearcImporterHelper.identifyProject(map);
				final String prearc=getPrearchivePath(p);
				final String time=(String)map.get(PrearcUtils.PREARC_TIMESTAMP);
				final String ses=(String)map.get(PrearcUtils.PREARC_SESSION_FOLDER);
				
				if(p==null || prearc==null || time==null || ses==null){
					throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, map.get(URL) + " not found.");
				}
				
				final File ps = new File(new File(prearc,time),ses);
				if(!ps.exists()){
					throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, map.get(URL) + " not found.");
				}
				
				map.put(FOLDER, ps);
			}
			
			//register actions
			for(final Map<String,Object> map: sessions){
				preRegisterAction((String)map.get(URL));
			}
			
			
			if(sessions.size()==1){
				final PrearcSessionArchiver archiver=buildArchiver(sessions.get(0),PrearcImporterHelper.identifyProject(sessions.get(0)),allowDataDeletion,overwrite);
				archiver.addStatusListener(new ActionListener());
				String _return =null;
				try {
					_return= archiver.call().toString();
				}catch (ActionException e) {
					logger.debug("", e);
					throwResourceException(e);
				} finally {
						archiver.dispose();
				}
				
				if(!StringUtils.isEmpty(redirect) && redirect.equalsIgnoreCase("true")){
					getResponse().redirectSeeOther(getContextPath()+_return);
				}else{
				getResponse().setEntity(_return+CRLF, MediaType.TEXT_URI_LIST);
				}
				return;
				
			}else{
				CallablesThread<String> thread = new CallablesThread<String>();
				for(final Map<String,Object> map: sessions){
					PrearcSessionArchiver archiver=buildArchiver(map,PrearcImporterHelper.identifyProject(sessions.get(0)),allowDataDeletion,overwrite);
					archiver.addStatusListener(new ActionListener());
					thread.addCallable(archiver);
				}
				
				thread.start();
				
				final Response response = getResponse();
				response.setEntity("", MediaType.TEXT_URI_LIST);
			}
		} catch (ResourceException e) {
			logger.error("",e);
			this.getResponse().setStatus(e.getStatus(), e.getMessage());
		} catch (IllegalArgumentException e) {
			logger.error("",e);
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
		}
	}
	
	private void throwResourceException(ActionException e) throws ResourceException{
		if(e.status!=null){
			throw new ResourceException(e.status, e.getMessage(), e);
		}else if(e instanceof ClientException){
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST,e.getMessage(),e);
		}else{
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,e.getMessage(),e);
		}
	}
	
	private void preRegisterAction(final String sessionURL){
		//TODO
	}
	
	public static class ActionListener implements StatusListenerI{
		private boolean initd=false;
		
		public ActionListener(){
			
		}
		
		@Override
		public void notify(StatusMessage message) {
			if(message.getStatus().equals(StatusMessage.Status.FAILED)){
				handleFailure();
			}else if(message.getStatus().equals(StatusMessage.Status.COMPLETED)){
				handleComplete();
			}else if(message.getStatus().equals(StatusMessage.Status.PROCESSING) && !initd){
				initd=true;
				handleProcessing();
			}
		}
		
		public void handleFailure(){
			//TODO
		}
		
		public void handleComplete(){
			//TODO
		}
		
		public void handleProcessing(){
			//TODO
		}
		
	}
	
	private String getPrearchivePath(final String project_id) throws ResourceException{
		XnatProjectdata proj = XnatProjectdata.getXnatProjectdatasById(project_id, user, false);
		if(proj==null){
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,"Unknown project: " + project_id);
		}
		return proj.getPrearchivePath();
	}
	
	private PrearcSessionArchiver buildArchiver(final Map<String,Object> session,final String project, final Boolean allowDataDeletion,final Boolean overwrite) throws ResourceException{
		return buildArchiver((File)session.get(FOLDER),project,(Map<String,Object>)session.get(ADDITIONAL_VALUES),allowDataDeletion,overwrite,(String)session.get(URL));
	}
	
	private PrearcSessionArchiver buildArchiver(final File sessionDir,final String project_id,Map<String,Object> additionalValues, final Boolean allowDataDeletion,final Boolean overwrite, final String url) throws ResourceException {
		final PrearcSessionArchiver archiver;
		try {
			archiver = new PrearcSessionArchiver(sessionDir, user, project_id, additionalValues, allowDataDeletion,overwrite);
		} catch (FileNotFoundException e) {
			logger.debug("user attempted to archive session with no XML", e);
			throw new ResourceException(Status.CLIENT_ERROR_CONFLICT,
					"Session metadata could not be read. Send a build request and try again.", e);
		} catch (IOException e) {
			logger.error("unable to read session document", e);
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Unable to read session document", e);
		} catch (SAXException e) {
			logger.error("error in session document", e);
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					"Unable to parse session document", e);
		}
		
		return archiver;
	}
}
