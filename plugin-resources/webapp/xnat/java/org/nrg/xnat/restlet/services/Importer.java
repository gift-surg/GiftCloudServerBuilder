package org.nrg.xnat.restlet.services;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
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
import org.restlet.data.*;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.restlet.util.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class Importer extends SecureResource {
	private static final String CRLF = "\r\n";
	private static final String HTTP_SESSION_LISTENER = "http-session-listener";
	private static final String JAVA = "Java";
	public static final String APPLET_FLAG = "applet";
	private final Logger logger = LoggerFactory.getLogger(Importer.class);

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
		    final Request request = getRequest();
		    if (logger.isDebugEnabled()) {
		        final ClientInfo client = request.getClientInfo();
		        final StringBuilder sb = new StringBuilder("handling POST from ");
		        sb.append(client.getAddress()).append(":").append(client.getPort());
		        sb.append(" ").append(client.getAgent());
		        logger.debug(sb.toString());
		    }
		    
			Representation entity = request.getEntity();

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

			
			if(handler==null && entity!=null){
				if(APPLICATION_DICOM.equals(entity.getMediaType()) || 
						APPLICATION_XMIRC.equals(entity.getMediaType()) || 
						APPLICATION_XMIRC_DICOM.equals(entity.getMediaType())){
					handler=ImporterHandlerA.GRADUAL_DICOM_IMPORTER;
				}
			}

			ImporterHandlerA importer;
			
			this.addAppletFlagToParams();
			
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
						
			if(entity!=null && APPLICATION_XMIRC.equals(entity.getMediaType())){
				returnString("OK", Status.SUCCESS_OK);
				return;
			}
			
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

    protected void respondToException(Exception e, Status status) {
		logger.error("",e);
		if (this.requested_format!=null && this.requested_format.equalsIgnoreCase("HTML")) {
			response = new ArrayList<String>();
			response.add(e.getMessage());
			returnDefaultRepresentation();
		} else {
			this.getResponse().setStatus(status, e.getMessage());
		}
	}

	/**
	 * Add an attribute that tells users of the global parameter 
	 * if the file is being uploaded via the Upload Applet.
	 * 
	 * Determining whether a session was uploaded via the applet is 
	 * done by inspecting the individual products of User Agent string.
	 * 
	 * Upload applet sessions seem to come in with a the first product is
	 * is the browser string (Mozilla etc.) and the second, the operation
	 * system identifier is "Java". So if the second product is "Java", 
	 * the session came in from the Upload Applet. 
	 * 
	 * Another case when the name of a product is "Java" is when a Java
	 * program makes calls to the REST API, for example, the functional
	 * tests in the xnat_test package. In this case however, the first
	 * product, the browser identifier is "Java" and not the second.  
	 * 
	 * TL;DR If the second product is "Java", the session came in via the 
	 * upload applet
	 * 
	 */
	private void addAppletFlagToParams() {
		List<Product> ps = this.getRequest().getClientInfo().getAgentProducts();
		String appletFound = "false";
		if (ps.size() > 2) {
			if (ps.get(1).getName().equals(Importer.JAVA)) {
				appletFound = "true";	
			}
		}
			
		if (!params.containsKey(Importer.APPLET_FLAG)) {
			this.params.put(Importer.APPLET_FLAG, appletFound);
		}
		else {
			// the "applet" query string parameter has already been passed,
			// don't override it. 
		}
 	}
	
	public static Boolean getUploadFlag(Map<String, Object> params) {
		if (params.containsKey(Importer.APPLET_FLAG)) {
			String flag = (String) params.get(Importer.APPLET_FLAG);
			return flag.equals("true");
		}
		else {
			return new Boolean(false);
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
			final ArrayList<String> preList=new ArrayList<String>();
			final ArrayList<String> archList=new ArrayList<String>();
			final StringBuilder sb=new StringBuilder("<html><head>");
			sb.append("<link type='text/css' rel='stylesheet' href='");
			sb.append(TurbineUtils.GetRelativePath(this.getHttpServletRequest()));
			sb.append("/style/xdat.css'>");
			sb.append("<link type='text/css' rel='stylesheet' href='/xnat/style/xnat.css'>");
			sb.append("</head><body class='yui-skin-sam'>");
			
			for(final String s: response){
				final DataURIA obj;
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
							sb.append("<br>&nbsp;&nbsp;&nbsp;<a target='_parent' href='");
							sb.append(TurbineUtils.GetRelativePath(this.getHttpServletRequest()));
							sb.append("/app/template/XDATScreen_prearchives.vm'>");
							sb.append(preList.size());
							sb.append(" sessions(s)</a> has been moved to the pre-archive");
					} catch (Exception e) {
						sb.append("<br>A total of ");
						sb.append(preList.size());
						sb.append(" session(s) have been moved to pre-archive.<br>");
					}
				}
				if (!archList.isEmpty()) {
					try {
						for (final String s : archList) {
							final String[] sarray=s.split("/");
							sb.append("<br>&nbsp;&nbsp;&nbsp;<a target='_parent' href='");
							sb.append(TurbineUtils.GetRelativePath(this.getHttpServletRequest()));
							sb.append("/data");
							sb.append(s);
							sb.append("'>");
							sb.append(sarray[7]);
							sb.append("</a> has been archived for project " + sarray[3]);
						}
					} catch (Exception e) {
						sb.append("<br>A total of ");
						sb.append(archList.size());
						sb.append(" session(s) have archived.<br>");
					}
				}
			sb.append("<script type='text/javascript'>");
			sb.append("if(window.parent.proceed!=undefined){window.parent.proceed();}");
			sb.append("</script>");
				sb.append("</body></html>");
			} else {
				sb.append("ERROR: The process could not be completed due to exceptions - <br>");
				for (final String s : response) {
					sb.append("<br><span style='color:#DD0000'>");
					sb.append(s);
					sb.append("</span>");
				}
				sb.append("Your data may be available in the prearchive for your review.</body><script type='text/javascript'>");
				sb.append("parent.document.getElementById('ex').style.display='none';");
				sb.append("parent.toggleExtractSummary();");
				sb.append("</script></html>");
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

		public void write(File f) throws IOException {
			FileUtils.moveFile(stored, f);
		}

		public String getName() {
			return stored.getName();
		}

		public InputStream getInputStream() throws IOException {
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
			sb.append(wrapPartialDataURI(s)).append(CRLF);
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
