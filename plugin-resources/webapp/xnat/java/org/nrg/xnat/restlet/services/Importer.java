package org.nrg.xnat.restlet.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.status.StatusList;
import org.nrg.xnat.helpers.transactions.HTTPSessionStatusManagerQueue;
import org.nrg.xnat.helpers.transactions.PersistentStatusQueueManagerI;
import org.nrg.xnat.helpers.uri.UriParserUtils.UriParser;
import org.nrg.xnat.restlet.actions.importer.ImporterHandlerA;
import org.nrg.xnat.restlet.actions.importer.ImporterNotFoundException;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.nrg.xnat.restlet.util.RequestUtil;
import org.nrg.xnat.restlet.util.XNATRestConstants;
import org.nrg.xnat.utils.UserUtils;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.util.Template;

public class Importer extends SecureResource {
	private static final String CRLF = "\r\n";
	private static final String HTTP_SESSION_LISTENER = "http-session-listener";
    static org.apache.log4j.Logger logger = Logger.getLogger(Importer.class);
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

	List<FileWriterWrapperI> fw=Collections.emptyList();
			
			String handler=null;
			String listenerControl=null;
			boolean httpSessionListener=false;
			
	final Map<String,Object> params=new Hashtable<String,Object>();
						
	public void loadParams(Form f) throws ClientException{
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
	}
			
	@Override
	public void handlePost() {
		//build fileWriters
		try {
			Representation entity = this.getRequest().getEntity();

			fw=this.getFileWriters(entity);

			if (RequestUtil.isMultiPartFormData(entity)) {
				loadParams(new Form(entity));
			}

			//maintain parameters
			loadParams(getQueryVariableForm());

			if(fw.size()==0){
				this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Unable to identify upload format.");
				return;
			}
			
			if(fw.size()>1){
				this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Importer is limited to one uploaded resource at a time.");
				return;
			}
			
//			XnatImagesessiondata session=null;
//
//			if(session_id!=null){
//				session=XnatImagesessiondata.getXnatImagesessiondatasById(session_id, user, false);
//			}
//
//			if(session==null){
//				if(project_id!=null){
//					session=(XnatImagesessiondata)XnatExperimentdata.GetExptByProjectIdentifier(project_id, session_id, user, false);
//				}
//			}
//
//			if(session==null){
//				if(project_id==null || subject_id==null || session_id==null){
//					this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "New sessions require a project, subject and session id.");
//					return;
//				}
//			}
			
			ImporterHandlerA importer;
			try {
				importer = ImporterHandlerA.buildImporter(handler, listenerControl, user, fw.get(0), params);
			} catch (SecurityException e) {
				logger.error("",e);
				throw new ServerException(e.getMessage(),e);
			} catch (IllegalArgumentException e) {
				logger.error("",e);
				throw new ClientException(Status.CLIENT_ERROR_BAD_REQUEST,e.getMessage(),e);
			} catch (NoSuchMethodException e) {
				logger.error("",e);
				throw new ClientException(Status.CLIENT_ERROR_BAD_REQUEST,e.getMessage(),e);
			} catch (InstantiationException e) {
				logger.error("",e);
				throw new ServerException(e.getMessage(),e);
			} catch (IllegalAccessException e) {
				logger.error("",e);
				throw new ServerException(e.getMessage(),e);
			} catch (InvocationTargetException e) {
				logger.error("",e);
				throw new ServerException(e.getMessage(),e);
			} catch (ImporterNotFoundException e) {
				logger.error("",e);
				throw new ClientException(Status.CLIENT_ERROR_NOT_FOUND,e.getMessage(),e);
				}
				
			
			if(httpSessionListener){
				if(StringUtils.isEmpty(listenerControl)){
					getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,"'" + XNATRestConstants.TRANSACTION_RECORD_ID+ "' is required when requesting '" + HTTP_SESSION_LISTENER + "'.");
					return;
				}
				final StatusList sq = new StatusList();
				importer.addStatusListener(sq);
				
				storeStatusList(listenerControl, sq);
			}
			
			List<String> response = importer.call();
			
			getResponse().setEntity(convertListURItoString(response), MediaType.TEXT_URI_LIST);
		} catch (ClientException e) {
			logger.error("",e);
			this.getResponse().setStatus((e.status!=null)?e.status:Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage());
		} catch (ServerException e) {
			logger.error("",e);
			this.getResponse().setStatus((e.status!=null)?e.status:Status.SERVER_ERROR_INTERNAL, e.getMessage());
		}catch (IllegalArgumentException e) {
			logger.error("",e);
			this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage());
		} catch (FileUploadException e) {
			logger.error("",e);
			this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, e.getMessage());
		}
	}

	public FileWriterWrapperI retrievePrestoreFile(final String src) throws ClientException{
		
		Map<String,Object> map=new UriParser("/user/cache/resources/{XNAME}/files/{FILE}",Template.MODE_EQUALS).readUri(src);
		
		if(!map.containsKey("XNAME") || !map.containsKey("FILE")){
			throw new ClientException(Status.CLIENT_ERROR_BAD_REQUEST,"src uri is invalid.",new Exception());
		}
		
		File f=UserUtils.getUserCacheFile(user, (String)map.get("XNAME"), (String)map.get("FILE"));
		
		if(f.exists()){
			return new UserCacheFile(f);
		}else{
			throw new ClientException(Status.CLIENT_ERROR_NOT_FOUND,"Unknown src file.",new Exception());
		}
	}
	
	class UserCacheFile implements FileWriterWrapperI{
		final File stored;
		
		public UserCacheFile(final File f){
			stored=f;
		}
		
		public void write(File f) throws IOException, Exception {
			FileUtils.moveFile(stored, f);
		}

		public String getName() {
			return stored.getName();
		}

		public InputStream getInputStream() throws IOException, Exception {
			return new FileInputStream(stored);
		}

		public void delete() {
			if(stored.exists())FileUtils.deleteQuietly(stored);
		}

		@Override
		public UPLOAD_TYPE getType() {
			return UPLOAD_TYPE.OTHER;
		}};

	public String convertListURItoString(final List<String> response){
		StringBuffer sb = new StringBuffer();
		for(final String s:response){
			sb.append(s).append(CRLF);
		}

		return sb.toString();
	}

	private void storeStatusList(final String transaction_id,final StatusList sl) throws IllegalArgumentException{
		this.retrieveSQManager().storeStatusQueue(transaction_id, sl);
	}
	
	private PersistentStatusQueueManagerI retrieveSQManager(){
		return new HTTPSessionStatusManagerQueue(this.getHttpSession());
	}

	
}
