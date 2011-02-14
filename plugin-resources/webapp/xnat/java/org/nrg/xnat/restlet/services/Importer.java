package org.nrg.xnat.restlet.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.status.StatusList;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.helpers.transactions.HTTPSessionStatusManagerQueue;
import org.nrg.xnat.helpers.transactions.PersistentStatusQueueManagerI;
import org.nrg.xnat.helpers.uri.UriParserUtils;
import org.nrg.xnat.helpers.uri.UriParserUtils.DataURIA;
import org.nrg.xnat.helpers.uri.UriParserUtils.UriParser;
import org.nrg.xnat.restlet.actions.importer.ImporterHandlerA;
import org.nrg.xnat.restlet.actions.importer.ImporterNotFoundException;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.nrg.xnat.restlet.util.XNATRestConstants;
import org.nrg.xnat.utils.UserUtils;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.restlet.util.Template;

public class Importer extends SecureResource {
	private static final String CRLF = "\r\n";
	private static final String HTTP_SESSION_LISTENER = "http-session-listener";
    static org.apache.log4j.Logger logger = Logger.getLogger(Importer.class);
	public Importer(Context context, Request request, Response response) {
		super(context, request, response);

		this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
		this.getVariants().add(new Variant(MediaType.TEXT_HTML));
		this.getVariants().add(new Variant(MediaType.TEXT_XML));
	}

	public boolean allowGet(){
		return false;
	}

	@Override
	public boolean allowPost() {
		return true;
	}

	List<FileWriterWrapperI> fw=new ArrayList<FileWriterWrapperI>();

	String handler=null;
	String listenerControl=null;
	boolean httpSessionListener=false;

	final Map<String,Object> params=new Hashtable<String,Object>();

	List<String> response=null;

	public void loadParams(Form f) throws ClientException{
		for(final String key:f.getNames()){
			for(String v:f.getValuesArray(key)){
				handleParam(key,v);
			}
		}
	}

	@Override
	public void handleParam(String key, Object value) throws ClientException {
		if(key.equals(ImporterHandlerA.IMPORT_HANDLER_ATTR)){
			handler=(String)value;
		}else if(key.equals(XNATRestConstants.TRANSACTION_RECORD_ID)){
			listenerControl=(String)value;
		}else if(key.equals("src")){
				fw.add(retrievePrestoreFile((String)value));
		}else if(key.equals(HTTP_SESSION_LISTENER)){
			listenerControl=(String)value;
			httpSessionListener=true;
		}else{
			params.put(key,value);
		}
	}

	@Override
	public void handlePost() {
		//build fileWriters
		try {
			Representation entity = this.getRequest().getEntity();

			fw=this.getFileWritersAndLoadParams(entity);

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

			response= importer.call();
			returnDefaultRepresentation();
		} catch (ClientException e) {
			respondToException(e,(e.status!=null)?e.status:Status.CLIENT_ERROR_BAD_REQUEST);
		} catch (ServerException e) {
			respondToException(e,(e.status!=null)?e.status:Status.SERVER_ERROR_INTERNAL);
		}catch (IllegalArgumentException e) {
			respondToException(e,Status.CLIENT_ERROR_BAD_REQUEST);
		} catch (FileUploadException e) {
			respondToException(e,Status.CLIENT_ERROR_BAD_REQUEST);
		}
	}

		private void respondToException(Exception e, Status status) {
			logger.error("",e);
			if (this.requested_format!=null && this.requested_format.equalsIgnoreCase("HTML")){
				response = new ArrayList<String>();
				response.add(e.getMessage());
				returnDefaultRepresentation();
			}else{
				this.getResponse().setStatus(status, e.getMessage());
			}
		}

		@Override
		public Representation represent(Variant variant) throws ResourceException {
			final MediaType mt=overrideVariant(variant);
			if(mt.equals(MediaType.TEXT_HTML)){
				return buildHTMLresponse(response);
			}else{
				return new StringRepresentation(convertListURItoString(response), MediaType.TEXT_URI_LIST);
			}
		}

		private Representation buildHTMLresponse(List<String> response) {
			ArrayList<String> preList=new ArrayList<String>();
			ArrayList<String> archList=new ArrayList<String>();
			StringBuffer sb=new StringBuffer("<html><head>" +
			"<link type='text/css' rel='stylesheet' href='/xnat/style/xdat.css'>" +
			"<link type='text/css' rel='stylesheet' href='/xnat/style/xnat.css'>" +
			"</head><body class='yui-skin-sam'>"
			);
			for(final String s: response){
				DataURIA obj;
				try {
					obj = UriParserUtils.parseURI(s);
					if(obj instanceof UriParserUtils.ArchiveURI){
						//is an archive session
						archList.add(s);
					}else{
						//is a prearchive session
						preList.add(s);
					}
				} catch (MalformedURLException e) {
					// Do nothing, return empty text
				}
			}
			if (!(preList.isEmpty() && archList.isEmpty())) {
				sb.append("The following sessions have been uploaded:<br>");
				if (!preList.isEmpty()) {
					try {
						for (String s : preList) {
							String[] sarray=s.split("/");
							sb.append("<br>&nbsp;&nbsp;&nbsp;<a target='_parent' href='" + TurbineUtils.GetRelativePath(this.getHttpServletRequest()) +
										"/app/template/XDATScreen_prearchives.vm'>" + sarray[5] +
									  "</a> has been moved to the pre-archive for project " + sarray[3]);
						}
					} catch (Exception e) {
						sb.append("<br>A total of " + preList.size() + " session(s) have been moved to pre-archive.<br>");
					}
					String tempString=sb.toString();
					System.out.println("HELLO" + tempString);
				}
				if (!archList.isEmpty()) {
					try {
						for (String s : archList) {
							String[] sarray=s.split("/");
							sb.append("<br>&nbsp;&nbsp;&nbsp;<a target='_parent' href='" + TurbineUtils.GetRelativePath(this.getHttpServletRequest()) + "/data" + s +
										"'>" + sarray[7] +
									  "</a> has been archived for project " + sarray[3]);
						}
					} catch (Exception e) {
						sb.append("<br>A total of " + archList.size() + " session(s) have archived.<br>");
					}
				}
				sb.append("</body></html>");
			} else {
				sb.append("ERROR: The process could not be completed due to exceptions - <br>");
				for (String s : response) {
					sb.append("<br><span style='color:#DD0000'>" + s + "</span>");
				}
				sb.append("</body><script type='text/javascript'>" +
						"parent.document.getElementById('ex').style.display='none';" +
						"parent.toggleExtractSummary();" +
						"</script></html>");
			}
		return new StringRepresentation(sb.toString(),MediaType.TEXT_HTML);
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
			sb.append("/data").append(s).append(CRLF);
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
