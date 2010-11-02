/**
 * Copyright 2010 Washington University
 */
package org.nrg.xnat.utils;

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.nrg.xdat.bean.CatEntryBean;
import org.nrg.xdat.bean.CatEntryMetafieldBean;
import org.nrg.xdat.bean.CatEntryTagBean;
import org.nrg.xdat.model.CatCatalogI;
import org.nrg.xdat.model.CatEntryI;
import org.nrg.xdat.model.CatEntryMetafieldI;
import org.nrg.xdat.model.CatEntryTagI;
import org.nrg.xdat.om.XnatAbstractresourceTag;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xnat.helpers.resource.XnatResourceInfo;

/**
 * @author timo
 *
 */
public class CatalogUtils {
    static Logger logger = Logger.getLogger(CatalogUtils.class);
	
	public static List<Object[]> getEntryDetails(CatCatalogI cat, String parentPath,String uriPath,XnatResource _resource, String coll_tags,boolean includeFile, final CatEntryFilterI filter){
		final ArrayList al = new ArrayList();
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
	            row[1]=(f.length());
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

	public static ArrayList<File> getFiles(CatCatalogI cat,String parentPath){
		ArrayList<File> al = new ArrayList<File>();
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

}
