/**
 * Copyright 2010 Washington University
 */
package org.nrg.xnat.helpers.resource.direct;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;

import org.apache.commons.lang.StringUtils;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImageassessordata;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatReconstructedimagedata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xnat.exceptions.InvalidArchiveStructure;
import org.nrg.xnat.helpers.FileWriterWrapper;
import org.nrg.xnat.helpers.resource.XnatResourceInfo;
import org.nrg.xnat.utils.CatalogUtils;
import org.restlet.data.Status;

/**
 * @author timo
 *
 */
public abstract class DirectResourceModifierA {
	
	public boolean saveFile(final FileWriterWrapper fi,final String relativePath, final XnatResource resource, final XDATUser user, final XnatResourceInfo info) throws IOException,FileNotFoundException,Exception{
		CatalogUtils.configureEntry(resource, info, user);
		
		final String dest_path=this.buildDestinationPath();

        final String resourceFolder=resource.getLabel();
        
		File saveTo=null;
		if(StringUtils.isBlank(resourceFolder)){
			saveTo = new File(new File(dest_path,getDefaultUID()),relativePath);
		}else{
			saveTo = new File(new File(dest_path,resourceFolder),relativePath);
		}
		
		saveTo.getParentFile().mkdirs();
		
		fi.write(saveTo);
		

		resource.setUri(saveTo.getAbsolutePath());
		
		return addResource(resource,user);
	}
	
	protected static String getDefaultUID(){
		java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat ("yyyyMMdd_HHmmss");
        return formatter.format(Calendar.getInstance().getTime());
	}
	
	protected abstract String buildDestinationPath() throws InvalidArchiveStructure;
	protected abstract boolean addResource(final XnatResource resource, final XDATUser user) throws Exception;
//		
//	public static boolean storeResourceFile(final FileWriterWrapper fi,final String relativePath, final XnatResource resource, final XDATUser user, final XnatResourceInfo info) throws IOException,Exception{
//		java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat ("yyyyMMdd_HHmmss");
//        String uploadID = formatter.format(Calendar.getInstance().getTime());
//	    
//		XnatExperimentdata assessed=null;
//		if(assesseds.size()==1)assessed=assesseds.get(0);
//        
//		if(recons.size()>0){
//			//reconstruction			
//			
//		}else if(scans.size()>0){
//			//scan
//
//		}else if(expts.size()>0){
//			XnatExperimentdata expt=this.expts.get(0);
////			experiment
//			XnatExperimentdata session=null;
//			
//			String dest_path=null;
//			if(expt.getItem().instanceOf("xnat:imageAssessorData")){
//				session = (XnatImagesessiondata)assessed;
//				if(expt.getId()!=null && !expt.getId().equals("")){
//					uploadID=expt.getId();
//				}
//				dest_path = FileUtils.AppendRootPath(((XnatImagesessiondata)session).getCurrentSessionFolder(true), "ASSESSORS/" + uploadID +"/");
//			}else{
//				if(!expt.getItem().instanceOf("xnat:imageSessionData")){
//					session = (XnatExperimentdata)expt;
//					dest_path = FileUtils.AppendRootPath(proj.getRootArchivePath(), expt.getId() + "/RESOURCES/" + uploadID +"/");
//				}else{
//					session = (XnatImagesessiondata)expt;
//					dest_path = FileUtils.AppendRootPath(((XnatImagesessiondata)session).getCurrentSessionFolder(true), "RESOURCES/" + uploadID +"/");
//				}
//			}
//
//			
//
//			if(expt.getItem().instanceOf("xnat:imageAssessorData")){
//				XnatImageassessordata iad = (XnatImageassessordata)expt;
//				if(type!=null){
//					if(type.equals("in")){
//						iad.setIn_file(resource);
//					}else{
//						iad.setOut_file(resource);
//					}
//				}else{
//					iad.setOut_file(resource);
//				}
//				
//				iad.save(user, false, false);
//				
//			}else{
//				session.setResources_resource(resource);
//				
//				session.save(user, false, false);
//			}
//			return true;
//		}else if(sub!=null){
//
//		}else if(proj!=null){
//			String dest_path=null;
//			dest_path = FileUtils.AppendRootPath(proj.getRootArchivePath(), "resources/");
//
//			
//			resource.setUri(saveTo.getAbsolutePath());
//			proj.setResources_resource(resource);
//			
//			proj.save(user, false, false);
//			return true;
//		}
//		return false;
//	}
}
