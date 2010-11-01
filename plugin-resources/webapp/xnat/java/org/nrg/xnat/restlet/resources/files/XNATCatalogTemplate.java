// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources.files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.zip.ZipOutputStream;

import org.apache.commons.fileupload.FileItem;
import org.nrg.xdat.bean.CatCatalogBean;
import org.nrg.xdat.bean.CatEntryBean;
import org.nrg.xdat.bean.CatEntryMetafieldBean;
import org.nrg.xdat.bean.CatEntryTagBean;
import org.nrg.xdat.model.CatCatalogBeanI;
import org.nrg.xdat.model.CatCatalogI;
import org.nrg.xdat.model.CatEntryBeanI;
import org.nrg.xdat.model.CatEntryI;
import org.nrg.xdat.model.CatEntryMetafieldI;
import org.nrg.xdat.model.CatEntryTagI;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatAbstractresourceTag;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImageassessordata;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatReconstructedimagedata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xft.utils.zip.TarUtils;
import org.nrg.xft.utils.zip.ZipI;
import org.nrg.xft.utils.zip.ZipUtils;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;

public class XNATCatalogTemplate extends XNATTemplate {
	XFTTable catalogs=null;
	
	ArrayList<String> resource_ids=null;
	ArrayList<XnatAbstractresource> resources=new ArrayList<XnatAbstractresource>();
	
	public XNATCatalogTemplate(Context context, Request request,
			Response response,boolean allowAll) {
		super(context, request, response);
		
		String resourceID= (String)request.getAttributes().get("RESOURCE_ID");

		if(resourceID!=null){
			resource_ids=new ArrayList<String>();
			for(String s:StringUtils.CommaDelimitedStringToArrayList(resourceID, true)){
				resource_ids.add(s);
				}
			}
			
			try {
			catalogs=this.loadCatalogs(resource_ids,true,allowAll);
			} catch (Exception e) {
	            logger.error("",e);
			}
	}
	
	
	public String getBaseURI() throws ElementNotFoundException{
		StringBuffer sb =new StringBuffer("/REST");
		if(proj!=null && sub!=null){
			sb.append("/projects/");
			sb.append(proj.getId());
			sb.append("/subjects/");
			sb.append(sub.getId());
		}
		if(recons.size()>0){
			sb.append("/experiments/");
			int aC=0;
			for(XnatExperimentdata assessed:this.assesseds){
				if(aC++>0)sb.append(",");
			sb.append(assessed.getId());
			}
			sb.append("/reconstructions/");
			int sC=0;
			for(XnatReconstructedimagedata recon:recons){
				if(sC++>0)sb.append(",");
			sb.append(recon.getId());
			}
			if(type!=null){
				sb.append("/" + type);
			}
		}else if(scans.size()>0){
			sb.append("/experiments/");
			int aC=0;
			for(XnatExperimentdata assessed:this.assesseds){
				if(aC++>0)sb.append(",");
			sb.append(assessed.getId());
			}
			sb.append("/scans/");
			int sC=0;
			for(XnatImagescandata scan:scans){
				if(sC++>0)sb.append(",");
			sb.append(scan.getId());
			}
		}else if(expts.size()>0){
			if(assesseds.size()>0){
				sb.append("/experiments/");
				int aC=0;
				for(XnatExperimentdata assessed:this.assesseds){
					if(aC++>0)sb.append(",");
				sb.append(assessed.getId());
				}
				sb.append("/assessors/");
				int eC=0;
				for(XnatExperimentdata expt:this.expts){
					if(eC++>0)sb.append(",");
				sb.append(expt.getId());
				}
				if(type!=null){
					sb.append("/" + type);
				}
			}else{
				sb.append("/experiments/");
				int eC=0;
				for(XnatExperimentdata expt:this.expts){
					if(eC++>0)sb.append(",");
				sb.append(expt.getId());
			}
			}
		}else if(sub!=null){
			
		}else if(proj!=null){
			sb.append("/projects/");
			sb.append(proj.getId());
		}
		return sb.toString();
	}


	public final static String[] FILE_HEADERS = {"Name","Size","URI","collection","file_tags","file_format","file_content","cat_ID"};
	public final static String[] FILE_HEADERS_W_FILE = {"Name","Size","URI","collection","file_tags","file_format","file_content","cat_ID","file"};
	
	protected List<Object[]> getEntryDetails(CatCatalogBean cat, String parentPath,String uriPath,XnatResource _resource, String coll_tags,boolean includeFile, final CatEntryFilterI filter){
		final ArrayList al = new ArrayList();
		for(final CatCatalogBean subset:cat.getSets_entryset()){
			al.addAll(getEntryDetails(subset,parentPath,uriPath,_resource,coll_tags,includeFile,filter));
		}
		
		final int ri=(includeFile)?9:8;
		for(final CatEntryBean entry:cat.getEntries_entry()){
			if(filter==null || filter.accept(entry)){
				final Object[] row = new Object[ri];
				final String entryPath = StringUtils.ReplaceStr(FileUtils.AppendRootPath(parentPath,entry.getUri()),"\\","/");
	            final File f=getFileOnLocalFileSystem(entryPath);
	            row[0]=(f.getName());
	            row[1]=(f.length());
	            if(FileUtils.IsAbsolutePath(entry.getUri())){
	                row[2]=uriPath+"/" + entry.getId();
	            }else{
	                row[2]=uriPath+"/" + entry.getUri();
	            }
	            row[3]=_resource.getLabel();
	            
	            row[4]="";
	            for(CatEntryMetafieldBean meta: entry.getMetafields_metafield()){
	            	if(!row[4].equals(""))row[4]=row[4] +",";
	            	row[4]=row[4]+meta.getName() + "=" + meta.getMetafield();
	            }
	            for(CatEntryTagBean tag: entry.getTags_tag()){
	            	if(!row[4].equals(""))row[4]=row[4]+",";
	            	row[4]=row[4]+tag.getTag();
	            }
	            row[5]=entry.getFormat();
	            row[6]=entry.getContent();
	            row[7]=_resource.getXnatAbstractresourceId();
	            if(includeFile)row[8]=f;
	            al.add(row);
			}
		}
		
		return al;
	}
	
	public interface CatEntryFilterI{
		public boolean accept(final CatEntryBean entry);
	}
    
    protected ArrayList<File> getFiles(CatCatalogBeanI cat,String parentPath){
    	ArrayList<File> al = new ArrayList<File>();
		for(CatCatalogBeanI subset:cat.getSets_entryset()){
			al.addAll(getFiles(subset,parentPath));
		}
		
		for(CatEntryBeanI entry:cat.getEntries_entry()){
			String entryPath = StringUtils.ReplaceStr(FileUtils.AppendRootPath(parentPath,entry.getUri()),"\\","/");
            File f=getFileOnLocalFileSystem(entryPath);
            
			if(f!=null)
				al.add(f);
		}
		
		return al;
	}
	
	protected CatEntryI getEntryByURI(CatCatalogI cat, String name){
		CatEntryI e=null;
		for(CatCatalogI subset:cat.getSets_entryset()){
			e = getEntryByURI(subset,name);
			if(e!=null) return e;
		}
		
		for(CatEntryI entry:cat.getEntries_entry()){
			try {
				String decoded=URLDecoder.decode(name);
				if(entry.getUri().equals(name) ||  entry.getUri().equals(decoded)){
				return entry;
			}
			} catch (Exception e1) {
				logger.error(e1);
			}
		}
		
		return null;
	}
	
	protected CatEntryBean getEntryByFilter(final CatCatalogBean cat, final CatEntryFilterI filter){
		CatEntryBean e=null;
		for(CatCatalogBean subset:cat.getSets_entryset()){
			e = getEntryByFilter(subset,filter);
			if(e!=null) return e;
		}
		
		for(CatEntryBean entry:cat.getEntries_entry()){
			try {
				if(filter.accept(entry)){
    				return entry;
    			}
			} catch (Exception e1) {
				logger.error(e1);
			}
		}
		
		return null;
	}
	
	protected CatEntryI getEntryByName(CatCatalogI cat, String name){
		CatEntryI e=null;
		for(CatCatalogI subset:cat.getSets_entryset()){
			e = getEntryByName(subset,name);
			if(e!=null) return e;
		}
		
		for(CatEntryI entry:cat.getEntries_entry()){
			String decoded=URLDecoder.decode(name);
			if(entry.getName().equals(name) ||  entry.getName().equals(decoded)){
				return entry;
			}
		}
		
		return null;
	}
	
	protected CatEntryI getEntryById(CatCatalogI cat, String name){
		CatEntryI e=null;
		for(CatCatalogI subset:cat.getSets_entryset()){
			e = getEntryByName(subset,name);
			if(e!=null) return e;
		}
		
		for(CatEntryI entry:cat.getEntries_entry()){
			if(entry.getId().equals(name)){
				return entry;
			}
		}
		
		return null;
	}

    protected File getFileOnLocalFileSystem(String fullPath) {
        File f = new File(fullPath);
        if (!f.exists()){
            if (!fullPath.endsWith(".gz")){
            	f= new File(fullPath + ".gz");
            	if (!f.exists()){
            		return null;
            	}
            }else{
                return null;
            }
        }
        
        return f;
    }
	
	public boolean storeResourceFile(FileWriterWrapper fi,String relativePath, XnatAbstractresource abst) throws IOException,Exception{
		XnatResource resource=(XnatResource)abst;
		StringBuffer sb = new StringBuffer();
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat ("yyyyMMdd_HHmmss");
        String uploadID = formatter.format(Calendar.getInstance().getTime());
        
        String resourceFolder=resource.getLabel();
        
	    if(this.getQueryVariable("description")!=null){
	    	resource.setDescription(this.getQueryVariable("description"));
	    }
	    if(this.getQueryVariable("format")!=null){
	    	resource.setFormat(this.getQueryVariable("format"));
	    }
	    if(this.getQueryVariable("content")!=null){
	    	resource.setContent(this.getQueryVariable("content"));
	    }
	    
	    if(this.getQueryVariables("tags")!=null){
	    	String[] tags = this.getQueryVariables("tags");
	    	for(String tag: tags){
	    		tag = tag.trim();
	    		if(!tag.equals("")){
	    			for(String s:StringUtils.CommaDelimitedStringToArrayList(tag)){
	    				s=s.trim();
	    				if(!s.equals("")){
	    					XnatAbstractresourceTag t = new XnatAbstractresourceTag((UserI)user);
	    		    		if(s.indexOf("=")>-1){
	    		    			t.setName(s.substring(0,s.indexOf("=")));
	    		    			t.setTag(s.substring(s.indexOf("=")+1));
	    		    		}else{
	    		    			if(s.indexOf(":")>-1){
		    		    			t.setName(s.substring(0,s.indexOf(":")));
		    		    			t.setTag(s.substring(s.indexOf(":")+1));
		    		    		}else{
		    		    			t.setTag(s);
		    		    		}
	    		    		}
	    		    		resource.setTags_tag(t);
	    				}
	    			}
	    			
	    		}
	    	}
	    }
		XnatExperimentdata assessed=null;
		if(this.assesseds.size()==1)assessed=assesseds.get(0);
        
		if(recons.size()>0){
			//reconstruction			
			if(assessed==null){
				this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Invalid session id.");
				//MATCHED
				return false;
			}
			
			XnatReconstructedimagedata recon=recons.get(0);
			
			uploadID=recon.getId();
			
			XnatImagesessiondata session = (XnatImagesessiondata)assessed;
			String dest_path = FileUtils.AppendRootPath(session.getCurrentSessionFolder(true), "PROCESSED/" + uploadID +"/");

			File saveTo=null;
			if(resourceFolder==null){
				saveTo = new File(new File(dest_path),relativePath);
			}else{
				saveTo = new File(new File(dest_path,resourceFolder),relativePath);
			}
			
			saveTo.getParentFile().mkdirs();
			
			fi.write(saveTo);
			
			resource.setUri(saveTo.getAbsolutePath());
			
			if(type!=null){
				if(type.equals("in")){
					recon.setIn_file(resource);
				}else{
					recon.setOut_file(resource);
				}
			}else{
				recon.setOut_file(resource);
			}
			
			recon.save(user, false, false);
			return true;
		}else if(scans.size()>0){
			//scan
			if(assessed==null){
				this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Invalid session id.");
				//MATCHED
				return false;
			}
			
			XnatImagescandata scan=scans.get(0);
			
			if(scan.getId()!=null && !scan.getId().equals("")){
				uploadID=scan.getId();
			}
			
			XnatImagesessiondata session = (XnatImagesessiondata)assessed;
			String dest_path = FileUtils.AppendRootPath(session.getCurrentSessionFolder(true), "SCANS/" + uploadID +"/");

			File saveTo=null;
			if(resourceFolder==null){
				saveTo = new File(new File(dest_path),relativePath);
			}else{
				saveTo = new File(new File(dest_path,resourceFolder),relativePath);
			}
			saveTo.getParentFile().mkdirs();
			
			fi.write(saveTo);
			
			resource.setUri(saveTo.getAbsolutePath());
			
			if(scan.getFile().size()==0){
				if(resource.getContent()==null && scan.getType()!=null){
					resource.setContent("RAW");
				}
			}
			
			scan.setFile(resource);
			
			scan.save(user, false, false);
			return true;
		}else if(expts.size()>0){
			XnatExperimentdata expt=this.expts.get(0);
//			experiment
			XnatExperimentdata session=null;
			
			String dest_path=null;
			if(expt.getItem().instanceOf("xnat:imageAssessorData")){
				session = (XnatImagesessiondata)assessed;
				if(expt.getId()!=null && !expt.getId().equals("")){
					uploadID=expt.getId();
				}
				dest_path = FileUtils.AppendRootPath(((XnatImagesessiondata)session).getCurrentSessionFolder(true), "ASSESSORS/" + uploadID +"/");
			}else{
				if(!expt.getItem().instanceOf("xnat:imageSessionData")){
					session = (XnatExperimentdata)expt;
					dest_path = FileUtils.AppendRootPath(proj.getRootArchivePath(), expt.getId() + "/RESOURCES/" + uploadID +"/");
				}else{
					session = (XnatImagesessiondata)expt;
					dest_path = FileUtils.AppendRootPath(((XnatImagesessiondata)session).getCurrentSessionFolder(true), "RESOURCES/" + uploadID +"/");
				}
			}

			File saveTo=null;
			if(resourceFolder==null){
				saveTo = new File(new File(dest_path),relativePath);
			}else{
				saveTo = new File(new File(dest_path,resourceFolder),relativePath);
			}
			saveTo.getParentFile().mkdirs();
			
			fi.write(saveTo);
			
			resource.setUri(saveTo.getAbsolutePath());
			

			if(expt.getItem().instanceOf("xnat:imageAssessorData")){
				XnatImageassessordata iad = (XnatImageassessordata)expt;
				if(type!=null){
					if(type.equals("in")){
						iad.setIn_file(resource);
					}else{
						iad.setOut_file(resource);
					}
				}else{
					iad.setOut_file(resource);
				}
				
				iad.save(user, false, false);
				
			}else{
				session.setResources_resource(resource);
				
				session.save(user, false, false);
			}
			return true;
		}else if(sub!=null){
			String dest_path=null;
			dest_path = FileUtils.AppendRootPath(proj.getRootArchivePath(), "subjects/" + sub.getArchiveDirectoryName() +"/");


			File saveTo=null;
			if(resourceFolder==null){
				saveTo = new File(new File(dest_path),relativePath);
			}else{
				saveTo = new File(new File(dest_path,resourceFolder),relativePath);
			}
			saveTo.getParentFile().mkdirs();
			
			fi.write(saveTo);
			
			resource.setUri(saveTo.getAbsolutePath());
			
			sub.setResources_resource(resource);
			
			sub.save(user, false, false);
			return true;
		}else if(proj!=null){
			String dest_path=null;
			dest_path = FileUtils.AppendRootPath(proj.getRootArchivePath(), "resources/");


			File saveTo=null;
			if(resourceFolder==null){
				saveTo = new File(new File(dest_path),relativePath);
			}else{
				saveTo = new File(new File(dest_path,resourceFolder),relativePath);
			}
			saveTo.getParentFile().mkdirs();
			
			fi.write(saveTo);
			
			resource.setUri(saveTo.getAbsolutePath());
			proj.setResources_resource(resource);
			
			proj.save(user, false, false);
			return true;
		}
		return false;
	}
	
	public boolean storeCatalogEntry(FileWriterWrapper fi,String dest, XnatAbstractresource abst) throws IOException,Exception{
			XnatResourcecatalog catResource=(XnatResourcecatalog)abst;
			File catFile = catResource.getCatalogFile(proj.getRootArchivePath());						
			String parentPath=catFile.getParent();	
			CatCatalogBean cat=catResource.getCleanCatalog(proj.getRootArchivePath(), false);
			
			
			String filename = fi.getName();

            int index = filename.lastIndexOf('\\');
            if (index< filename.lastIndexOf('/'))index = filename.lastIndexOf('/');
            if(index>0)filename = filename.substring(index+1);
            
            String compression_method = ".zip";
            if (filename.indexOf(".")!=-1){
                compression_method = filename.substring(filename.lastIndexOf("."));
            } 
            
            if (this.isQueryVariableTrue("extract") && (compression_method.equalsIgnoreCase(".tar") || 
                    compression_method.equalsIgnoreCase(".gz") || 
                    compression_method.equalsIgnoreCase(".zip") || 
                    compression_method.equalsIgnoreCase(".zar")))
            {   
            	File destinationDir=catFile.getParentFile();
                InputStream is = fi.getInputStream();

                ZipI zipper = null;
                if (compression_method.equalsIgnoreCase(".tar")){
                    zipper = new TarUtils();
                }else if (compression_method.equalsIgnoreCase(".gz")){
                    zipper = new TarUtils();
                    zipper.setCompressionMethod(ZipOutputStream.DEFLATED);
                }else{
                    zipper = new ZipUtils();
                }
                
                ArrayList<File> files =zipper.extract(is,destinationDir.getAbsolutePath());
                
                for(File f: files){
                	if(!f.isDirectory()){
                    	String relative = destinationDir.toURI().relativize(f.toURI()).getPath();
                    	
                    	CatEntryI e= this.getEntryByURI(cat, relative);
        				
        				if(e==null){
        				    CatEntryBean newEntry = new CatEntryBean();
        				    newEntry.setUri(relative);
        				    newEntry.setName(f.getName());
        				    
        				    if(this.getQueryVariable("description")!=null){
        				    	newEntry.setDescription(this.getQueryVariable("description"));
        				    }
        				    if(this.getQueryVariable("format")!=null){
        				    	newEntry.setFormat(this.getQueryVariable("format"));
        				    }
        				    if(this.getQueryVariable("content")!=null){
        				    	newEntry.setContent(this.getQueryVariable("content"));
        				    }
        				    if(this.getQueryVariables("tags")!=null){
        				    	String[] tags = this.getQueryVariables("tags");
        				    	for(String tag: tags){
        				    		tag = tag.trim();
        				    		for(String s : StringUtils.CommaDelimitedStringToArrayList(tag)){
        				    			s=s.trim();
        					    		if(!s.equals("")){
        						    		if(s.indexOf("=")>-1){
        						    			CatEntryMetafieldBean meta = new CatEntryMetafieldBean();
        						    			meta.setName(s.substring(0,s.indexOf("=")));
        						    			meta.setMetafield(s.substring(s.indexOf("=")+1));
        						    			newEntry.addMetafields_metafield(meta);
        						    		}else{
        						    			if(s.indexOf(":")>-1){
        							    			CatEntryMetafieldBean meta = new CatEntryMetafieldBean();
        							    			meta.setName(s.substring(0,s.indexOf(":")));
        							    			meta.setMetafield(s.substring(s.indexOf(":")+1));
        							    			newEntry.addMetafields_metafield(meta);
        				    		    		}else{
        							    			CatEntryTagBean t = new CatEntryTagBean();
        							    			t.setTag(s);
        							    			newEntry.addTags_tag(t);
        				    		    		}
        						    		}
        					    		}
        				    		}
        				    	}
        				    }
        				    
        				    
        				    cat.addEntries_entry(newEntry);
        				}
                	}
                }
			}else{
				File saveTo = new File(parentPath,dest);
				
				saveTo.getParentFile().mkdirs();
				fi.write(saveTo);
				
				CatEntryI e= this.getEntryByURI(cat, dest);
				
				if(e==null){
				    CatEntryBean newEntry = new CatEntryBean();
				    newEntry.setUri(dest);
				    newEntry.setName(saveTo.getName());
				    
				    if(this.getQueryVariable("description")!=null){
				    	newEntry.setDescription(this.getQueryVariable("description"));
				    }
				    if(this.getQueryVariable("format")!=null){
				    	newEntry.setFormat(this.getQueryVariable("format"));
				    }
				    if(this.getQueryVariable("content")!=null){
				    	newEntry.setContent(this.getQueryVariable("content"));
				    }
				    if(this.getQueryVariables("tags")!=null){
				    	String[] tags = this.getQueryVariables("tags");
				    	for(String tag: tags){
				    		tag = tag.trim();
				    		for(String s : StringUtils.CommaDelimitedStringToArrayList(tag)){
				    			s=s.trim();
					    		if(!s.equals("")){
						    		if(s.indexOf("=")>-1){
						    			CatEntryMetafieldBean meta = new CatEntryMetafieldBean();
						    			meta.setName(s.substring(0,s.indexOf("=")));
						    			meta.setMetafield(s.substring(s.indexOf("=")+1));
						    			newEntry.addMetafields_metafield(meta);
						    		}else{
						    			if(s.indexOf(":")>-1){
							    			CatEntryMetafieldBean meta = new CatEntryMetafieldBean();
							    			meta.setName(s.substring(0,s.indexOf(":")));
							    			meta.setMetafield(s.substring(s.indexOf(":")+1));
							    			newEntry.addMetafields_metafield(meta);
				    		    		}else{
							    			CatEntryTagBean t = new CatEntryTagBean();
							    			t.setTag(s);
							    			newEntry.addTags_tag(t);
				    		    		}
						    		}
					    		}
				    		}
				    	}
				    }
				    
				    
				    cat.addEntries_entry(newEntry);
				}
			}
			
            FileOutputStream fos=new FileOutputStream(catFile);
            OutputStreamWriter fw;
			try {
				FileLock fl=fos.getChannel().lock();
				try{
					fw = new OutputStreamWriter(fos);
					cat.toXML(fw, true);
					fw.flush();
				}finally{
					fl.release();
				}
			}finally{
				fos.close();
			}
			
			return true;
	}
	
	public static class FileWriterWrapper{
		public FileItem fi=null;
		public Representation entry=null;
		public String name=null;
		
		public FileWriterWrapper(FileItem f,String n){
			fi=f;
			name=n;
		}

		public FileWriterWrapper(Representation f,String n){
			entry=f;
			name=n;
		}
		
		public void write(File f) throws IOException,Exception{
			if(fi!=null){
				fi.write(f);
			}else{
				FileOutputStream fw = new FileOutputStream(f);
				if(entry.getSize()>2000000){
					org.apache.commons.io.IOUtils.copyLarge(entry.getStream(), fw);
				}else{
					org.apache.commons.io.IOUtils.copy(entry.getStream(), fw);
				}
				fw.close();
			}
		}
		
		public String getName(){
			return name;
		}
		

		
		public InputStream getInputStream() throws IOException,Exception{
			if(fi!=null){
				return fi.getInputStream();
			}else{
				return entry.getStream();
			}
		}
		
		public void delete(){
			if(fi!=null){
				fi.delete();
			}
		}
	}
}
