/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.xnat.helpers;

import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.status.ListenerUtils;
import org.nrg.xdat.bean.XnatImagesessiondataBean;
import org.nrg.xdat.model.XnatImagesessiondataI;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.helpers.merge.MergePrearchiveSessions;
import org.nrg.xnat.helpers.merge.MergeSessionsA.SaveHandlerI;
import org.nrg.xnat.helpers.prearchive.PrearcTableBuilder;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.nrg.xnat.helpers.xmlpath.XMLPathShortcuts;
import org.nrg.xnat.restlet.actions.PrearcImporterA;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.nrg.xnat.restlet.util.XNATRestConstants;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.nrg.xnat.turbine.utils.ImageUploadHelper;
import org.restlet.data.Status;

public class PrearcImporterHelper extends PrearcImporterA{
    private static final String TEMP_UNPACK = "temp-unpack";
	static org.apache.log4j.Logger logger = Logger.getLogger(PrearcImporterHelper.class);
	
	
	private final FileWriterWrapperI fi;
	private final XDATUser user;
	
	private final Map<String,Object> params;
	private final Object uID;
	
	private final boolean overwrite,allowDataDeletion;
	
	/**
	 * Helper class to extract a passed zip into the prearchive.
	 * @param uID2
	 * @param u
	 * @param fi
	 * @param project
	 * @param additionalValues
	 */
	public PrearcImporterHelper(final Object uID2, final XDATUser u, final FileWriterWrapperI fi, Map<String,Object> params,boolean overwrite,boolean allowDataDeletion){
    	super((uID2==null)?u:uID2,u,fi,params,overwrite,allowDataDeletion);
    	this.user=u;
    	this.uID=(uID2==null)?u:uID2;
		this.fi=fi;
		this.overwrite=overwrite;
		this.allowDataDeletion=allowDataDeletion;
		this.params=params;
	}
	
	public static String identifyProject(Map<String,Object> params){
		if(params.containsKey("PROJECT_ID")){
			return (String)params.get("PROJECT_ID");
		}
		if(params.containsKey("project")){
			return (String)params.get("project");
		}
		return null;
	}
		
	public List<PrearcSession> call() throws ClientException,ServerException{
		final String project=identifyProject(params);
		final String old_session_folder=(String)params.remove(PrearcUtils.PREARC_SESSION_FOLDER);
		String old_timestamp=(String)params.remove(PrearcUtils.PREARC_TIMESTAMP);
			
		final String new_timestamp=(new java.text.SimpleDateFormat(XNATRestConstants.PREARCHIVE_TIMESTAMP)).format(Calendar.getInstance().getTime());
		
		final File cacheDIR=write_to_cache(user,fi,new_timestamp);
		
		//IF timestamp,session is specified then push to a temporary space and merge
		//ELSE import directly to project.
				
		boolean destination_specified=false;
		if(!StringUtils.isEmpty(old_timestamp)&& !StringUtils.isEmpty(old_session_folder)){
			destination_specified=true;
			if(!StringUtils.isEmpty(project)){
				this.failed("User must specify project portion of prearchive path, if timestamp/session portion is specified.");
				throw new ClientException(Status.CLIENT_ERROR_BAD_REQUEST,"User must specify project portion of prearchive path, if timestamp/session portion is specified.",new IllegalArgumentException());
			}
		}else{
			if(StringUtils.isEmpty(old_timestamp)){
				this.failed("User must specify timestamp portion of prearchive path, if session portion is specified.");
				throw new ClientException(Status.CLIENT_ERROR_BAD_REQUEST,"User must specify timestamp portion of prearchive path, if session portion is specified.",new IllegalArgumentException());
			}
		}
		
		List<File> files=null;
		
		final Map<String,Object> additionalValues=XMLPathShortcuts.identifyUsableFields(params,XMLPathShortcuts.EXPERIMENT_DATA);
		
		if(StringUtils.isEmpty(old_timestamp))old_timestamp=new_timestamp;
					
		if(destination_specified || project==null){
			File projectPrearc;
			if(project==null){
				projectPrearc=new File(ArcSpecManager.GetInstance().getGlobalPrearchivePath(),TEMP_UNPACK);
			}else{
				projectPrearc=new File(ArcSpecManager.GetInstance().getPrearchivePathForProject(project));
			}
			List<File> tempFiles=reorganize(cacheDIR, new File(projectPrearc,new_timestamp), additionalValues);
			
			files=new ArrayList<File>();
			for(final File f:tempFiles){
				File builtF=merge_to_destination(project,old_timestamp,old_session_folder,f);
				if(!files.contains(builtF)){
					files.add(builtF);
				}
			}
		}else{
			files=reorganize(cacheDIR, new File(ArcSpecManager.GetInstance().getPrearchivePathForProject(project),old_timestamp), additionalValues);
		}			
					
		final List<PrearcSession> sessions= new ArrayList<PrearcSession>();
		
		for(final File f:files){
			try {
				sessions.add(new PrearcSession(f,new File(f.getAbsolutePath()+".xml"),new URI(StringUtils.join(new String[]{"/prearchive/projects/",project,"/",new_timestamp,"/",f.getName()}))));
			} catch (URISyntaxException e) {
				throw new ServerException(e.getMessage(),e);
			}
		}
		
		return sessions;
	}
	
	private File write_to_cache(final XDATUser user, final FileWriterWrapperI fi,final String timestamp) throws ServerException{
			final String filename = fi.getName();
		this.processing("Importing file (" + filename + ")");
			
		//BUILD CACHE PATH        
		final File cacheDIR = new File(new File(user.getCachedFile("uploads"),user.getXdatUserId().toString()),timestamp);
		if (!cacheDIR.exists()){
			this.processing("mkdir " + cacheDIR.getAbsolutePath());
			cacheDIR.mkdirs();
		}
			
		final File uploaded = new File(cacheDIR,cleanFileName(filename)) ;
			                
		this.processing("Uploading to "+uploaded.getAbsolutePath() + " ... ");
			
		try {
			fi.write(uploaded);
		} catch (Exception e1) {
			throw new ServerException(e1.getMessage(),e1);
		}

		this.processing("file uploaded");
		
		return cacheDIR;
	}
	
	private File merge_to_destination(String project, final String timestamp,String sessionFolder, File srcDIR) throws ClientException,ServerException{
		
		if(project==null){
			//determine correct project
			project=identifyProject(srcDIR,user);
		}
		String prearc_path;
		if(project!=null)
			prearc_path=ArcSpecManager.GetInstance().getPrearchivePathForProject(project);
		else{
			prearc_path=ArcSpecManager.GetInstance().getGlobalPrearchivePath();
		}
		
		if(sessionFolder==null){
			sessionFolder=srcDIR.getName();
		}
		
		final File destDIR=new File(new File(prearc_path,timestamp),sessionFolder);
		
		final File srcXML=new File(srcDIR.getAbsolutePath()+".xml");
		final File destXML=new File(destDIR.getAbsolutePath()+".xml");

		final XnatImagesessiondataBean src;
		try {
			src=PrearcTableBuilder.parseSession(srcXML);
		} catch (Exception e) {
			failed("Unable to parse meta-data for uploaded data.");
			throw new ClientException(Status.CLIENT_ERROR_BAD_REQUEST,e.getMessage(),e);
		}

		XnatImagesessiondataBean dest=null;
		if(destDIR.exists()){
			try {
				dest=PrearcTableBuilder.parseSession(destXML);
			} catch (Exception e) {
				failed("Unable to parse meta-data for existing data.");
				throw new ClientException(Status.CLIENT_ERROR_BAD_REQUEST,e.getMessage(),e);
			}
		}
		
		SaveHandlerI<XnatImagesessiondataBean> saveImpl=new SaveHandlerI<XnatImagesessiondataBean>() {
			public void save(XnatImagesessiondataBean merged) throws Exception {
				FileWriter fw = new FileWriter(destXML);
				merged.toXML(fw);
				fw.close();
			}
		};
		
		//pass in populated beans and root paths
		ListenerUtils.addListeners(this,new MergePrearchiveSessions(uID,srcDIR,src,src.getPrearchivepath(),destDIR,dest,dest.getPrearchivepath(),overwrite,allowDataDeletion,saveImpl))
			.call();

		org.nrg.xft.utils.FileUtils.DeleteFile(srcXML);
		org.nrg.xft.utils.FileUtils.DeleteFile(srcDIR);
		
		return destDIR;
		
	}
	
	private static String identifyProject(final File f, final XDATUser user) throws ClientException,ServerException{
		try {
			final XnatImagesessiondataI session=PrearcTableBuilder.parseSession(f);
			final String project =session.getProject();
			
			if(!StringUtils.isEmpty(project)){
				XnatProjectdata proj=XnatProjectdata.getProjectByIDorAlias(project, user, false);
				if(proj!=null){
					return proj.getId();
				}
			}
		} catch (Exception e) {
			throw new ServerException(e.getMessage(),e);
		}
		
		return null;
	}
	
	public List<File> reorganize(final File cacheDIR, final File prearcDIR,Map<String,Object> additionalValues) throws ClientException,ServerException{
			if (!prearcDIR.exists()){
			this.processing("mkdir " + prearcDIR.getAbsolutePath());
			    prearcDIR.mkdirs();
			}
			
		this.processing("Importing to " + prearcDIR.getAbsolutePath());
	    prearcDIR.mkdirs();
		
		return ListenerUtils.addListeners(this,new ImageUploadHelper(uID,this.user,TEMP_UNPACK,cacheDIR, prearcDIR,additionalValues))
			.call();
			}
			
	
	public static String cleanFileName(String filename){
		int index = filename.lastIndexOf('\\');
        if (index< filename.lastIndexOf('/'))index = filename.lastIndexOf('/');
        if(index>0)filename = filename.substring(index+1);
        return filename;
	}
}
