/**
 * Copyright 2010 Washington University
 */
package org.nrg.xnat.helpers.resource.direct;

import java.io.File;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.nrg.xdat.bean.CatCatalogBean;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.exceptions.InvalidArchiveStructure;
import org.nrg.xnat.helpers.resource.XnatResourceInfo;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.nrg.xnat.utils.CatalogUtils;

/**
 * @author timo
 *
 */
public abstract class ResourceModifierA {
	final boolean overwrite;
	final XDATUser user;
	
	public ResourceModifierA(final boolean overwrite, final XDATUser user){
		this.overwrite=overwrite;
		this.user=user;
	}
	
//	private boolean saveFile(final FileWriterWrapperI fi,final String relativePath,final String type, final XnatResource resource, final XDATUser user, final XnatResourceInfo info) throws IOException,FileNotFoundException,Exception{
//		CatalogUtils.configureEntry(resource, info, user);
//		
//		final String dest_path=this.buildDestinationPath();
//
//        final String resourceFolder=resource.getLabel();
//        
//		File saveTo=null;
//		if(StringUtils.isBlank(resourceFolder)){
//			saveTo = new File(new File(dest_path,getDefaultUID()),relativePath);
//		}else{
//			saveTo = new File(new File(dest_path,resourceFolder),relativePath);
//		}
//		
//		saveTo.getParentFile().mkdirs();
//		
//		fi.write(saveTo);
//		
//
//		resource.setUri(saveTo.getAbsolutePath());
//		
//		return addResource(resource,type,user);
//	}
	
	private boolean createCatalog(XnatResourcecatalog resource, XnatResourceInfo info) throws Exception{
		CatalogUtils.configureEntry(resource, info, user);
		
		final String dest_path=this.buildDestinationPath();
        
        CatCatalogBean cat = new CatCatalogBean();
		if(resource.getLabel()!=null){
			cat.setId(resource.getLabel());
		}else{
			cat.setId(getDefaultUID());
		}
        
		File saveTo = new File(new File(dest_path,cat.getId()),cat.getId() + "_catalog.xml");
		saveTo.getParentFile().mkdirs();
		
		CatalogUtils.writeCatalogToFile(cat, saveTo);		

		resource.setUri(saveTo.getAbsolutePath());
		
		return true;
	}
	
	public boolean addFile(final List<FileWriterWrapperI> fws, final Object resourceIdentifier, final String type, final String filepath, final XnatResourceInfo info, final boolean extract) throws Exception{
		final XnatAbstractresourceI abst=getResourceByIdentifier(resourceIdentifier,type);
		
		if(abst==null){
			//new resource
			final XnatResourcecatalog catRes=new XnatResourcecatalog((UserI)user);
			
			createCatalog(catRes, info);
			
			addResource(catRes, type, user);
		}else{
			if(!(abst instanceof XnatResourcecatalog)){
				throw new Exception("Conflict:Non-catalog resource already exits.");
			}
		}
		
		boolean _return=true;
		for(final FileWriterWrapperI fw:fws){
			if(!CatalogUtils.storeCatalogEntry(fw, filepath, (XnatResourcecatalog)abst, getProject(), extract, info,overwrite)){
				_return=false;
			}
		}
		return _return;
	}
	
	public XnatAbstractresourceI getResourceByIdentifier(final Object resourceIdentifier,final String type){
		XnatAbstractresourceI abst=null;
		
		if(resourceIdentifier instanceof Integer){
			abst=getResourceById((Integer)resourceIdentifier,type);
		}
		
		if(abst!=null){
			return abst;
		}
		
		abst=getResourceByLabel(resourceIdentifier.toString(),type);
		
		if(abst!=null){
			return abst;
		}
		
		if(StringUtils.isNumeric(resourceIdentifier.toString())){
			abst=getResourceById(Integer.valueOf(resourceIdentifier.toString()), type);
		}
		
		return abst;
	}
	
	protected static String getDefaultUID(){
		java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat ("yyyyMMdd_HHmmss");
        return formatter.format(Calendar.getInstance().getTime());
	}
	
	protected abstract String buildDestinationPath() throws InvalidArchiveStructure;
	protected abstract XnatAbstractresourceI getResourceById(final Integer i, final String type);
	protected abstract XnatAbstractresourceI getResourceByLabel(final String lbl, final String type);
	
	public abstract XnatProjectdata getProject();
	
	public abstract boolean addResource(final XnatResource resource, final String type, final XDATUser user) throws Exception;
	public String getRootPath(){
		return getProject().getRootArchivePath();
	}
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
