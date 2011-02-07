package org.nrg.xnat.restlet.services;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nrg.status.StatusListenerI;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.archive.PrearcSessionArchiver;
import org.nrg.xnat.helpers.PrearcImporterHelper;
import org.nrg.xnat.helpers.prearchive.PrearcDatabase;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.nrg.xnat.helpers.prearchive.SessionDataTriple;
import org.nrg.xnat.helpers.uri.UriParserUtils;
import org.nrg.xnat.helpers.uri.UriParserUtils.ArchiveURI;
import org.nrg.xnat.helpers.uri.UriParserUtils.DataURIA;
import org.nrg.xnat.helpers.uri.UriParserUtils.PrearchiveURI;
import org.nrg.xnat.restlet.services.prearchive.BatchPrearchiveActionsA;
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

public class Archiver extends BatchPrearchiveActionsA  {
	private static final String REDIRECT2 = "redirect";
	private static final String FOLDER = "folder";
	private static final String URL = "URL";
	private static final String ADDITIONAL_VALUES = "additionalValues";
	private static final String OVERWRITE = "overwrite";
	private static final String PROJECT = "project";
	private static final String CRLF = "\r\n";
	private static final String DEST = "dest";
	
	private final static Logger logger = LoggerFactory.getLogger(Archiver.class);
	
	public Archiver(Context context, Request request, Response response) {
		super(context, request, response);
				
	}
	
			final Map<String,Object> additionalValues=new Hashtable<String,Object>();
			
			String project_id=null;
			String overwriteV=null;
			String timestamp=null;
			String[] sessionFolder=null;
			String dest=null;
	String redirect=null;
			
	@Override
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
					srcs=Arrays.asList(f.getValuesArray(SRC));
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
			
			if(sessions.size()==1){
				String _return;
				
				final String project=PrearcImporterHelper.identifyProject(sessions.get(0));
				
				try {
					if (!PrearcUtils.canModify(user, project)) {
						this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Invalid permissions for new project.");
						return;
					}
					
					_return = PrearcDatabase.archive(sessions.get(0), project, allowDataDeletion, overwrite, user, new ArrayList<StatusListenerI>());
				} catch (Exception e) {
					logger.error("",e);
					this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e);
					return;
				}
								
				if(!StringUtils.isEmpty(redirect) && redirect.equalsIgnoreCase("true")){
					getResponse().redirectSeeOther(getContextPath()+_return);
				}else{
					getResponse().setEntity(_return+CRLF, MediaType.TEXT_URI_LIST);
				}
				return;
				
			}else{				
				Map<SessionDataTriple,Boolean> m;
				
				final String project=PrearcImporterHelper.identifyProject(sessions.get(0));
				
				try {
					if (!PrearcUtils.canModify(user, project)) {
						this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Invalid permissions for new project.");
						return;
					}
					
					m=PrearcDatabase.archive(sessions, project, allowDataDeletion, overwrite, user, new ArrayList<StatusListenerI>());
				} catch (Exception e) {
					logger.error("",e);
					this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e);
					return;
				}
								
				try {
					getResponse().setEntity(updatedStatusRepresentation(m.keySet(),overrideVariant(getPreferredVariant())));
				} catch (Exception e) {
					logger.error("",e);
					this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e);
					return;
				}
			}
		} catch (ResourceException e) {
			logger.error("",e);
			this.getResponse().setStatus(e.getStatus(), e.getMessage());
		} catch (IllegalArgumentException e) {
			logger.error("",e);
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
		}
	}
	
	private String getPrearchivePath(final String project_id) throws ResourceException{
		XnatProjectdata proj = XnatProjectdata.getXnatProjectdatasById(project_id, user, false);
		if(proj==null){
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,"Unknown project: " + project_id);
		}
		return proj.getPrearchivePath();
	}
	
	public static File getSrcDIR(Map<String,Object> params){
		return (File)params.get(FOLDER);
	}
		
	@SuppressWarnings("unchecked")
	public static  PrearcSessionArchiver buildArchiver(final Map<String,Object> session,final String project, final Boolean allowDataDeletion,final Boolean overwrite,final XDATUser user) throws IOException, SAXException{
		return buildArchiver(getSrcDIR(session),project,(Map<String,Object>)session.get(ADDITIONAL_VALUES),allowDataDeletion,overwrite,(String)session.get(URL),user);
	}
	
	public static PrearcSessionArchiver buildArchiver(final File sessionDir,final String project_id,Map<String,Object> additionalValues, final Boolean allowDataDeletion,final Boolean overwrite, final String url,final XDATUser user) throws IOException, SAXException {
		final PrearcSessionArchiver archiver;
		archiver = new PrearcSessionArchiver(sessionDir, user, project_id, additionalValues, allowDataDeletion,overwrite);
			
		return archiver;
	}
}
