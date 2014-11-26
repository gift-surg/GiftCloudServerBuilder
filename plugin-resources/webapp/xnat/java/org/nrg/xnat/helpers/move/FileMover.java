/*
 * org.nrg.xnat.helpers.move.FileMover
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.helpers.move;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import org.nrg.action.ActionException;
import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xnat.helpers.file.StoredFile;
import org.nrg.xnat.helpers.resource.XnatResourceInfo;
import org.nrg.xnat.helpers.resource.direct.DirectResourceModifierBuilder;
import org.nrg.xnat.helpers.resource.direct.ResourceModifierA;
import org.nrg.xnat.helpers.resource.direct.ResourceModifierBuilderI;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.UriParserUtils;
import org.nrg.xnat.helpers.uri.archive.*;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.nrg.xnat.utils.UserUtils;
import org.restlet.data.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.Date;
import java.util.List;

public class FileMover {
	private final static Logger logger = LoggerFactory.getLogger(FileMover.class);
	
	final boolean overwrite;
	final XDATUser user;
	final ListMultimap<String,Object> params;
	
	public FileMover(Boolean overwrite, XDATUser user, ListMultimap<String,Object> params){
		if(overwrite==null){
			this.overwrite=false;
		}else{
			this.overwrite=overwrite;
		}
		
		this.user=user;
		this.params=params;
	}
	
	public Boolean call(org.nrg.xnat.helpers.uri.URIManager.UserCacheURI src,ResourceURII dest,EventMetaI ci) throws Exception {
		File srcF;		
		if(src.getProps().containsKey(UriParserUtils._REMAINDER)){
			srcF=UserUtils.getUserCacheFile(user, (String)src.getProps().get(URIManager.XNAME), (String)src.getProps().get(UriParserUtils._REMAINDER));
		}else{
			srcF=UserUtils.getUserCacheFile(user, (String)src.getProps().get(URIManager.XNAME));
		}
		
		
		
		final String label = dest.getResourceLabel();
		
		String filepath=dest.getResourceFilePath();
		if(filepath!=null && filepath.equals("/")){
			filepath=null;
		}
		
		final String type=(String)dest.getProps().get(URIManager.TYPE);
						
		this.buildResourceModifier(dest,overwrite,type,ci).addFile(
				(List<? extends FileWriterWrapperI>)Lists.newArrayList(new StoredFile(srcF,overwrite)),
				label,
				type, 
				filepath, 
				this.buildResourceInfo(ci),
				overwrite);
		
		return Boolean.TRUE;
	}
	
    public XnatResourceInfo buildResourceInfo(EventMetaI ci){        		
		final String description;
	    if(!CollectionUtils.isEmpty(params.get("description"))){
	    	description=(String)(this.params.get("description").get(0));
	    }else{
	    	description=null;
	    }
	    
	    final String format;
	    if(!CollectionUtils.isEmpty(params.get("format"))){
	    	format=(String)(this.params.get("format").get(0));
	    }else{
	    	format=null;
	    }
	    
	    final String content;
	    if(!CollectionUtils.isEmpty(params.get("content"))){
	    	content=(String)(this.params.get("content").get(0));
	    }else{
	    	content=null;
	    }
	    
	    String[] tags;
	    if(!CollectionUtils.isEmpty(params.get("tags"))){
	    	tags = (String[])this.params.get("tags").toArray();
	    }else{
	    	tags=null;
	    }
        
	    Date now=EventUtils.getEventDate(ci, false);
		return XnatResourceInfo.buildResourceInfo(description, format, content, tags,user,now,now,EventUtils.getEventId(ci));
	}

	protected ResourceModifierA buildResourceModifier(final ResourceURII arcURI,final boolean overwrite, final String type, final EventMetaI ci) throws ActionException{
		XnatImagesessiondata assessed=null;
			
		
		
		if(arcURI instanceof AssessedURII)assessed=((AssessedURII)arcURI).getSession();
			
		//this should allow dependency injection - TO
		
		try {
			if(!user.canEdit(arcURI.getSecurityItem())){
				throw new ClientException(Status.CLIENT_ERROR_FORBIDDEN, new Exception("Unauthorized attempt to add a file to "+ arcURI.getUri()));
			}
		} catch (Exception e) {
			logger.error("",e);
			throw new ClientException(Status.CLIENT_ERROR_FORBIDDEN, e);
		}

		final ResourceModifierBuilderI builder=new DirectResourceModifierBuilder();
		
		if(arcURI instanceof ReconURII){
			//reconstruction						
			builder.setRecon(assessed,((ReconURII)arcURI).getRecon(), type);
		}else if(arcURI instanceof ScanURII){
			//scan
			builder.setScan(assessed, ((ScanURII)arcURI).getScan());
		}else if(arcURI instanceof AssessorURII){//			experiment
			builder.setAssess((XnatImagesessiondata)assessed, ((AssessorURII)arcURI).getAssessor(), type);
		}else if(arcURI instanceof ExperimentURII){
			XnatExperimentdata expt=((ExperimentURII)arcURI).getExperiment();
			builder.setExpt(expt.getPrimaryProject(false),expt);
		}else if(arcURI instanceof SubjectURII){
			XnatSubjectdata sub=((SubjectURII)arcURI).getSubject();
			builder.setSubject(sub.getPrimaryProject(false), sub);
		}else if(arcURI instanceof ProjectURII){
			builder.setProject(((ProjectURII)arcURI).getProject());
		}else if(arcURI instanceof PseudonymURII){
			builder.setPseudonym(((PseudonymURII)arcURI).getPseudonym());
		}else{
			throw new ClientException("Unsupported resource:"+arcURI.getUri());
		}
		
		try {
			return builder.buildResourceModifier(overwrite,user,ci);
		} catch (Exception e) {
			throw new ServerException(e);
		}
	}
}
