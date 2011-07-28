/**
 * Copyright 2010 Washington University
 */
package org.nrg.xnat.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;
import org.nrg.xdat.bean.CatCatalogBean;
import org.nrg.xdat.bean.CatCatalogMetafieldBean;
import org.nrg.xdat.bean.CatEntryBean;
import org.nrg.xdat.bean.CatEntryMetafieldBean;
import org.nrg.xdat.bean.CatEntryTagBean;
import org.nrg.xdat.bean.base.BaseElement;
import org.nrg.xdat.bean.reader.XDATXMLReader;
import org.nrg.xdat.model.CatCatalogI;
import org.nrg.xdat.model.CatDcmentryI;
import org.nrg.xdat.model.CatEntryI;
import org.nrg.xdat.model.CatEntryMetafieldI;
import org.nrg.xdat.model.CatEntryTagI;
import org.nrg.xdat.model.XnatResourcecatalogI;
import org.nrg.xdat.om.XnatAbstractresourceTag;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xft.utils.zip.TarUtils;
import org.nrg.xft.utils.zip.ZipI;
import org.nrg.xft.utils.zip.ZipUtils;
import org.nrg.xnat.helpers.resource.XnatResourceInfo;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.xml.sax.SAXException;

/**
 * @author timo
 *
 */
@SuppressWarnings("deprecation")
public class CatalogUtils {
    static Logger logger = Logger.getLogger(CatalogUtils.class);
	
	public static List<Object[]> getEntryDetails(CatCatalogI cat, String parentPath,String uriPath,XnatResource _resource, String coll_tags,boolean includeFile, final CatEntryFilterI filter){
		final ArrayList<Object[]> al = new ArrayList<Object[]>();
		for(final CatCatalogI subset:cat.getSets_entryset()){
			al.addAll(getEntryDetails(subset,parentPath,uriPath,_resource,coll_tags,includeFile,filter));
		}
		
		final int ri=(includeFile)?9:8;
		for(final CatEntryI entry:cat.getEntries_entry()){
			if(filter==null || filter.accept(entry)){
				final Object[] row = new Object[ri];
				final String entryPath = StringUtils.ReplaceStr(FileUtils.AppendRootPath(parentPath,entry.getUri()),"\\","/");
	            final File f=getFileOnLocalFileSystem(entryPath);
	            row[0]=(f.getName());
	            if(includeFile){
	            	row[1]=0;
	            }else{
	            	row[1]=(f.length());
	            }
	            if(FileUtils.IsAbsolutePath(entry.getUri())){
	                row[2]=uriPath+"/" + entry.getId();
	            }else{
	                row[2]=uriPath+"/" + entry.getUri();
	            }
	            row[3]=_resource.getLabel();
	            
	            row[4]="";
	            for(CatEntryMetafieldI meta: entry.getMetafields_metafield()){
	            	if(!row[4].equals(""))row[4]=row[4] +",";
	            	row[4]=row[4]+meta.getName() + "=" + meta.getMetafield();
	            }
	            for(CatEntryTagI tag: entry.getTags_tag()){
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
	
	public interface CatCatalogFilterI{
		public boolean accept(final CatCatalogI cat);
	}
	
	public interface CatEntryFilterI{
		public boolean accept(final CatEntryI entry);
	}
	
	public static CatEntryI getEntryByFilter(final CatCatalogI cat, final CatEntryFilterI filter){
		CatEntryI e=null;
		for(CatCatalogI subset:cat.getSets_entryset()){
			e = getEntryByFilter(subset,filter);
			if(e!=null) return e;
		}
		
		for(CatEntryI entry:cat.getEntries_entry()){
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
	
	public static Collection<CatEntryI> getEntriesByFilter(final CatCatalogI cat, final CatEntryFilterI filter){
		List<CatEntryI> entries=new ArrayList<CatEntryI>();
	
		for(CatCatalogI subset:cat.getSets_entryset()){
			entries.addAll(getEntriesByFilter(subset,filter));
		}
		
		for(CatEntryI entry:cat.getEntries_entry()){
			try {
				if(filter==null || filter.accept(entry)){
    				entries.add(entry);
    			}
			} catch (Exception e1) {
				logger.error(e1);
			}
		}
		
		return entries;
	}

	public static CatCatalogI getCatalogByFilter(final CatCatalogI cat, final CatCatalogFilterI filter){
		CatCatalogI e=null;
		for(CatCatalogI subset:cat.getSets_entryset()){
			e = getCatalogByFilter(subset,filter);
			if(e!=null) return e;
		}
		
		return null;
	}

	public static List<File> getFiles(CatCatalogI cat,String parentPath){
		List<File> al = new ArrayList<File>();
		for(CatCatalogI subset:cat.getSets_entryset()){
			al.addAll(getFiles(subset,parentPath));
		}
		
		for(CatEntryI entry:cat.getEntries_entry()){
			String entryPath = StringUtils.ReplaceStr(FileUtils.AppendRootPath(parentPath,entry.getUri()),"\\","/");
	        File f=getFileOnLocalFileSystem(entryPath);
	        
			if(f!=null)
				al.add(f);
		}
		
		return al;
	}

	public static File getFile(CatEntryI entry,String parentPath){
		String entryPath = StringUtils.ReplaceStr(FileUtils.AppendRootPath(parentPath,entry.getUri()),"\\","/");
	    return getFileOnLocalFileSystem(entryPath);
	}
	
	public static Stats getFileStats(CatCatalogI cat,String parentPath){
		return new Stats(cat,parentPath);
	}
	
	public static class Stats{
		public int count;
		public long size;
		
		public Stats(CatCatalogI cat, String parentPath){
			count = 0;
			size = 0;
            Iterator<File> files = getFiles(cat,parentPath).iterator();
            while (files.hasNext()){
                File f = files.next();
               
                if (f!=null && f.exists() && !f.getName().endsWith("catalog.xml")){
                    count++;
                    size+=f.length();
                }
            }
		}
	}

	public static CatEntryI getEntryByURI(CatCatalogI cat, String name){
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

	public static CatEntryI getEntryByName(CatCatalogI cat, String name){
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

	public static CatEntryI getEntryById(CatCatalogI cat, String name){
		CatEntryI e=null;
		for(CatCatalogI subset:cat.getSets_entryset()){
			e = getEntryById(subset,name);
			if(e!=null) return e;
		}
		
		for(CatEntryI entry:cat.getEntries_entry()){
			if(entry.getId().equals(name)){
				return entry;
			}
		}
		
		return null;
	}

	public static CatDcmentryI getDCMEntryByUID(CatCatalogI cat, String uid){
		CatDcmentryI e=null;
		for(CatCatalogI subset:cat.getSets_entryset()){
			e = getDCMEntryByUID(subset,uid);
			if(e!=null) return e;
		}
		
		for(CatEntryI entry:cat.getEntries_entry()){
			if(entry instanceof CatDcmentryI && ((CatDcmentryI)entry).getUid().equals(uid)){
				return (CatDcmentryI)entry;
			}
		}
		
		return null;
	}

	public static CatDcmentryI getDCMEntryByInstanceNumber(CatCatalogI cat, Integer num){
		CatDcmentryI e=null;
		for(CatCatalogI subset:cat.getSets_entryset()){
			e = getDCMEntryByInstanceNumber(subset,num);
			if(e!=null) return e;
		}
		
		for(CatEntryI entry:cat.getEntries_entry()){
			if(entry instanceof CatDcmentryI && ((CatDcmentryI)entry).getInstancenumber().equals(num)){
				return (CatDcmentryI)entry;
			}
		}
		
		return null;
	}

	protected static File getFileOnLocalFileSystem(String fullPath) {
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

	public static void configureEntry(final CatEntryBean newEntry,final XnatResourceInfo info){
		if (info.getDescription() != null) {
			newEntry.setDescription(info.getDescription());
		}
		if (info.getFormat() != null) {
			newEntry.setFormat(info.getFormat());
		}
		if (info.getContent() != null) {
			newEntry.setContent(info.getContent());
		}
		if (info.getTags().size() > 0) {
			for (final String entry : info.getTags()) {
				final CatEntryTagBean t = new CatEntryTagBean();
				t.setTag(entry);
				newEntry.addTags_tag(t);
			}
		}
		if (info.getMeta().size() > 0) {
			for (final Map.Entry<String, String> entry : info.getMeta().entrySet()) {
				final CatEntryMetafieldBean meta = new CatEntryMetafieldBean();
				meta.setName(entry.getKey());
				meta.setMetafield(entry.getValue());
				newEntry.addMetafields_metafield(meta);
			}
		}
	}

	public static void configureEntry(final XnatResource newEntry,final XnatResourceInfo info, final XDATUser user) throws Exception{
		if (info.getDescription() != null) {
			newEntry.setDescription(info.getDescription());
		}
		if (info.getFormat() != null) {
			newEntry.setFormat(info.getFormat());
		}
		if (info.getContent() != null) {
			newEntry.setContent(info.getContent());
		}
		if (info.getTags().size() > 0) {
			for (final String entry : info.getTags()) {
				final XnatAbstractresourceTag t = new XnatAbstractresourceTag((UserI)user);
				t.setTag(entry);
	    		newEntry.setTags_tag(t);
			}
		}
		if (info.getMeta().size() > 0) {
			for (final Map.Entry<String, String> entry : info.getMeta().entrySet()) {
				final XnatAbstractresourceTag t = new XnatAbstractresourceTag((UserI)user);
				t.setTag(entry.getValue());
				t.setName(entry.getKey());
	    		newEntry.setTags_tag(t);
			}
		}
	}

	public final static String[] FILE_HEADERS = {"Name","Size","URI","collection","file_tags","file_format","file_content","cat_ID"};
	public final static String[] FILE_HEADERS_W_FILE = {"Name","Size","URI","collection","file_tags","file_format","file_content","cat_ID","file"};

	public static boolean storeCatalogEntry(final FileWriterWrapperI fi, String dest, final XnatResourcecatalog catResource, final XnatProjectdata proj, final boolean extract, final XnatResourceInfo info,final boolean overwrite) throws IOException, Exception {
		final File catFile = catResource.getCatalogFile(proj.getRootArchivePath());
		final String parentPath = catFile.getParent();
		final CatCatalogBean cat = catResource.getCleanCatalog(proj.getRootArchivePath(), false);
	
		String filename = fi.getName();
	
		int index = filename.lastIndexOf('\\');
		if (index < filename.lastIndexOf('/')) {
			index = filename.lastIndexOf('/');
		}
		
		if (index > 0) {
			filename = filename.substring(index + 1);
		}
		
		if(dest==null){
			dest=filename;
		}
	
		String compression_method = ".zip";
		if (filename.indexOf(".") != -1) {
			compression_method = filename.substring(filename.lastIndexOf("."));
		}
	
		if (extract && (compression_method.equalsIgnoreCase(".tar") || compression_method.equalsIgnoreCase(".gz") || compression_method.equalsIgnoreCase(".zip") || compression_method.equalsIgnoreCase(".zar"))) {
			File destinationDir = catFile.getParentFile();
			if(dest!=null){
				destinationDir=new File(destinationDir,dest);
			}
			final InputStream is = fi.getInputStream();
	
			ZipI zipper = null;
			if (compression_method.equalsIgnoreCase(".tar")) {
				zipper = new TarUtils();
			} else if (compression_method.equalsIgnoreCase(".gz")) {
				zipper = new TarUtils();
				zipper.setCompressionMethod(ZipOutputStream.DEFLATED);
			} else {
				zipper = new ZipUtils();
			}
	
			@SuppressWarnings("unchecked")
			final List<File> files = zipper.extract(is, destinationDir.getAbsolutePath(),overwrite);
	
			for (final File f : files) {
				if (!f.isDirectory()) {
					final String relative = destinationDir.toURI().relativize(f.toURI()).getPath();
	
					final CatEntryI e = getEntryByURI(cat, relative);
	
					if (e == null) {
						final CatEntryBean newEntry = new CatEntryBean();
						newEntry.setUri(relative);
						newEntry.setName(f.getName());
	
						configureEntry(newEntry, info);
	
						cat.addEntries_entry(newEntry);
					}
				}
			}
		} else {
			final File saveTo = new File(parentPath, (dest!=null)?dest:filename);

            if(saveTo.exists() && !overwrite){
            	throw new IOException("File already exists"+saveTo.getCanonicalPath());
            }
            
			saveTo.getParentFile().mkdirs();
			fi.write(saveTo);
	
			if(saveTo.isDirectory()){
				final Iterator<File> iter=org.apache.commons.io.FileUtils.iterateFiles(saveTo,null,true);
				while(iter.hasNext()){
					final File movedF=iter.next();
					
					String relativePath=FileUtils.RelativizePath(saveTo, movedF).replace('\\', '/');
					if(dest!=null){
						relativePath=dest+"/"+relativePath;
					}
					
					final CatEntryI e = getEntryByURI(cat, relativePath);
					
					if (e == null) {
						final CatEntryBean newEntry = new CatEntryBean();
						newEntry.setUri(relativePath);
						newEntry.setName(movedF.getName());
						
						configureEntry(newEntry, info);
			
						cat.addEntries_entry(newEntry);
					}
				}
				
			}else{
				final CatEntryI e = getEntryByURI(cat, dest);
				
				if (e == null) {
					final CatEntryBean newEntry = new CatEntryBean();
					newEntry.setUri(dest);
					newEntry.setName(saveTo.getName());
					
					configureEntry(newEntry, info);
		
					cat.addEntries_entry(newEntry);
				}
			}
		}
	
		writeCatalogToFile(cat,catFile);
	
		return true;
	}
	
	public static void writeCatalogToFile(CatCatalogI xml, File dest) throws Exception{
		final FileOutputStream fos = new FileOutputStream(dest);
		OutputStreamWriter fw;
		try {
			final FileLock fl = fos.getChannel().lock();
			try {
				fw = new OutputStreamWriter(fos);
				xml.toXML(fw);
				fw.flush();
			} finally {
				fl.release();
			}
		} finally {
			fos.close();
		}
	}

	
	public static File getCatalogFile(final String rootPath,final XnatResourcecatalogI resource){
        String fullPath = getFullPath(rootPath,resource);
        if (fullPath.endsWith("\\")) {
            fullPath = fullPath.substring(0,fullPath.length() -1);
        }
        if (fullPath.endsWith("/")) {
            fullPath = fullPath.substring(0,fullPath.length() -1);
        }



        File f = new File(fullPath);
        if (!f.exists())
        {
            f = new File(fullPath + ".gz");
        }
        
        if(!f.exists()){
        	f=new File(fullPath);
        	
        	CatCatalogBean cat = new CatCatalogBean();
			if(resource.getLabel()!=null){
				cat.setId(resource.getLabel());
			}else{
				cat.setId("" + Calendar.getInstance().getTimeInMillis());
			}
			
			f.getParentFile().mkdirs();
			
			try {
				FileWriter fw = new FileWriter(f);
				cat.toXML(fw, true);
				fw.close();
			} catch (IOException e) {
				logger.error("",e);
			}
        }

        return f;
    }
    
    public static CatCatalogBean getCleanCatalog(String rootPath,XnatResourcecatalogI resource,boolean includeFullPaths){
    	try {
			File catF=CatalogUtils.getCatalogFile(rootPath,resource);
			if(catF.getName().endsWith(".gz")){
				try {
					FileUtils.GUnzipFiles(catF);
					catF=CatalogUtils.getCatalogFile(rootPath,resource);
				} catch (FileNotFoundException e) {
			        logger.error("",e);
				} catch (IOException e) {
			        logger.error("",e);
				}
			}
			
			InputStream fis = new FileInputStream(catF);
			if (catF.getName().endsWith(".gz"))
			{
			    fis = new GZIPInputStream(fis);
			}
			
			BaseElement base=null;
			
			XDATXMLReader reader = new XDATXMLReader();
			base = reader.parse(fis);
				
			String parentPath = catF.getParent();

			if (base instanceof CatCatalogBean){
                CatCatalogBean cat = (CatCatalogBean)base;
                formalizeCatalog(cat, parentPath);
                                
                if(includeFullPaths){
                    CatCatalogMetafieldBean mf = new CatCatalogMetafieldBean();
                    mf.setName("CATALOG_LOCATION");
                    mf.setMetafield(parentPath);
                    cat.addMetafields_metafield(mf);
                }
                
			    return cat;
			}
		} catch (FileNotFoundException e) {
	        logger.error("",e);
		} catch (IOException e) {
	        logger.error("",e);
		} catch (SAXException e) {
	        logger.error("",e);
		}
    	
    	return null;
    }
    
    private static boolean formalizeCatalog(final CatCatalogI cat, final String catPath){
    	return formalizeCatalog(cat,catPath,cat.getId());
    }
    
    private static boolean formalizeCatalog(final CatCatalogI cat, final String catPath, String header){
    	boolean modified=false;
    	
    	for(CatCatalogI subSet:cat.getSets_entryset()){
    		if(formalizeCatalog(subSet,catPath,header + "/" + subSet.getId())){
    			modified=true;
    		}
    	}
    	for(CatEntryI entry: cat.getEntries_entry()){
	    	if(entry.getId()==null || !entry.getId().equals("")){
		        String entryPath = StringUtils.ReplaceStr(FileUtils.AppendRootPath(catPath,entry.getUri()),"\\","/");
		        File f =getFileOnLocalFileSystem(entryPath);
		        if(f!=null){
			        ((CatEntryBean)entry).setId(header + "/" + f.getName());
			        modified=true;
		        }else{
		        	logger.error("Missing Resource:" + entryPath);
		        }
	    	}
	    }
	    
	    return modified;
    }

    public static String getFullPath(String rootPath,XnatResourcecatalogI resource){

        String fullPath = StringUtils.ReplaceStr(FileUtils.AppendRootPath(rootPath,resource.getUri()),"\\","/");
        while (fullPath.indexOf("//")!=-1)
        {
            fullPath =StringUtils.ReplaceStr(fullPath,"//","/");
        }

        if(!fullPath.endsWith("/"))
        {
            fullPath+="/";
        }

        return fullPath;
    }
    
	
}
