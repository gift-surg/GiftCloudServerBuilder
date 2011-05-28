// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.DefaultFileItemFactory;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.nrg.action.ActionException;
import org.nrg.action.ClientException;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.utils.AccessLogger;
import org.nrg.xdat.turbine.utils.PopulateItem;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFT;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.schema.Wrappers.XMLWrapper.SAXReader;
import org.nrg.xft.utils.zip.ZipUtils;
import org.nrg.xnat.helpers.FileWriterWrapper;
import org.nrg.xnat.restlet.representations.CSVTableRepresentation;
import org.nrg.xnat.restlet.representations.HTMLTableRepresentation;
import org.nrg.xnat.restlet.representations.ItemHTMLRepresentation;
import org.nrg.xnat.restlet.representations.ItemXMLRepresentation;
import org.nrg.xnat.restlet.representations.JSONTableRepresentation;
import org.nrg.xnat.restlet.representations.XMLTableRepresentation;
import org.nrg.xnat.restlet.representations.XMLXFTItemRepresentation;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.nrg.xnat.restlet.util.RequestUtil;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Parameter;
import org.restlet.data.Preference;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.resource.FileRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.restlet.util.Series;
import org.xml.sax.SAXParseException;

import com.noelios.restlet.http.HttpConstants;

@SuppressWarnings("deprecation")
public abstract class SecureResource extends Resource {
	private static final String COMPRESSION = "compression";

	public static class FileUploadException extends Exception {
		private static final long serialVersionUID = 1L;

		public FileUploadException(String message, Exception e) {
			super(message,e);
		}

	}

	private static final String CONTENT_DISPOSITION = "Content-Disposition";

	private static final String ACTION = "action";

	public static final String USER_ATTRIBUTE = "user";

	static Logger logger = Logger.getLogger(SecureResource.class);
	public Hashtable<String, String> fieldMapping = new Hashtable<String, String>();
	
	// TODO: these should be proper extension types: application/x-xList, application/x-xcat+xml, application/x-xar
	public static final MediaType APPLICATION_XLIST = MediaType.register(
			"application/xList", "XNAT Listing");
	
	public static final MediaType APPLICATION_XCAT = MediaType.register(
			"application/xcat", "XNAT Catalog");
	
	public static final MediaType APPLICATION_XAR = MediaType.register(
			"application/xar", "XAR Archive");
	
	public static final MediaType APPLICATION_DICOM = MediaType.register(
	        "application/dicom", "Digital Imaging and Communications in Medicine");

	
	public static final MediaType APPLICATION_XMIRC = MediaType.register(
	        "application/x-mirc", "MIRC");
	
	public static final MediaType APPLICATION_XMIRC_DICOM = MediaType.register(
	        "application/x-mirc-dicom", "MIRC DICOM");
	
	protected List<String> actions=null;
	protected String userName=null;
	protected XDATUser user =null;
	protected String requested_format = null;
	public String filepath = null;
	
	public SecureResource(Context context, Request request, Response response) {
		super(context, request, response);
		
		requested_format = getQueryVariable("format");

		// expects that the user exists in the session (either via traditional
		// session or set via the XnatSecureGuard
		user = (XDATUser) getRequest().getAttributes().get(USER_ATTRIBUTE);

		filepath = getRequest().getResourceRef().getRemainingPart();
		if (filepath != null) {
			if (filepath.indexOf("?") > -1) {
				filepath = filepath.substring(0, filepath.indexOf("?"));
			}
			if (filepath.startsWith("/")) {
				filepath = filepath.substring(1);
				}
		}
		logAccess();
	}


	public void logAccess() {
		String url = this.getRequest().getResourceRef().toString();

		if (Method.GET.equals(getRequest().getMethod())				&& url.indexOf("resources/SNAPSHOTS") > -1) {
			// skip logging for snapshots
			}else{
			String login = "";
			if (user != null) {
				login = user.getLogin();
			}

			AccessLogger.LogServiceAccess(login, getRequest().getClientInfo()					.getAddress(), getRequest().getMethod() + " " + url, "");
			}
		}
		
	public MediaType getRequestedMediaType() {
		if (this.requested_format != null) {
			if (this.requested_format.equals("xml")) {
				return MediaType.TEXT_XML;
			} else if (this.requested_format.equals("json")) {
				return MediaType.APPLICATION_JSON;
			} else if (this.requested_format.equals("csv")) {
				return MediaType.APPLICATION_EXCEL;
			} else if (this.requested_format.equals("html")) {
				return MediaType.TEXT_HTML;
			} else if (this.requested_format.equals("zip")) {
				return MediaType.APPLICATION_ZIP;
			} else if (this.requested_format.equals("tar.gz")) {
				return MediaType.APPLICATION_GNU_TAR;
			}else if(this.requested_format.equals("xList")){
				return APPLICATION_XLIST;
			}else if(this.requested_format.equalsIgnoreCase("xcat")){
				return APPLICATION_XCAT;
			}else if(this.requested_format.equalsIgnoreCase("xar")){
				return APPLICATION_XAR;
			}
		}
		return null;
	}

	public boolean isHTMLRequest() {
		MediaType rmt = getRequestedMediaType();

		if (rmt != null && rmt.equals(MediaType.TEXT_HTML)) {
			return true;
		}

		for (Preference<MediaType> pref : getRequest().getClientInfo().getAcceptedMediaTypes()) {
			if (pref.getMetadata().equals(MediaType.TEXT_HTML)) {
				return true;
			}
		}

		return false;
	}
	
	Form f= null;
	public Form getQueryVariableForm(){
		if(f==null){
			f= getRequest().getResourceRef().getQueryAsForm();
		}
		return f;
	}
	
	public Form getBodyAsForm(){
		Representation entity = this.getRequest().getEntity();
		
		if (RequestUtil.isMultiPartFormData(entity)) {
			return new Form(entity);
		}
		
		return null;
	}
	
	public String getQueryVariable(String key){
		Form f = getQueryVariableForm();
		if (f != null) {
			return f.getFirstValue(key);
		}
		return null;
	}
	
	public boolean containsQueryVariable(String key){
		if(getQueryVariable(key)==null)
		{
			return false;
		}else{
			return true;
			}
		}
		
	public boolean isQueryVariable(String key, String value,			boolean caseSensitive) {
		if (this.getQueryVariable(key) != null) {
			if ((caseSensitive && this.getQueryVariable(key).equals(value))					|| (!caseSensitive && this.getQueryVariable(key)							.equalsIgnoreCase(value))) {
				return true;
			}
		}
		return false;
	}
	
	public String[] getQueryVariables(String key) {
		Form f = getQueryVariableForm();
		if (f != null) {
			return f.getValuesArray(key);
			}
		return null;
	}

	public MediaType overrideVariant(Variant v) {
		MediaType rmt = this.getRequestedMediaType();
		if (rmt != null) {
			return rmt;
		}

		if(v!=null){
			return v.getMediaType();
		}else{
			return MediaType.TEXT_XML;
		}
	}
	
	public Representation representTable(XFTTable table, MediaType mt,Hashtable<String,Object> params){
		return representTable(table,mt,params,null);
	}
	
	public Representation representTable(XFTTable table, MediaType mt,Hashtable<String,Object> params,Map<String,Map<String,String>> cp){
		if(table!=null){
			if(this.getQueryVariable("sortBy")!=null){
				final String sortBy=this.getQueryVariable("sortBy");
				table.sort(Arrays.asList(StringUtils.split(sortBy, ',')));
				if(this.isQueryVariable("sortOrder","DESC",false)){
					table.reverse();
				}
			}
			
	        if (mt.equals(MediaType.TEXT_XML)){
				return new XMLTableRepresentation(table,cp,params,MediaType.TEXT_XML);
			}else if (mt.equals(MediaType.APPLICATION_JSON)){
				return new JSONTableRepresentation(table,cp,params,MediaType.APPLICATION_JSON);
			}else if (mt.equals(MediaType.APPLICATION_EXCEL)){
				return new CSVTableRepresentation(table,cp,params,MediaType.APPLICATION_EXCEL);
			}else if (mt.equals(APPLICATION_XLIST)){
				Representation rep= new HTMLTableRepresentation(table,cp,params,MediaType.TEXT_HTML,false);
				rep.setMediaType(MediaType.TEXT_HTML);
				return rep;
			}else{
				return new HTMLTableRepresentation(table,cp,params,MediaType.TEXT_HTML,true);
			}
		}else{
			Representation rep = new StringRepresentation("", mt);
			rep.setExpirationDate(Calendar.getInstance().getTime());
			return rep;
		}
		}
	
	public String getCurrentURI(){
		return this.getRequest().getResourceRef().getPath();
	}
	
	public void returnDefaultRepresentation(){
		getResponse().setEntity(getRepresentation(getVariants().get(0)));
        Representation selectedRepresentation = getResponse().getEntity();
        if (getRequest().getConditions().hasSome()) {
			final Status status = getRequest().getConditions().getStatus(					getRequest().getMethod(), selectedRepresentation);

            if (status != null) {
                getResponse().setStatus(status);
                getResponse().setEntity(null);
            }
        }
	}
	
	public Representation representItem(XFTItem item, MediaType mt,			Hashtable<String, Object> metaFields, boolean allowDBAccess,			boolean allowSchemaLocation) {
		if (item != null) {
			if (mt.equals(MediaType.TEXT_XML)) {
				return new XMLXFTItemRepresentation(item, MediaType.TEXT_XML,						metaFields, allowDBAccess, allowSchemaLocation);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	public Representation representItem(XFTItem item, MediaType mt){

		if (mt.equals(MediaType.TEXT_HTML)) {
			try {
				return new ItemHTMLRepresentation(item, MediaType.TEXT_HTML, getRequest(), user);
			} catch (Exception e) {
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e);
				return null;
			}
		} else {
			return new ItemXMLRepresentation(item, MediaType.TEXT_XML, true,!this.isQueryVariableTrue("concealHiddenFields"));
		}
	}
	
	public FileRepresentation representFile(File f,MediaType mt){
		if(f.getName().toLowerCase().endsWith(".gif")){
			mt = MediaType.IMAGE_GIF;
		}else if(f.getName().toLowerCase().endsWith(".jpeg")){
			mt = MediaType.IMAGE_JPEG;
		}else if(f.getName().toLowerCase().endsWith(".xml")){
			mt = MediaType.TEXT_XML;
		}else{
			if(mt.equals(MediaType.TEXT_XML) && !f.getName().toLowerCase().endsWith(".xml")){
				mt=MediaType.ALL;
			}else{
				mt=MediaType.APPLICATION_OCTET_STREAM;
			}
		}

		this.setContentDisposition(String.format("attachment; filename=\"%s\";",f.getName()));
		
		FileRepresentation fr= new FileRepresentation(f,mt);
		fr.setModificationDate(new Date(f.lastModified()));
		
		return fr;
	}

	public boolean allowDataDeletion() {
		if (this.getQueryVariable("allowDataDeletion") != null				&& this.getQueryVariable("allowDataDeletion").equals("true")) {
			return true;
		} else {
			return false;
		}
	}

	public boolean populateFromDB() {
		if (this.getQueryVariable("populateFromDB") == null) {
			return true;
		} else if (this.getQueryVariable("populateFromDB").equals("true")) {
			return true;
		} else {
			return false;
		}
	}
	
	protected boolean completeDocument=false;
	
	public XFTItem loadItem(String dataType, boolean parseFileItems)throws IOException, SAXParseException {
		return loadItem(dataType, parseFileItems, null);
	}

	public XFTItem loadItem(String dataType, boolean parseFileItems,XFTItem template) throws IOException, SAXParseException {
        XFTItem item = null;
		if (template != null && this.populateFromDB()) {
			item = template;
		}
        
		String req_format=getQueryVariable("req_format");
		Representation entity = this.getRequest().getEntity();
		if(req_format==null){
			if ((entity == null || (entity.getSize() == 0) || (entity.getSize() == -1) || (entity.getMediaType() != null					&& entity.getMediaType().getName().equals(							MediaType.MULTIPART_FORM_DATA.getName()) && !(parseFileItems)))) {
				req_format="";
			} else {
				req_format = "xml";
			}
		}
		
		if (req_format.equals("xml") && parseFileItems && !this.isQueryVariableTrue("inbody")) {
			if (entity != null && entity.getMediaType() != null && entity.getMediaType().getName().equals(MediaType.MULTIPART_FORM_DATA.getName())) {
	        try {
					org.apache.commons.fileupload.DefaultFileItemFactory factory = new DefaultFileItemFactory();
					org.restlet.ext.fileupload.RestletFileUpload upload = new RestletFileUpload(factory);

					List<FileItem> items = upload.parseRequest(this							.getRequest());
				
					for (FileItem fi : items) {
						if (fi.getName().endsWith(".xml")) {
							SAXReader reader = new SAXReader(user);
							if (item != null) {
								reader.setTemplate(item);
							}
							try {
								item = reader.parse(fi.getInputStream());

								if(!reader.assertValid()){
									throw reader.getErrors().get(0);
								}
								if (XFT.VERBOSE) {
									System.out.println("Loaded XML Item:" + item.getProperName());
								}
								if (item != null) {
									completeDocument = true;
								}
							} catch (SAXParseException e) {
								e.printStackTrace();
								this										.getResponse()										.setStatus(												Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY,												e.getMessage());
								throw e;
							} catch (IOException e) {
								e.printStackTrace();
								this.getResponse().setStatus(										Status.SERVER_ERROR_INTERNAL);
							} catch (Exception e) {
								e.printStackTrace();
								this.getResponse().setStatus(										Status.SERVER_ERROR_INTERNAL);
							}
						}
					}
				} catch (org.apache.commons.fileupload.FileUploadException e) {
					logger.error("",e);
					this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e.getMessage());
				}
			} else {
				if (entity != null) {
					Reader sax = entity.getReader();
					try {
	            
						SAXReader reader = new SAXReader(user);
						if (item != null) {
							reader.setTemplate(item);
						}

						item = reader.parse(sax);

						if (!reader.assertValid()) {
							throw reader.getErrors().get(0);
						}
						if (XFT.VERBOSE) {
							System.out.println("Loaded XML Item:"									+ item.getProperName());
						}
	            if(item!=null){
					completeDocument=true;
	            }
	            
			} catch (SAXParseException e) {
				e.printStackTrace();
				this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY,e.getMessage());
						throw e;
			} catch (IOException e) {
				e.printStackTrace();
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			} catch (Exception e) {
				e.printStackTrace();
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			}
				}
			}
		}else if(req_format.equals("form")){
			try {
				Form bodyForm = new Form(entity);
				Form queryForm = getRequest().getResourceRef().getQueryAsForm();
				
				Map<String,String> params=bodyForm.getValuesMap();
				
				params.putAll(queryForm.getValuesMap());
				
					if(params.containsKey("ELEMENT_0")){
						dataType=params.get("ELEMENT_0");
				}
				if (params.containsKey("xsiType")) {
					dataType = params.get("xsiType");
				}

				if (dataType == null) {
						for(String key: params.keySet()){
							if(key.indexOf(":")>-1 && key.indexOf("/")>-1){
								dataType=key.substring(0,key.indexOf("/"));
								break;
							}
						}
					}

				if(dataType!=null){
					PopulateItem populater = null;
					if (item != null) {
						populater = PopulateItem.Populate(params, user,								dataType, true, item);
					} else if (dataType != null) {
						populater = PopulateItem.Populate(params, user,								dataType, true);
					}
					
					item= populater.getItem();
				}
			} catch (XFTInitException e) {
				e.printStackTrace();
			} catch (ElementNotFoundException e) {
				e.printStackTrace();
			} catch (FieldNotFoundException e) {
				e.printStackTrace();
			}
		}
			
		try {
			Form queryForm = this.getQueryVariableForm();
			
			Map<String,String> params=queryForm.getValuesMap();
				if(params.containsKey("ELEMENT_0")){
					dataType=params.get("ELEMENT_0");
			}
			if (params.containsKey("xsiType")) {
				dataType = params.get("xsiType");
			}

			if (dataType == null) {
					for(String key: params.keySet()){
						if(key.indexOf(":")>-1 && key.indexOf("/")>-1){
							dataType=key.substring(0,key.indexOf("/"));
							break;
						}
					}
				}

			if (this.fieldMapping.size() > 0) {
				for (String key : fieldMapping.keySet()) {
					if (params.containsKey(key)) {
						params.put(this.fieldMapping.get(key), params.get(key));
					}
				}
			}
			
			PopulateItem populater=null;
			if(item!=null){
				populater = PopulateItem.Populate(params,user,dataType,true,item);
			}else if(dataType!=null){
				populater = PopulateItem.Populate(params,user,dataType,true);
			}
			
			if(populater!=null)
				item= populater.getItem();
			
		} catch (XFTInitException e) {
			e.printStackTrace();
		} catch (ElementNotFoundException e) {
			e.printStackTrace();
		} catch (FieldNotFoundException e) {
			e.printStackTrace();
		}
		return item;
	}

	public void returnSuccessfulCreateFromList(String newURI) {
		Reference ticket_ref = getRequest().getResourceRef().addSegment(newURI);
		getResponse().setLocationRef(ticket_ref);

		String targetRef = ticket_ref.getTargetRef().toString();
		if (targetRef.indexOf("?") > -1) {
			targetRef = targetRef.substring(0, targetRef.indexOf("?"));
		}

		getResponse().setEntity(new StringRepresentation(targetRef));
		Representation selectedRepresentation = getResponse().getEntity();
		if (getRequest().getConditions().hasSome()) {
			final Status status = getRequest().getConditions().getStatus(					getRequest().getMethod(), selectedRepresentation);

			if (status != null) {
				getResponse().setStatus(status);
				getResponse().setEntity(null);
			}
		}
	}

	public void returnString(String message,Status status) {
		returnRepresentation(new StringRepresentation(message),status);
	}

	public void returnString(String message,MediaType mt,Status st) {
		returnRepresentation(new StringRepresentation(message,mt),st);
			}

	public void returnRepresentation(Representation message,Status st) {
		getResponse().setEntity(message);
		getResponse().setStatus(st);
	}

	public void returnXML(XFTItem item) {
		getResponse().setEntity(this.representItem(item, MediaType.TEXT_XML));
		Representation selectedRepresentation = getResponse().getEntity();
		if (getRequest().getConditions().hasSome()) {
			final Status status = getRequest().getConditions().getStatus(					getRequest().getMethod(), selectedRepresentation);

			if (status != null) {
				getResponse().setStatus(status);
				getResponse().setEntity(null);
			}
		}
	}

	protected void setResponseHeader(String key, String value) {
		Form responseHeaders = (Form) getResponse().getAttributes().get(				"org.restlet.http.headers");

		if (responseHeaders == null) {
			responseHeaders = new Form();
			getResponse().getAttributes().put("org.restlet.http.headers",					responseHeaders);
		}

		responseHeaders.add(key, value);
	}

	protected boolean isQueryVariableTrue(String key) {
		if (this.getQueryVariable(key) != null) {
			String v = this.getQueryVariable(key);
			if (v.equalsIgnoreCase("false") || v.equalsIgnoreCase("0")) {
				return false;
			} else {
				return true;
			}
		} else {
			return false;
		}
	}
	
	protected boolean isQueryVariableFalse(String key){
		if(this.getQueryVariable(key)!=null){
			String v= this.getQueryVariable(key);
			if(v.equalsIgnoreCase("false") || v.equalsIgnoreCase("0")){
				return true;
			}else{
				return false;
			}
		}else return false;
	}

	public String getLabelForFieldMapping(String xPath) {
		for (Map.Entry<String, String> entry : this.fieldMapping.entrySet()) {
			if (entry.getValue().equalsIgnoreCase(xPath)) {
				return entry.getKey();
			}
		}
		return null;
	}

	protected HttpServletRequest getHttpServletRequest() {
		return new RequestUtil().getHttpServletRequest(getRequest());
	}
	
	@SuppressWarnings("unchecked")
	public void setContentDisposition(String content){
		Object oHeaders = getResponse().getAttributes().get(
                HttpConstants.ATTRIBUTE_HEADERS);
        Series<Parameter> headers = null;
        if (oHeaders != null) {
            headers = (Series<Parameter>) oHeaders;
        } else {
            headers = new Form();
        }
        headers.add(new Parameter(CONTENT_DISPOSITION,content));

        getResponse().getAttributes().put(HttpConstants.ATTRIBUTE_HEADERS,
                headers);
	}
	
	/**
	 * Return the list of query string parameters value with the name 'action'.  List is created on first access, and cached for later access.
	 * @return Should never be null.
	 */
	public List<String> getActions(){
		if(actions==null){
			final Form f = getQueryVariableForm();
			if (f != null) {
				final String[] actionA=f.getValuesArray(ACTION);
				if(actionA!=null && actionA.length>0){
					actions=Arrays.asList(actionA);
}
			}
			
			if(actions==null)actions=new ArrayList<String>();
		}
		return actions;
	}
	
	public boolean containsAction(final String name){
		if(getActions().contains(name)){
			return true;
		}else{
			return false;
		}
	}
	
	public List<FileWriterWrapperI> getFileWriters() throws FileUploadException, ClientException{
		return getFileWritersAndLoadParams(this.getRequest().getEntity());
	}
	

	public void handleParam(final String key,final Object value) throws ClientException{
		
	}

	public void loadParams(Form f) throws ClientException{
		if(f!=null){
			for(final String key:f.getNames()){
				for(String v:f.getValuesArray(key)){
					handleParam(key,v);
				}
			}
		}
	}
	
	public void loadParams (final String _json) throws ClientException {
	    try {
	        final JSONObject json = new JSONObject(_json);
	        for (final String key: JSONObject.getNames(json)) {
	            handleParam(key, json.get(key));
	        }
	    } catch (JSONException e) {
	        logger.error("invalid JSON message " + _json, e);
	    } catch (NullPointerException e) {
	        logger.error("",e);
	    }
	}
					
	public List<FileWriterWrapperI> getFileWritersAndLoadParams(final Representation entity) throws FileUploadException,ClientException{
	    final List<FileWriterWrapperI> wrappers=new ArrayList<FileWriterWrapperI>();
		if(this.isQueryVariableTrue("inbody") || RequestUtil.isFileInBody(entity)){
			
			if (entity != null && entity.getMediaType() != null && entity.getMediaType().getName().equals(MediaType.MULTIPART_FORM_DATA.getName())) {
				this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_ACCEPTABLE,"In-body File posts must include the file directly as the body of the message (not as part of multi-part form data).");
		        return null;
			}else{
				// NOTE: modified driveFileName here to return a name when content-type is null
				final String fileName=(filepath==null || filepath.equals(""))?RequestUtil.deriveFileName("upload",entity,false):filepath;
				
				if(fileName==null){
					throw new FileUploadException("In-body File posts must include the file directly as the body of the message.", new Exception());
				}
				
				if(entity.getSize()==-1 || entity.getSize()==0){
					throw new FileUploadException("In-body File posts must include the file directly as the body of the message.", new Exception());
				}
				
				wrappers.add(new FileWriterWrapper(entity,fileName));
			}
			
		}else{
			if (RequestUtil.isMultiPartFormData(entity)) {
				final org.apache.commons.fileupload.DefaultFileItemFactory factory = new org.apache.commons.fileupload.DefaultFileItemFactory();
				final org.restlet.ext.fileupload.RestletFileUpload upload = new  org.restlet.ext.fileupload.RestletFileUpload(factory);
				List<FileItem> items;
			    try {
					items = upload.parseRequest(this.getRequest());
				} catch (org.apache.commons.fileupload.FileUploadException e) {
					throw new FileUploadException(e.getMessage(),e);
				}
					
				for (final Iterator<FileItem> it = items.iterator(); it.hasNext(); ) {    
				    final FileItem fi = it.next();
				     
                    if (fi.isFormField()) {
                    	// Load form field to passed parameters map
                    	handleParam(fi.getFieldName(),fi.getString());
                       	continue;
                    } 
                    if (fi.getName()==null) {
                    	throw new FileUploadException("multi-part form posts must contain the file name of the uploaded file.", new Exception());
                    } 
				    
				    String fileName=fi.getName();
				    if(fileName.indexOf('\\')>-1){
				    	fileName=fileName.substring(fileName.lastIndexOf('\\')+1);
				    }
				    wrappers.add(new FileWriterWrapper(fi,fileName));
				}
			}
			
		}
		
		return wrappers;
	}
	
	public HttpSession getHttpSession() {
		return getHttpServletRequest().getSession();
}

	public static String CONTEXT_PATH=null;

	public String getContextPath(){
		if(CONTEXT_PATH==null){
			CONTEXT_PATH=TurbineUtils.GetRelativePath(getHttpServletRequest());
		}
		return CONTEXT_PATH;
	}

	public String wrapPartialDataURI(String uri){
		return "/data"+uri;
	}

	public void setResponseStatus(final ActionException e){
		this.getResponse().setStatus(e.getStatus(), e, e.getMessage());
	}
	
	public Integer identifyCompression(Integer defaultCompression)throws ActionException{
		try {
			if(this.containsQueryVariable(COMPRESSION)){
				return Integer.valueOf(this.getQueryVariable(COMPRESSION));
			}
		} catch (NumberFormatException e) {
			throw new ClientException(e.getMessage());
		}
		
		if(defaultCompression!=null){
			return defaultCompression;
		}else{
			return ZipUtils.DEFAULT_COMPRESSION;
		}
	}
}
