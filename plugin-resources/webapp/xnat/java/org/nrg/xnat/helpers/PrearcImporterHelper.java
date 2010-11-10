/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.xnat.helpers;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.nrg.status.StatusListenerI;
import org.nrg.status.StatusProducer;
import org.nrg.xdat.om.ArcArchivespecification;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.nrg.xnat.restlet.util.XNATRestConstants;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.nrg.xnat.turbine.utils.ImageUploadHelper;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

public class PrearcImporterHelper extends StatusProducer implements Callable<Multimap<String,Object>>{
    public static final String ADDITIONAL_VALUES = "ADDITIONAL_VALUES";
	public static final String UPLOAD_ID = "UPLOAD_ID";
	static org.apache.log4j.Logger logger = Logger.getLogger(PrearcImporterHelper.class);
	
	private final FileWriterWrapperI fi;
	private final String project;
	private final XDATUser user;
	
	private final Map<String,Object> additionalValues;
	private final Object uID;
	
	/**
	 * Helper class to extract a passed zip into the prearchive.
	 * @param uID2
	 * @param u
	 * @param fi
	 * @param project
	 * @param additionalValues
	 */
	public PrearcImporterHelper(final Object uID2, final XDATUser u, final FileWriterWrapperI fi, final String project,Map<String,Object> additionalValues){
    	super((uID2==null)?u:uID2);
    	this.user=u;
    	this.uID=uID2;
		this.fi=fi;
		this.project=project;
		
		this.additionalValues=additionalValues;
	}
	
	public Multimap<String,Object> call() throws IOException, Exception{
		
			final String timestamp=(new java.text.SimpleDateFormat(XNATRestConstants.PREARCHIVE_TIMESTAMP)).format(Calendar.getInstance().getTime());
			
			final String filename = fi.getName();
			this.processing("Importing file (" + filename + ") for project: "+project);
			
			final ArcArchivespecification spec=ArcSpecManager.GetInstance();
			
			String prearchive_path = spec.getPrearchivePathForProject(project);
			String cachepath= spec.getCachePathForProject(project);
			                
			//BUILD PREARCHIVE PATH
			prearchive_path +=timestamp + File.separator;
			File prearcDIR = new File(prearchive_path);
			
			this.processing("mkdir " + prearchive_path);
			if (!prearcDIR.exists()){
			    prearcDIR.mkdirs();
			}
			
			//BUILD TEMPORARY PATH        
			cachepath += "uploads" + File.separator + user.getXdatUserId() + File.separator + timestamp + File.separator;
			final File cacheDIR = new File(cachepath);
			if (!cacheDIR.exists()){
				cacheDIR.mkdirs();
				this.processing("mkdir " + cachepath);
			}
			
			final File uploaded = new File(cachepath + cleanFileName(filename)) ;
			
			this.processing("Uploading to "+uploaded.getAbsolutePath() + " ... ");
			
			fi.write(uploaded);

			this.processing("file uploaded");
			
			final ImageUploadHelper helper = new ImageUploadHelper(uID,this.user,project,cacheDIR, prearcDIR,additionalValues);
			
			for(final StatusListenerI listener: this.getListeners()){
				helper.addStatusListener(listener);
			}
			
			this.processing("Prearchive:" + prearcDIR.getAbsolutePath());
			
			final Multimap<String,Object> results=helper.call();
			
	        final Multimap<String,Object> response= LinkedHashMultimap.create();
			
			final List<PrearcSession> sessions= new ArrayList<PrearcSession>();
			
			for(final Object o:results.get(ImageUploadHelper.SESSIONS_RESPONSE)){
				response.put(ImageUploadHelper.SESSIONS_RESPONSE,new PrearcSession((File)o,new URI(StringUtils.join(new String[]{"REST/prearchives/projects/",project,"/",timestamp,"/",((File)o).getName()}))));
			}
			
			return response;
	}
	
	public class PrearcSession{
		private final File sessionDIR;
		private final URI url;
		
		private PrearcSession(final File f, final URI u){
			sessionDIR=f;
			url=u;
		}

		public File getSessionDIR() {
			return sessionDIR;
		}

		public URI getUrl() {
			return url;
		}
	}
	
	public static String cleanFileName(String filename){
		int index = filename.lastIndexOf('\\');
        if (index< filename.lastIndexOf('/'))index = filename.lastIndexOf('/');
        if(index>0)filename = filename.substring(index+1);
        return filename;
	}
}
