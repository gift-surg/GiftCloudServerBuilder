/*
 * org.nrg.xnat.archive.PrearcSessionValidator
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.archive;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.xdat.model.CatCatalogI;
import org.nrg.xdat.model.CatDcmentryI;
import org.nrg.xdat.model.CatEntryI;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.model.XnatResourcecatalogI;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.event.EventUtils;
import org.nrg.xnat.helpers.merge.MergePrearcToArchiveSession;
import org.nrg.xnat.helpers.merge.MergeSessionsA.SaveHandlerI;
import org.nrg.xnat.helpers.merge.MergeUtils;
import org.nrg.xnat.restlet.actions.PrearcImporterA.PrearcSession;
import org.nrg.xnat.turbine.utils.XNATUtils;
import org.nrg.xnat.utils.CatalogUtils;
import org.nrg.xnat.utils.CatalogUtils.CatEntryFilterI;
import org.xml.sax.SAXException;

import com.google.common.collect.Lists;

/**
 * @author Timothy R. Olsen <tim@deck5consulting.com>
 *
 * This is a copy of PrearcSessionArchiver to simply validates if the PrearcSessionArchiver would succeed, and records anywhere where it wouldn't.
 *
 * 1- Session already exists
 * 2- Session label modification via archiving process
 * 3- Project modification via archiving process
 * 4- Subject modification via archiving process
 * 5- Study Instance UID mis-match
 * 6- Unable to identify destination project for data
 * 7- Processing exception during metadata population
 * 8- Unable to identify a session label for the data
 * 9- Unable to create new session ID
 * 10- Concurrent processing job (workflow entry)
 * 11- Unable to identify subject label for session data
 * 12- Operation will create a new subject entry
 * 13- Meta-data validation exception
 * 14- File or catalog entry conflict (overwriting data)
 * 15- Processing exception during comparison of new files vs old files
 * 16- Session already contains a scan with the same series UID and ID
 * 17- Session already contains a scan with the same ID, but a different series UID
 * 18- Session already contains a scan with the same series UID, but a different ID
 * 19- Illegal session modality modification
 * 20- Unexpected files or file references
 * 21- Missing referenced files
 */
public final class PrearcSessionValidator extends PrearcSessionArchiver  {
	
	protected PrearcSessionValidator(final XnatImagesessiondata src, final PrearcSession prearcSession, final XDATUser user, final String project,final Map<String,Object> params) {
		super(src,prearcSession,user,project,params,false,true,false,false);
	}

	public PrearcSessionValidator(final PrearcSession session,final XDATUser user, final Map<String,Object> params)	throws IOException,SAXException {
		super(session,user,params,false,true,false,false);
	}



	/**
	 * This method overwrites the one in archiver so that multiple exceptions can be recorded, rather than just the first one.
	 * @return
	 */
	public void checkForConflicts(final XnatImagesessiondata src, final File srcDIR, final XnatImagesessiondata existing, final File destDIR) {
		if(existing!=null){
			//it already exists
			conflict(1,PRE_EXISTS);

			//check if this would change the label (not allowed)
			if(!StringUtils.equals(src.getLabel(),existing.getLabel())){
				this.fail(2,LABEL_MOD);
			}
	
			//check if this would change the project (not allowed)
			if(!StringUtils.equals(existing.getProject(),src.getProject())){
				fail(3,PROJ_MOD);
			}
	
			//check if this would change the subject (not allowed)
			if(!StringUtils.equals(existing.getSubjectId(),src.getSubjectId())){
				String subjectId = existing.getLabel();
				String newError = SUBJECT_MOD + ": " + subjectId + " Already Exists for another Subject";
				fail(4,newError);
			}
			
			//check if the UIDs match
			if(StringUtils.isNotEmpty(existing.getUid()) && StringUtils.isNotEmpty(src.getUid())){
				if(!StringUtils.equals(existing.getUid(), src.getUid())){
					conflict(5,UID_MOD);
				}
			}
			
			//check if the XSI types match
			if(!StringUtils.equals(existing.getXSIType(), src.getXSIType())){
				fail(19,MODALITY_MOD);
			}
			
			for(final XnatImagescandataI newScan : src.getScans_scan()){
				XnatImagescandataI match=MergeUtils.getMatchingScanById(newScan.getId(), existing.getScans_scan());//match by ID
				if(match!=null){
					if(StringUtils.equals(match.getUid(),newScan.getUid())){
						conflict(16,"Session already contains a scan (" + match.getId() +") with the same UID and number.");
					}else{
						conflict(17,"Session already contains a scan (" + match.getId() +") with the same number, but a different UID. - New scan will be renamed to " + match.getId() + "_1.");
					}
				}
				
				XnatImagescandataI match2=MergeUtils.getMatchingScanByUID(newScan, existing.getScans_scan());//match by UID
				if(match2!=null){
					if(match==null || !StringUtils.equals(match.getId(),newScan.getId())){
						conflict(18,"Session already contains a scan (" + match2.getId() +") with the same UID, but a different number.");
					}
				}
			}
		}
	}
	
	public String call(){
		return null;
	}

	/**
	 * Mimics the behavior of PrearcSessionArchiver.call(), but tracks the exceptions, rather then failing on them.
	 * @return
	 */
	public List<PrearcSessionValidator.Notice> validate()    {
		if(StringUtils.isEmpty(project)){
			fail(6,"unable to identify destination project");
		}
		try {
			populateAdditionalFields();
		} catch (ClientException e1) {
			fail(7,e1.getMessage());//this is a processing exception
		}

		try {
			fixSessionLabel();
		} catch (ClientException e1) {
			fail(8,e1.getMessage());//this means the code couldn't identify the session label.
		}
		
		XnatImagesessiondata existing = retrieveExistingExpt();
		
		if(existing==null){
			try {
				if(!XNATUtils.hasValue(src.getId()))src.setId(XnatExperimentdata.CreateNewID());
			} catch (Exception e) {
				fail(9,"unable to create new session ID");
			}
		}else{
			src.setId(existing.getId());
			try {
				preventConcurrentArchiving(existing.getId(),user);
			} catch (ClientException e) {
				conflict(10,e.getMessage());//this means there is an open workflow entry
			}
		}
		

		try {
			fixSubject(EventUtils.TEST_EVENT(user),false);
		} catch (ClientException e1) {
			fail(11,e1.getMessage());//this means the action couldn't identify the subject
		} catch (ServerException e1) {
			warn(12,e1.getMessage());//this just means the action was going to create a new subject
		}

		
		try {
			validateSesssion();
		} catch (ServerException e1) {
			fail(13,e1.getMessage());//this is some sort of schema validation exception
		}

		File arcSessionDir;
		try {
			arcSessionDir = getArcSessionDir();
		} catch (Exception e) {
			return notices;
		}

		if(existing!=null)checkForConflicts(src,this.prearcSession.getSessionDir(),existing,arcSessionDir);

		SaveHandlerI<XnatImagesessiondata> saveImpl=new SaveHandlerI<XnatImagesessiondata>() {
			public void save(XnatImagesessiondata merged) throws Exception {					
				
			}
		};

		MergePrearcToArchiveSession merger=new MergePrearcToArchiveSession(src.getPrearchivePath(),
																		 this.prearcSession.getSessionDir(),
																		 src,
																		 src.getPrearchivepath(),
																		 arcSessionDir,
																		 existing,
																		 arcSessionDir.getAbsolutePath(),
																		 true, 
																		 false,
																		 saveImpl,user,EventUtils.TEST_EVENT(user));

		try {
			merger.checkForConflict();
		} catch (ClientException e) {
			conflict(14,e.getMessage());
		} catch (ServerException e) {
			fail(15,e.getMessage());
		}
		

		//validate files to confirm DICOM contents
		for(final XnatImagescandataI scan: src.getScans_scan()){
			for(final XnatAbstractresourceI resource:scan.getFile()){
				if(resource instanceof XnatResourcecatalogI){
					final File f=CatalogUtils.getCatalogFile(src.getPrearchivepath(), (XnatResourcecatalogI)resource);
					if(f==null || !f.exists()){
						warn(21,"Expected a catalog file, however it was missing.");
					}
					
					final List<File> unreferenced=CatalogUtils.getUnreferencedFiles(f.getParentFile());
					if(unreferenced.size()>0){
						warn(20,String.format("Scan %1$s has %2$s non-%3$s (or non-parsable %3$s) files", scan.getId(),unreferenced.size(),resource.getLabel()));
					}
					
					if(StringUtils.equals(resource.getLabel(),"DICOM")){
						//check for entries that aren't DICOM entries or don't have a UID stored
						final CatCatalogI cat=CatalogUtils.getCatalog(f);
						final Collection<CatEntryI> nonDCM=CatalogUtils.getEntriesByFilter(cat, new CatEntryFilterI(){
							@Override
							public boolean accept(CatEntryI entry) {
								return ((!(entry instanceof CatDcmentryI)) || StringUtils.isEmpty(((CatDcmentryI)entry).getUid()));
							}});
						
						if(nonDCM.size()>0){
							warn(20,String.format("Scan %1$s has %2$s non-DICOM (or non-parsable DICOM) files", scan.getId(),nonDCM.size()));
						}
					}
				}
			}
		}

		return notices;

	}
		
	public static abstract class Notice{
		final int code;
		final String msg;
		public Notice(int code,String msg){
			this.code=code;
			this.msg=msg;
		}
		
		public int getCode(){
			return code;
		}
		
		public String getMessage(){
			return msg;
		}
		
		public abstract String getType();
	}
	
	public class Warning extends Notice{
		public Warning(int code, String msg) {
			super(code, msg);
		}

		@Override
		public String getType() {
			return "WARN";
		}
	}
	
	public class Failure extends Notice{
		public Failure(int code, String msg) {
			super(code, msg);
		}

		@Override
		public String getType() {
			return "FAIL";
		}
	}
	
	public class Conflict extends Notice{
		public Conflict(int code, String msg) {
			super(code, msg);
		}

		@Override
		public String getType() {
			return "CONFLICT";
		}
	}
	
	private List<Notice> notices=Lists.newArrayList();
	
	protected void fail(int code, String msg){
		notices.add(new Failure(code, msg));
	}
	protected void warn(int code, String msg){
		notices.add(new Warning(code, msg));
	}
	protected void conflict(int code, String msg){
		notices.add(new Conflict(code, msg));
	}
}
