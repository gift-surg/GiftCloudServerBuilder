// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources;

import com.noelios.restlet.http.HttpConstants;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.DefaultFileItemFactory;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nrg.action.ActionException;
import org.nrg.action.ClientException;
import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.utils.AccessLogger;
import org.nrg.xdat.turbine.utils.PopulateItem;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFT;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTable;
import org.nrg.xft.db.DBAction;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.db.ViewManager;
import org.nrg.xft.event.EventDetails;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.presentation.FlattenedItemA;
import org.nrg.xft.presentation.ItemJSONBuilder;
import org.nrg.xft.presentation.ItemMerger;
import org.nrg.xft.presentation.ItemPropBuilder;
import org.nrg.xft.schema.Wrappers.XMLWrapper.SAXReader;
import org.nrg.xft.utils.zip.ZipUtils;
import org.nrg.xnat.helpers.FileWriterWrapper;
import org.nrg.xnat.itemBuilders.FullFileHistoryBuilder;
import org.nrg.xnat.restlet.representations.*;
import org.nrg.xnat.presentation.ChangeSummaryBuilderA.ChangeSummary;
import org.nrg.xnat.presentation.ChangeSummaryBuilderA.ItemEventI;
import org.nrg.xnat.presentation.DateBasedSummaryBuilder;
import org.nrg.xnat.restlet.representations.JSONObjectRepresentation;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.nrg.xnat.restlet.util.RequestUtil;
import org.nrg.xnat.turbine.utils.ArchivableItem;
import org.nrg.xnat.utils.WorkflowUtils;
import org.restlet.Context;
import org.restlet.data.*;
import org.restlet.ext.fileupload.RestletFileUpload;
import org.restlet.resource.*;
import org.restlet.util.Series;
import org.xml.sax.SAXParseException;

import javax.servlet.http.HttpServletRequest;
import com.google.common.collect.Maps;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

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

	public static Logger logger = Logger.getLogger(SecureResource.class);
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

	protected String csrfToken = null;

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

			filepath=TurbineUtils.escapeParam(filepath);
		}
		logAccess();
	}

	public static Object getParameter(Request request,String key){
		return TurbineUtils.escapeParam(request.getAttributes().get(key));
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

			AccessLogger.LogServiceAccess(login, getRequest().getClientInfo().getAddress(), getRequest().getMethod() + " " + url, "");
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
			} else if (this.requested_format.equals("tar")) {
				return MediaType.APPLICATION_TAR;
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

	public boolean isZIPRequest(){
		return isZIPRequest(getRequestedMediaType());
	}

	public static boolean isZIPRequest(MediaType mt){
		if (mt==null ||
				!(mt.equals(MediaType.APPLICATION_ZIP)
					|| mt.equals(MediaType.APPLICATION_TAR)
					|| mt.equals(MediaType.APPLICATION_GNU_TAR))) {
    		return false;
    	}else{
    		return true;
    	}
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

	private Form f= null;
	/**
	 * This method is used internally to get the Query form.  It should remain private so that all access to parameters are guaranteed to be properly escaped.
	 * @return
	 */
	private Form getQueryVariableForm(){
		if(f==null){
			f= getRequest().getResourceRef().getQueryAsForm();
		}
		return f;
	}

	public Map<String,String> getQueryVariableMap(){
		return convertFormToMap(getQueryVariableForm());
	}


	private Form _body;
	/**
	 * This method is used internally to get the Body form.  It should remain private so that all access to parameters are guaranteed to be properly escaped.
	 * @return
	 */
	private Form getBodyAsForm(){
		if(_body==null){
		Representation entity = this.getRequest().getEntity();

		if (RequestUtil.isMultiPartFormData(entity)) {
				_body=new Form(entity);
			}
		}

		return _body;
	}

	private static Map<String,String> convertFormToMap(Form q){
		Map<String,String> map=Maps.newLinkedHashMap();
		if(q!=null){
			for(String s:q.getValuesMap().keySet()){
				map.put(s, TurbineUtils.escapeParam(q.getFirstValue(s)));
			}
		}
		return map;
	}

	public Map<String,String> getBodyVariableMap(){
		return convertFormToMap(getBodyAsForm());
	}

	public String getBodyVariable(String key){
		Form f = getBodyAsForm();
		if (f != null) {
			return TurbineUtils.escapeParam(f.getFirstValue(key));
		}
		return null;
	}

	private static String[] getVariablesFromForm(Form f, String key){
		if (f != null) {
			String[] values= f.getValuesArray(key).clone();
			for(int i=0;i<values.length;i++){
				values[i]=TurbineUtils.escapeParam(values[i]);
			}
			return f.getValuesArray(key);
		}
		return null;
	}

	public String[] getBodyVariables(String key) {
		return getVariablesFromForm(getBodyAsForm(),key);
	}

	public boolean hasBodyVariable(String key){
		if(getBodyVariable(key)==null)
		{
			return false;
		}else{
			return true;
		}
	}

	public Set<String> getBodyVariableKeys() {
		Form f = getBodyAsForm();
		if (f != null) {
			return f.getValuesMap().keySet();
		}
		return null;
	}

	public String getQueryVariable(String key){
		Form f = getQueryVariableForm();
		if (f != null && f.getValuesMap().containsKey(key)) {
			return TurbineUtils.escapeParam(f.getFirstValue(key));
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

	public boolean hasQueryVariable(String key){
		return containsQueryVariable(key);
	}

	public Map<String,Object> getQueryVariablesAsMap(){
		Map<String,Object> params=new Hashtable<String,Object>();
		Form f = getQueryVariableForm();
		if (f != null) {
			for (Parameter p:f){
				params.put(p.getName(), p.getValue());
			}
		}
		return params;
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
		return getVariablesFromForm(getQueryVariableForm(),key);
	}

	public Set<String> getQueryVariableKeys() {
		Form f = getQueryVariableForm();
		if (f != null) {
			return f.getValuesMap().keySet();
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

	public String getReason(){
		return this.getQueryVariable(EventUtils.EVENT_REASON);
	}
	
	public Representation representTable(XFTTable table, MediaType mt,Hashtable<String,Object> params){
		return representTable(table,mt,params,null);
	}

	public Representation representTable(XFTTable table, MediaType mt,Hashtable<String,Object> params,Map<String,Map<String,String>> cp){
		if(table!=null){
			if(this.getQueryVariable("sortBy")!=null){
				final String sortBy=this.getQueryVariable("sortBy");
				table.sort(Arrays.asList(StringUtils.split(sortBy, ',')));
				if(this.isQueryVariable("sortOrder","DESC",false) && !mt.equals(APPLICATION_XLIST)){
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
				return new ItemHTMLRepresentation(item, MediaType.TEXT_HTML, getRequest(), user,getQueryVariable("requested_screen"),new Hashtable<String,Object>());
			} catch (Exception e) {
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e);
				return null;
			}
		}else if (mt.equals(MediaType.APPLICATION_JSON)) {
			try {
				FlattenedItemA.HistoryConfigI history=(isQueryVariableTrue("includeHistory"))?FlattenedItemA.GET_ALL:new FlattenedItemA.HistoryConfigI(){
					@Override
					public boolean getIncludeHistory() {
						return false;
					}
				};
				return new JSONObjectRepresentation(MediaType.APPLICATION_JSON,(new ItemJSONBuilder()).call(item, history,  isQueryVariableTrue("includeHeaders")));
			} catch (Exception e) {
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e);
				return null;
			}
		} else {
			return new ItemXMLRepresentation(item, MediaType.TEXT_XML, true,!this.isQueryVariableTrue("concealHiddenFields"));
		}
	}

	public MediaType buildMediaType(MediaType mt, String fName){
		if(fName.endsWith(".gif")){
			mt = MediaType.IMAGE_GIF;
		}else if(fName.endsWith(".jpeg")){
			mt = MediaType.IMAGE_JPEG;
		}else if(fName.endsWith(".xml")){
			mt = MediaType.TEXT_XML;
		}else if(fName.endsWith(".jpg")){
			mt = MediaType.IMAGE_JPEG;
		}else if(fName.endsWith(".png")){
			mt = MediaType.IMAGE_PNG;
		}else if(fName.endsWith(".bmp")){
			mt = MediaType.IMAGE_BMP;
		}else if(fName.endsWith(".tiff")){
			mt = MediaType.IMAGE_TIFF;
		}else if(fName.endsWith(".html")){
			mt = MediaType.TEXT_HTML;
		}else{
			if((mt!=null && mt.equals(MediaType.TEXT_XML)) && !fName.endsWith(".xml")){
				mt=MediaType.ALL;
			}else{
				mt=MediaType.APPLICATION_OCTET_STREAM;
			}
		}
		return mt;
	}

	public FileRepresentation representFile(File f,MediaType mt){
		mt=buildMediaType(mt,f.getName());

		this.setContentDisposition(f.getName());

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
				Map<String,String> params=getBodyVariableMap();

				params.putAll(getQueryVariableMap());

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
			Map<String,String> params=getQueryVariableMap();
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
			logger.error("",e);
		} catch (ElementNotFoundException e) {
			logger.error("",e);
		} catch (FieldNotFoundException e) {
			logger.error("",e);
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

	protected boolean isFalse(Object value) {
		if (value != null) {
			String v = (String)value;
			if(v.equalsIgnoreCase("false") || v.equalsIgnoreCase("0")){
				return true;
			}else{
				return false;
			}
		}
		else {
			return false;
		}
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

    /**
     * Sets the Content-Disposition response header. The filename parameter indicates the name of the content.
     * This method specifies the content as an attachment. If you need to specify inline content (e.g. for MIME
     * content in email or embedded content situations), use {@link #setContentDisposition(String, boolean)}.
     *
     * <b>Note:</b> This differs from the {@link TurbineUtils#setContentDisposition(HttpServletResponse, String)}
     * version of this method in that it performs the header set in a "restlet-y" way. Both methods use the same
     * {@link TurbineUtils#createContentDispositionValue(String, boolean)} method to create the actual value set
     * for the response header.
     *
     * @param filename    The suggested filename for downloaded content.
     */
	public void setContentDisposition(String filename){
        setContentDisposition(filename, true);
    }

    /**
     * Sets the Content-Disposition response header. The filename parameter indicates the name of the content.
     * This method specifies the content as an attachment when the <b>isAttachment</b> parameter is set to true,
     * and as inline content when the <b>isAttachment</b> parameter is set to false. You can specify the content
     * as an attachment by default by calling {@link #setContentDisposition(String)}.
     *
     * <b>Note:</b> This differs from the {@link TurbineUtils#setContentDisposition(HttpServletResponse, String, boolean)}
     * version of this method in that it performs the header set in a "restlet-y" way. Both methods use the same
     * {@link TurbineUtils#createContentDispositionValue(String, boolean)} method to create the actual value set
     * for the response header.
     *
     * @param filename    The suggested filename for downloaded content.
     * @param isAttachment    Indicates whether the content is an attachment or inline.
     */
	@SuppressWarnings("unchecked")
	public void setContentDisposition(String filename, boolean isAttachment) {
        final Map<String, Object> attributes = getResponse().getAttributes();
        if (attributes.containsKey(CONTENT_DISPOSITION)) {
            throw new IllegalStateException("A content disposition header has already been added to this response.");
        }
		Object oHeaders = attributes.get(HttpConstants.ATTRIBUTE_HEADERS);
        Series<Parameter> headers;
        if (oHeaders != null) {
            headers = (Series<Parameter>) oHeaders;
        } else {
            headers = new Form();
        }
        headers.add(new Parameter(CONTENT_DISPOSITION, TurbineUtils.createContentDispositionValue(filename, isAttachment)));
        attributes.put(HttpConstants.ATTRIBUTE_HEADERS, headers);
	}

	/**
	 * Return the list of query string parameters value with the name 'action'.  List is created on first access, and cached for later access.
	 * @return Should never be null.
	 */
	public List<String> getActions(){
		if(actions==null){
			final String[] actionA=getQueryVariables(ACTION);
				if(actionA!=null && actionA.length>0){
					actions=Arrays.asList(actionA);
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

	public void loadQueryVariables() throws ClientException{
		loadParams(getQueryVariableForm());
	}

	public void loadBodyVariables() throws ClientException{
		loadParams(getBodyAsForm());
	}

	public void loadParams(Form f) throws ClientException{
		if(f!=null){
			for(final String key:f.getNames()){
				for(String v:f.getValuesArray(key)){
					handleParam(key,TurbineUtils.escapeParam(v));
				}
			}
		}
	}

	public void loadParams (final String _json) throws ClientException {
	    try {
	        final JSONObject json = new JSONObject(_json);
	        String[] keys = JSONObject.getNames(json);
	        if (keys != null) {
	            for (final String key : keys) {
	            	handleParam(key, TurbineUtils.escapeParam(json.get(key)));
	            }
	        }
	    } catch (JSONException e) {
	        logger.error("invalid JSON message " + _json, e);
	    } catch (NullPointerException e) {
	        logger.error("",e);
	    }
	}

	/**
	 * Gets file writers and load parameters from the request entity. By default this uses the filename as the name
	 * of the {@link FileWriterWrapperI} parameter. When form fields are encountered, the {@link #handleParam(String, Object)}
	 * method is called to cache all of the standard form fields.
	 * @param entity The request entity.
	 * @return A list of any {@link FileWriterWrapperI} objects found in the request.
	 * @throws FileUploadException
	 * @throws ClientException
	 * @see #getFileWritersAndLoadParams(Representation, boolean)
	 */
	public List<FileWriterWrapperI> getFileWritersAndLoadParams(final Representation entity) throws FileUploadException,ClientException{
		return getFileWritersAndLoadParams(entity, false);
	}

	/**
	 * Gets file writers and load parameters from the request entity. When <b>useFileFieldName</b> is <b>true</b>, this uses the
	 * field name in the form as the name in the {@link FileWriterWrapperI} object. Otherwise, it uses the filename as the name
	 * of the {@link FileWriterWrapperI} parameter. When form fields are encountered, the {@link #handleParam(String, Object)}
	 * method is called to cache all of the standard form fields.
	 * @param entity The request entity.
	 * @param useFileFieldName Indicates whether the form field name should be used to identify the extracted files.
	 * @return A list of any {@link FileWriterWrapperI} objects found in the request.
	 * @throws FileUploadException
	 * @throws ClientException
	 * @see #getFileWritersAndLoadParams(Representation)
	 */
	public List<FileWriterWrapperI> getFileWritersAndLoadParams(final Representation entity, boolean useFileFieldName) throws FileUploadException,ClientException{
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

				if (entity == null || entity.getSize() == -1 || entity.getSize() == 0) {
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
                    	handleParam(fi.getFieldName(),TurbineUtils.escapeParam(fi.getString()));
                       	continue;
                    }
                    if (fi.getName()==null) {
                    	throw new FileUploadException("multi-part form posts must contain the file name of the uploaded file.", new Exception());
                    }

				    String fileName=fi.getName();
				    if(fileName.indexOf('\\')>-1){
				    	fileName=fileName.substring(fileName.lastIndexOf('\\')+1);
				    }

				    wrappers.add(new FileWriterWrapper(fi, useFileFieldName ? fi.getFieldName() : fileName));
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
	
	public Representation buildChangesets(XFTItem item, MediaType mt) throws Exception{
		Map<Date,ChangeSummary> changes=DateBasedSummaryBuilder.build(
				Arrays.asList(
						ItemMerger.merge(
							ItemPropBuilder.build(item, FlattenedItemA.GET_ALL,Arrays.asList(new FullFileHistoryBuilder())))),null);
			
			JSONObject wrapper = new JSONObject();
			JSONArray objects=new JSONArray();
			
			for(Map.Entry<Date,ChangeSummary> entry: changes.entrySet()){
				JSONObject o = new JSONObject();
				o.put("date", entry.getKey());
				
				JSONArray a = new JSONArray();
				o.put("events", a);
				for(ItemEventI ie: entry.getValue().getEvents()){
					a.put(ie.toJSON());
				}
				
				objects.put(o);
			}
			wrapper.put("changesets", objects);
			
			return new JSONObjectRepresentation(MediaType.APPLICATION_JSON, wrapper);
	}
	
	public Integer getEventId(){
		final String id=getQueryVariable(EventUtils.EVENT_ID);
		if(id!=null){
			return Integer.valueOf(id);
		}else{
			return null;
		}
	}
	
	public EventUtils.TYPE getEventType(){
		final String id=getQueryVariable(EventUtils.EVENT_TYPE);
		if(id!=null){
			return EventUtils.getType(id, EventUtils.TYPE.WEB_SERVICE);
		}else{
			return EventUtils.TYPE.WEB_SERVICE;
		}
	}
	
	public String getAction(){
		return getQueryVariable(EventUtils.EVENT_ACTION);
	}
	
	public String getComment(){
		return this.getQueryVariable(EventUtils.EVENT_COMMENT);
	}
	
	public EventDetails newEventInstance(EventUtils.CATEGORY cat){
		return EventUtils.newEventInstance(cat, getEventType(), getAction(), getReason(), getComment());
	}
	
	public EventDetails newEventInstance(EventUtils.CATEGORY cat,String action){
		return EventUtils.newEventInstance(cat, getEventType(), (getAction()!=null)?getAction():action, getReason(), getComment());
	}
		
	public void delete(ArchivableItem item,EventDetails event) throws Exception{
		final PersistentWorkflowI workflow=WorkflowUtils.getOrCreateWorkflowData(getEventId(), user, item.getXSIType(), item.getId(), item.getProject(),event);
		final EventMetaI ci=workflow.buildEvent();
    	
		try {
			String removeFiles=this.getQueryVariable("removeFiles");
			if (removeFiles!=null){
				final List<XFTItem> hash = item.getItem().getChildrenOfType("xnat:abstractResource");
			    
			    for (XFTItem resource : hash){
			        ItemI om = BaseElement.GetGeneratedItem((XFTItem)resource);
			        if (om instanceof XnatAbstractresource){
			            XnatAbstractresource resourceA = (XnatAbstractresource)om;
			            resourceA.deleteWithBackup(item.getArchiveRootPath(),user,ci);
			        }
			    }
			}
			DBAction.DeleteItem(item.getItem().getCurrentDBVersion(), user,ci);
			
			WorkflowUtils.complete(workflow,ci);
		} catch (Exception e) {
			WorkflowUtils.fail(workflow,ci);
		}
        
	    user.clearLocalCache();
		MaterializedView.DeleteByUser(user);
	}
	
	public void delete(ArchivableItem parent, ItemI item,EventDetails event) throws Exception{
		final PersistentWorkflowI workflow=WorkflowUtils.getOrCreateWorkflowData(getEventId(), user, parent.getXSIType(), parent.getId(), parent.getProject(),event);
		final EventMetaI ci=workflow.buildEvent();
    	
		try {
			String removeFiles=this.getQueryVariable("removeFiles");
			if (removeFiles!=null){
				final List<XFTItem> hash = item.getItem().getChildrenOfType("xnat:abstractResource");
			    
			    for (XFTItem resource : hash){
			        ItemI om = BaseElement.GetGeneratedItem((XFTItem)resource);
			        if (om instanceof XnatAbstractresource){
			            XnatAbstractresource resourceA = (XnatAbstractresource)om;
			            resourceA.deleteWithBackup(parent.getArchiveRootPath(),user,ci);
			        }
			    }
			}
			DBAction.DeleteItem(item.getItem().getCurrentDBVersion(), user,ci);
			
			WorkflowUtils.complete(workflow,ci);
		} catch (Exception e) {
			WorkflowUtils.fail(workflow,ci);
			throw e;
		}
        
	    user.clearLocalCache();
		MaterializedView.DeleteByUser(user);
	}
	
	public void create(ArchivableItem parent, ItemI sub,boolean overwriteSecurity, boolean allowDataDeletion,EventDetails event) throws Exception{
		PersistentWorkflowI wrk=WorkflowUtils.getOrCreateWorkflowData(getEventId(), user, parent.getItem(), event);
		EventMetaI c=wrk.buildEvent();
		
		try {
			if(sub.save(user,false,false,c)){
				WorkflowUtils.complete(wrk, c);
				MaterializedView.DeleteByUser(user);
			}
		} catch (Exception e) {
			WorkflowUtils.fail(wrk, c);
			throw e;
		}
	}
	
	public void create(ArchivableItem sub,boolean overwriteSecurity, boolean allowDataDeletion,EventDetails event) throws Exception{
		PersistentWorkflowI wrk=WorkflowUtils.getOrCreateWorkflowData(getEventId(), user, sub.getItem(), event);
		EventMetaI c=wrk.buildEvent();
		
		try {
			if(sub.save(user,false,false,c)){
				WorkflowUtils.complete(wrk, c);
				MaterializedView.DeleteByUser(user);
			}
		} catch (Exception e) {
			WorkflowUtils.fail(wrk, c);
			throw e;
		}
	}

	protected static Representation returnStatus(ItemI i,MediaType mt){
		try {
			if(i.needsActivation()){
			    return new StringRepresentation(ViewManager.QUARANTINE,mt);
			}else{
			    return new StringRepresentation(ViewManager.ACTIVE,mt);
			}
		} catch (Exception e) {
		    return new StringRepresentation(ViewManager.ACTIVE,mt);
		}
	}
	
	protected void postSaveManageStatus(ItemI i) throws ActionException{
		try {
			if(this.isQueryVariableTrue("activate")){
				if(user.canActivate(i.getItem())){
					PersistentWorkflowI wrk= PersistentWorkflowUtils.getOrCreateWorkflowData(getEventId(), user, i.getItem(), this.newEventInstance(EventUtils.CATEGORY.DATA, "Activated"));
					try {
						i.activate(user);
						WorkflowUtils.complete(wrk, wrk.buildEvent());
					} catch (Exception e) {
						logger.error("",e);
						WorkflowUtils.fail(wrk,wrk.buildEvent());
					}
				}else {
					this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Specified user account has insufficient activation priviledges for experiments in this project.");
				}
			}

			if(this.isQueryVariableTrue(ViewManager.QUARANTINE)){
				if(user.canActivate(i.getItem())){
					PersistentWorkflowI wrk= PersistentWorkflowUtils.getOrCreateWorkflowData(getEventId(), user, i.getItem(), this.newEventInstance(EventUtils.CATEGORY.DATA, "Quarantined"));
					try {
						i.quarantine(user);
						WorkflowUtils.complete(wrk, wrk.buildEvent());
					} catch (Exception e) {
						logger.error("",e);
						WorkflowUtils.fail(wrk,wrk.buildEvent());
					}
				}else {
					this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Specified user account has insufficient activation priviledges for experiments in this project.");
				}
			}


			if(this.isQueryVariableTrue("_lock")){
				if(user.canActivate(i.getItem())){
					PersistentWorkflowI wrk= PersistentWorkflowUtils.getOrCreateWorkflowData(getEventId(), user, i.getItem(), this.newEventInstance(EventUtils.CATEGORY.DATA, "Locked"));
					try {
						i.lock(user);
						WorkflowUtils.complete(wrk, wrk.buildEvent());
					} catch (Exception e) {
						logger.error("",e);
						WorkflowUtils.fail(wrk,wrk.buildEvent());
					}
				}else { 
					throw new ClientException(Status.CLIENT_ERROR_FORBIDDEN,"Specified user account has insufficient activation priviledges for experiments in this project.",new Exception());
				}
			}
//			else if(this.isQueryVariableTrue("_unlock")){
//				if(user.canActivate(i.getItem())){
//					PersistentWorkflowI wrk= PersistentWorkflowUtils.getOrCreateWorkflowData(getEventId(), user, i.getItem(), this.newEventInstance(EventUtils.CATEGORY.DATA, "Unlocked"));
//					try {
//						i.activate(user);
//						WorkflowUtils.complete(wrk, wrk.buildEvent());
//					} catch (Exception e) {
//						logger.error("",e);
//						WorkflowUtils.fail(wrk,wrk.buildEvent());
//					}
//				}else { 
//					throw new ClientException(Status.CLIENT_ERROR_FORBIDDEN,"Specified user account has insufficient activation priviledges for experiments in this project.",new Exception());
//				}
//			}
		} catch (ActionException e) {
			throw e;
		} catch (Exception e) {
			logger.error("",e);
			throw new org.nrg.action.ServerException("Error modifying status",e);
		}
	}

    protected void respondToException(Exception exception, Status status) {
        logger.error("Transaction got a status: " + status, exception);
        getResponse().setStatus(status, exception.getMessage());
    }
}
