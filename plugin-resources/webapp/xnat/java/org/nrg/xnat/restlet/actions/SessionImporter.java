/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.xnat.restlet.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.status.ListenerUtils;
import org.nrg.status.StatusProducer;
import org.nrg.xdat.model.XnatImagesessiondataI;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.archive.PrearcSessionArchiver;
import org.nrg.xnat.helpers.PrearcImporterHelper;
import org.nrg.xnat.helpers.prearchive.PrearcTableBuilder;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.nrg.xnat.helpers.uri.UriParserUtils;
import org.nrg.xnat.restlet.actions.PrearcImporterA.PrearcSession;
import org.nrg.xnat.restlet.actions.importer.ImporterHandlerA;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.nrg.xnat.utils.WorkflowUtils;
import org.restlet.data.Status;
import org.xml.sax.SAXException;

public class SessionImporter extends ImporterHandlerA implements Callable<List<String>> {
	private static final String TRUE = "true";

	public static final String AUTO_ARCHIVE = "auto-archive";

	static Logger logger = Logger.getLogger(SessionImporter.class);

	public static final String AA = "AA";
	public static final String DEST = "dest";
	public static final String RESPONSE_URL = "URL";
	
	private final Boolean allowDataDeletion;
	
	private final Boolean overwrite;
	
	private final FileWriterWrapperI fw;
	
	private final Object uID;
	
	private final XDATUser user;
	
	final Map<String,Object> params;
	
	
	
	@SuppressWarnings("serial")
	static List<String> prearc_variables=new ArrayList<String>(){{
		add(AA);
		add(AUTO_ARCHIVE);
		add(PrearcUtils.PREARC_SESSION_FOLDER);
		add(PrearcUtils.PREARC_TIMESTAMP);
	}};
	
	/**
	 * 
	 * @param listenerControl
	 * @param u
	 * @param session
	 * @param overwrite:   'append' means overwrite, but preserve un-modified content (don't delete anything)
	 *                      'delete' means delete the pre-existing content.
	 * @param additionalValues: should include project, subject_ID and label (if session is null)
	 */
	public SessionImporter(final Object listenerControl, final XDATUser u, final FileWriterWrapperI fw, final Map<String,Object> params){
		super(listenerControl, u, fw, params);
		this.uID=listenerControl;
		this.user=u;
		this.fw=fw;
		this.params=params;
		
		String overwriteV=(String)params.remove("overwrite");
		
		if(overwriteV==null){
			this.allowDataDeletion=false;
			this.overwrite=false;
		}else{
			if(overwriteV.equalsIgnoreCase(PrearcUtils.APPEND)){
				this.allowDataDeletion=false;
				this.overwrite=true;
			}else if(overwriteV.equalsIgnoreCase(PrearcUtils.DELETE)){
				this.allowDataDeletion=true;
				this.overwrite=true;
			} else{
				this.allowDataDeletion=false;
				this.overwrite=true;
			}
			}
		}
		
	public static List<PrearcSession> importToPrearc(StatusProducer parent,String format,Object listener,XDATUser user,FileWriterWrapperI fw,Map<String,Object> params,boolean overwrite, boolean allowDataDeletion) throws ClientException, ServerException{
		//write file
		try {
			return ListenerUtils.addListeners(parent, PrearcImporterA.buildImporter(format,listener, user, fw, params,overwrite,allowDataDeletion))
				.call();
		} catch (SecurityException e) {
			throw new ServerException(e.getMessage(),e);
		} catch (IllegalArgumentException e) {
			throw new ClientException(Status.CLIENT_ERROR_BAD_REQUEST,e.getMessage(),e);
		} catch (NoSuchMethodException e) {
			throw new ClientException(Status.CLIENT_ERROR_BAD_REQUEST,e.getMessage(),e);
		} catch (InstantiationException e) {
			throw new ServerException(e.getMessage(),e);
		} catch (IllegalAccessException e) {
			throw new ServerException(e.getMessage(),e);
		} catch (InvocationTargetException e) {
			throw new ServerException(e.getMessage(),e);
		} catch (PrearcImporterA.UnknownPrearcImporterException e) {
			throw new ClientException(Status.CLIENT_ERROR_NOT_FOUND,e.getMessage(),e);
		}
	}

	public static XnatImagesessiondata getExperimentByIdorLabel(final String project, final String expt_id, final XDATUser user){
		XnatImagesessiondata expt=null;
		if(!StringUtils.isEmpty(project)){
			expt=(XnatImagesessiondata)XnatExperimentdata.GetExptByProjectIdentifier(project, expt_id, user, false);
			}
			
		if(expt==null){
			expt=(XnatImagesessiondata)XnatExperimentdata.getXnatExperimentdatasById(expt_id, user, false);
			}
		return expt;
			}
			
	@SuppressWarnings("serial")
	public List<String> call() throws ClientException,ServerException{
			try {
			String dest =(String)params.get(DEST);

			XnatImagesessiondata expt=null;
			
			final UriParserUtils.DataURIA destination=(!StringUtils.isEmpty(dest))?UriParserUtils.parseURI(dest):null;
			
			String project=null;
			
			Map<String,Object> prearc_parameters=new HashMap<String,Object>(params);
			
			
			//check for existing session by URI
			if(destination!=null){
				if(destination instanceof UriParserUtils.PrearchiveURI){
					prearc_parameters.putAll(destination.getProps());
				}else{
					project=PrearcImporterHelper.identifyProject(destination.getProps());
					if(!StringUtils.isEmpty(project)){
						prearc_parameters.put("project", project);
			}
			
					if(destination.getProps().containsKey(UriParserUtils.SUBJECT_ID)){
						prearc_parameters.put("subject_ID", destination.getProps().get(UriParserUtils.SUBJECT_ID));
					}
			
					String expt_id=(String)destination.getProps().get(UriParserUtils.EXPT_ID);
					if(!StringUtils.isEmpty(expt_id)){
						expt=getExperimentByIdorLabel(project, expt_id,user);
			}
			
					if(expt==null){
						if(!StringUtils.isEmpty(expt_id)){
							prearc_parameters.put("label", expt_id);
						}
					}
				}
			}
			
			if(expt==null){
				if(StringUtils.isEmpty(project)){
					project=PrearcImporterHelper.identifyProject(prearc_parameters);
				}
				
				//check for existing experiment by params
				if(prearc_parameters.containsKey(UriParserUtils.SUBJECT_ID)){
					prearc_parameters.put("xnat:subjectAssessorData/subject_ID", prearc_parameters.get(UriParserUtils.SUBJECT_ID));
				}
									
				String expt_id=(String)prearc_parameters.get(UriParserUtils.EXPT_ID);
				String expt_label=(String)prearc_parameters.get(UriParserUtils.EXPT_LABEL);
				if(!StringUtils.isEmpty(expt_id)){
					expt=getExperimentByIdorLabel(project, expt_id,user);
				}
				
				if(expt==null && !StringUtils.isEmpty(expt_label)){
					expt=getExperimentByIdorLabel(project, expt_id,user);
				}
					
				if(expt==null){
					if(!StringUtils.isEmpty(expt_label)){
						prearc_parameters.put("xnat:experimentData/label", expt_label);
					}else if(!StringUtils.isEmpty(expt_id)){
						prearc_parameters.put("xnat:experimentData/label", expt_id);
					}
				}
			}
			
			//set properties to match existing session
			if(expt!=null){
				prearc_parameters.put("xnat:experimentData/project", expt.getProject());
				prearc_parameters.put("xnat:subjectAssessorData/subject_ID", expt.getSubjectId());
				prearc_parameters.put("xnat:experimentData/label", expt.getLabel());
				prearc_parameters.put("xnat:experimentData/ID", expt.getId());
			}
			
			//import to prearchive
			final List<PrearcSession> sessions=importToPrearc(this,(String)params.remove(PrearcImporterA.PREARC_IMPORTER_ATTR),uID,user,fw,prearc_parameters,overwrite,allowDataDeletion);
			
			if(sessions.size()==0){
				throw new ClientException("Upload did not include parseable files for session generation.");
			}
			
			//if prearc=destination, then return
			if(destination!=null && destination instanceof UriParserUtils.PrearchiveURI){
				return returnURLs(sessions);
			}

			
			//if unknown destination, only one session supported
			if(sessions.size()>1){
				throw new ClientException("Upload included files for multiple imaging sessions.");
			}
			
			final PrearcSession session = sessions.get(0);
				
			final XnatImagesessiondataI isd=PrearcTableBuilder.parseSession(session.getSessionXML());
				
			if(isAutoArchive(params,destination,isd)){
					final String uri=ListenerUtils.addListeners(this, new PrearcSessionArchiver(session.getSessionDIR(), user, isd.getProject(), removePrearcVariables(params), allowDataDeletion,overwrite))
						.call();
					return new ArrayList<String>(){{add(uri);}};
			}else{
				this.completed("Successfully uploaded " + sessions.size() +" sessions to the prearchive.");
				return returnURLs(sessions);
			}
			
		} catch (ClientException e) {
			this.failed(e.getMessage());
			throw e;
		} catch (ServerException e) {
			this.failed(e.getMessage());
			throw e;
		} catch (IOException e) {
			this.failed(e.getMessage());
			throw new ServerException(e.getMessage(),e);
		} catch (SAXException e) {
			this.failed(e.getMessage());
			throw new ClientException(e.getMessage(),e);
		} catch (Throwable e) {
			logger.error("",e);
			throw new ServerException(e.getMessage(),new Exception());
		}
	}
	
	public static boolean isAutoArchive(final Map<String,Object> params, final UriParserUtils.DataURIA destination,final XnatImagesessiondataI isd) throws ServerException, IOException, SAXException{
		//determine auto-archive setting
		String aa = (String)params.remove(AA);
		
		if(aa==null){
			aa = (String)params.remove(AUTO_ARCHIVE);
		}
		
		boolean autoarchive=false;
		
		if(destination !=null && destination instanceof UriParserUtils.ArchiveURI){
			autoarchive=true;
		}
		
		if(aa!=null && aa.toString().equalsIgnoreCase(TRUE)){
			autoarchive=true;
		}
			
		if(isd.getProject()==null){
			return false;
		}
		
		if(!autoarchive){
			final Integer code=ArcSpecManager.GetInstance().getPrearchiveCodeForProject(isd.getProject());
			if(code!=null && code.equals(4)){
				autoarchive=true;
			}
		}
		
		return autoarchive;
	}
	
	public static Map<String,Object> removePrearcVariables(final Map<String,Object> params){
		for(String param: prearc_variables){
			params.remove(param);
		}
		return params;
	}
	
	public static List<String> returnURLs(final List<PrearcSession> sessions){
		List<String> _return= new ArrayList<String>();
		for(final PrearcSession ps: sessions){
			_return.add(ps.getUrl().toString());
		}
		return _return;
	}
	
}