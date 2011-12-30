/**
 * Copyright 2010 Washington University
 */
package org.nrg.xnat.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
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
import org.nrg.xdat.model.CatCatalogMetafieldI;
import org.nrg.xdat.model.CatDcmentryI;
import org.nrg.xdat.model.CatEntryI;
import org.nrg.xdat.model.CatEntryMetafieldI;
import org.nrg.xdat.model.CatEntryTagI;
import org.nrg.xdat.model.XnatResourcecatalogI;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatAbstractresourceTag;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xft.utils.zip.TarUtils;
import org.nrg.xft.utils.zip.ZipI;
import org.nrg.xft.utils.zip.ZipUtils;
import org.nrg.xnat.helpers.resource.XnatResourceInfo;
import org.nrg.xnat.presentation.ChangeSummaryBuilderA;
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

	public static File getFileOnLocalFileSystem(String fullPath) {
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

	public static void configureEntry(final CatEntryBean newEntry,final XnatResourceInfo info, boolean modified){
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
		
		if(modified){
			if(info.getUser()!=null && newEntry.getModifiedby()==null){
				newEntry.setModifiedby(info.getUser().getUsername());
			}
			if(info.getLastModified()!=null){
				newEntry.setModifiedtime(info.getLastModified());
			}
			if(info.getEvent_id()!=null && newEntry.getModifiedeventid()==null){
				newEntry.setModifiedeventid(info.getEvent_id().toString());
			}
		}else{
			if(info.getUser()!=null && newEntry.getCreatedby()==null){
				newEntry.setCreatedby(info.getUser().getUsername());
			}
			if(info.getCreated()!=null && newEntry.getCreatedtime()==null){
				newEntry.setCreatedtime(info.getCreated());
			}
			if(info.getEvent_id()!=null && newEntry.getCreatedeventid()==null){
				newEntry.setCreatedeventid(info.getEvent_id().toString());
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

	public static boolean storeCatalogEntry(final FileWriterWrapperI fi, String dest, final XnatResourcecatalog catResource, final XnatProjectdata proj, final boolean extract, final XnatResourceInfo info,final boolean overwrite, final EventMetaI ci) throws IOException, Exception {
		final File catFile = catResource.getCatalogFile(proj.getRootArchivePath());
		final String parentPath = catFile.getParent();
		final CatCatalogBean cat = catResource.getCleanCatalog(proj.getRootArchivePath(), false,null,null);
	
		String filename = fi.getName();
	
		int index = filename.lastIndexOf('\\');
		if (index < filename.lastIndexOf('/')) {
			index = filename.lastIndexOf('/');
		}
		
		if (index > 0) {
			filename = filename.substring(index + 1);
		}
		
		if(StringUtils.IsEmpty(dest)){
			dest=filename;
		}else if(dest.startsWith("/")){
			dest=dest.substring(1);
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
			final List<File> files = zipper.extract(is, destinationDir.getAbsolutePath(),overwrite,ci);
	
			for (final File f : files) {
				if (!f.isDirectory()) {
					final String relative = destinationDir.toURI().relativize(f.toURI()).getPath();
	
					final CatEntryI e = getEntryByURI(cat, relative);
	
					if (e == null) {
						final CatEntryBean newEntry = new CatEntryBean();
						newEntry.setUri(relative);
						newEntry.setName(f.getName());
	
						configureEntry(newEntry, info,false);
	
						cat.addEntries_entry(newEntry);
					}
				}
			}
		} else {
			final File saveTo = new File(parentPath, (dest!=null)?dest:filename);

            if(saveTo.exists() && !overwrite){
            	throw new IOException("File already exists"+saveTo.getCanonicalPath());
            }else if(saveTo.exists()){
            	final CatEntryBean e = (CatEntryBean)getEntryByURI(cat, dest);
				
				CatalogUtils.moveToHistory(catFile,cat,saveTo,e,ci);
            }
            
			saveTo.getParentFile().mkdirs();
			fi.write(saveTo);
	
			if(saveTo.isDirectory()){
				@SuppressWarnings("unchecked")
				final Iterator<File> iter=org.apache.commons.io.FileUtils.iterateFiles(saveTo,null,true);
				while(iter.hasNext()){
					final File movedF=iter.next();
					
					String relativePath=FileUtils.RelativizePath(saveTo, movedF).replace('\\', '/');
					if(dest!=null){
						relativePath=dest+"/"+relativePath;
					}
					updateEntry(cat, relativePath, movedF, info, ci);
				}
				
			}else{
				updateEntry(cat, dest, saveTo, info, ci);
			}
		}
	
		writeCatalogToFile(cat,catFile);
	
		return true;
	}
	
	private static void updateEntry(CatCatalogBean cat, String dest, File f, XnatResourceInfo info, EventMetaI ci){
		final CatEntryBean e = (CatEntryBean)getEntryByURI(cat, dest);
		
		if (e == null) {
			final CatEntryBean newEntry = new CatEntryBean();
			newEntry.setUri(dest);
			newEntry.setName(f.getName());
			
			configureEntry(newEntry, info,false);

			cat.addEntries_entry(newEntry);
		}else{
			if(ci!=null){
				if(ci.getUser()!=null)
					e.setModifiedby(ci.getUser().getUsername());
				e.setModifiedtime(ci.getEventDate());
				if(ci.getEventId()!=null){
					e.setModifiedeventid(ci.getEventId().toString());
				}
			}
		}
	}
	
	public static void refreshAuditSummary(CatCatalogI cat){
		CatCatalogMetafieldI field=null;
		for(CatCatalogMetafieldI mf:cat.getMetafields_metafield()){
			if("AUDIT".equals(mf.getName())){
				field=mf;
				break;
			}
		}
		
		if(field==null){
			field=new CatCatalogMetafieldBean();
			field.setName("AUDIT");
			try {
				cat.addMetafields_metafield(field);
			} catch (Exception e) {
			}
		}
		
		
		field.setMetafield(convertAuditToString(buildAuditSummary(cat)));
	}
	
	private static String convertAuditToString(Map<String,Map<String,Integer>> summary){
		StringBuilder sb= new StringBuilder();
		int counter1=0;
		for(Map.Entry<String,Map<String,Integer>> entry: summary.entrySet()){
			if(counter1++>0)sb.append("|");
			sb.append(entry.getKey()).append("=");
			int counter2=0;
			for(Map.Entry<String,Integer> sub:entry.getValue().entrySet()){
				sb.append(sub.getKey()).append(":").append(sub.getValue());
				if(counter2++>0)sb.append(";");
			}
			
		}
		return sb.toString();
	}
	
	public static Map<String,Map<String,Integer>> retrieveAuditySummary(CatCatalogI cat){
		if(cat==null)return new HashMap<String,Map<String,Integer>>();
		CatCatalogMetafieldI field=null;
		for(CatCatalogMetafieldI mf:cat.getMetafields_metafield()){
			if("AUDIT".equals(mf.getName())){
				field=mf;
				break;
			}
		}
		
		if(field!=null){
			return convertAuditToMap(field.getMetafield());
		}else{
			return buildAuditSummary(cat);
		}
		
	}
	
	private static Map<String,Map<String,Integer>> convertAuditToMap(String audit){
		Map<String,Map<String,Integer>> summary = new HashMap<String,Map<String,Integer>>();
		for(String changeset:audit.split("|")){
			String[] split1=changeset.split("=");
			if(split1.length>1){
				String key=split1[0];
				Map<String,Integer> counts=new HashMap<String,Integer>();
				for(String operation:split1[1].split(";")){
					String[] entry=operation.split(":");
					counts.put(entry[0], Integer.valueOf(entry[1]));
				}
				summary.put(key,counts);
			}
		}
		return summary;
	}
	
	private static Map<String,Map<String,Integer>> buildAuditSummary(CatCatalogI cat){
		Map<String,Map<String,Integer>> summary = new HashMap<String,Map<String,Integer>>();
		buildAuditSummary(cat, summary);
		return summary;
	}
	
	private static void buildAuditSummary(CatCatalogI cat,Map<String,Map<String,Integer>> summary){
		for(CatCatalogI subSet:cat.getSets_entryset()){
			buildAuditSummary(cat, summary);
    	}
    	for(CatEntryI entry: cat.getEntries_entry()){
    		addAuditEntry(summary,entry.getCreatedeventid(),entry.getCreatedtime(),ChangeSummaryBuilderA.ADDED,1);
    		
    		if(entry.getModifiedtime()!=null){
        		addAuditEntry(summary,entry.getModifiedeventid(),entry.getModifiedtime(),ChangeSummaryBuilderA.REMOVED,1);
    		}
    	}
	}
	
	public static void addAuditEntry(Map<String,Map<String,Integer>> summary, String key, String action, Integer i){	
		if(!summary.containsKey(key)){
			summary.put(key, new HashMap<String,Integer>());
		}
		
		if(!summary.get(key).containsKey(action)){
			summary.get(key).put(action, Integer.valueOf(0));
		}
		
		summary.get(key).put(action, summary.get(key).get(action) + i);
	}
	
	public static void addAuditEntry(Map<String,Map<String,Integer>> summary, Integer eventid,Object d, String action, Integer i){	
		String key=eventid+":"+d;
		addAuditEntry(summary, key, action,i);
	}
	
	public static void writeCatalogToFile(CatCatalogI xml, File dest) throws Exception{
		if(!dest.getParentFile().exists()){
			dest.getParentFile().mkdirs();
		}
		
		refreshAuditSummary(xml);
		
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
    
    public static CatCatalogBean getCatalog(File catF){
    	if(!catF.exists())return null;
    	try {			
			InputStream fis = new FileInputStream(catF);
			if (catF.getName().endsWith(".gz"))
			{
			    fis = new GZIPInputStream(fis);
			}
			
			BaseElement base=null;
			
			XDATXMLReader reader = new XDATXMLReader();
			base = reader.parse(fis);

			if (base instanceof CatCatalogBean){
                return (CatCatalogBean)base;
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
    
    public static CatCatalogBean getCatalog(String rootPath,XnatResourcecatalogI resource){
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
		
		return getCatalog(catF);
    }
	
	public static CatCatalogBean getCleanCatalog(String rootPath,XnatResourcecatalogI resource,boolean includeFullPaths){
		return getCleanCatalog(rootPath, resource, includeFullPaths,null,null);
	}
    
    public static CatCatalogBean getCleanCatalog(String rootPath,XnatResourcecatalogI resource,boolean includeFullPaths, UserI user, EventMetaI c){
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
                formalizeCatalog(cat, parentPath, user,c);
                                
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
    
    public static boolean formalizeCatalog(final CatCatalogI cat, final String catPath, UserI user, EventMetaI now){
    	return formalizeCatalog(cat,catPath,cat.getId(),user,now);
    }
    
    private static boolean formalizeCatalog(final CatCatalogI cat, final String catPath, String header, UserI user, EventMetaI now){
    	boolean modified=false;
    	
    	for(CatCatalogI subSet:cat.getSets_entryset()){
    		if(formalizeCatalog(subSet,catPath,header + "/" + subSet.getId(),user,now)){
    			modified=true;
    		}
    	}
    	for(CatEntryI entry: cat.getEntries_entry()){
    		if(entry.getCreatedby()==null && user!=null){
    			entry.setCreatedby(user.getUsername());
    			modified=true;
    		}
    		if(entry.getCreatedtime()==null && now!=null){
    			((CatEntryBean)entry).setCreatedtime(now.getEventDate());
    			modified=true;
    		}
    		if(entry.getCreatedeventid()==null && now!=null && now.getEventId()!=null){
    			((CatEntryBean)entry).setCreatedeventid(now.getEventId().toString());
    			modified=true;
    		}
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
    
    public boolean modifyEntry(CatCatalogI cat, CatEntryI oldEntry, CatEntryI newEntry){
    	for(int i=0;i<cat.getEntries_entry().size();i++){
			CatEntryI e= cat.getEntries_entry().get(i);
			if(e.getUri().equals(oldEntry.getUri())){
				cat.getEntries_entry().remove(i);
				cat.getEntries_entry().add(newEntry);
				return true;
			}
		}
		
		for(CatCatalogI subset: cat.getSets_entryset()){
			if(modifyEntry(subset,oldEntry,newEntry)){
				return true;
			}
		}
		
		return false;
    }
	
	public static List<File> findHistoricalCatFiles(File catFile) {
		final List<File> files=new ArrayList<File>();
		
		final File historyDir=FileUtils.BuildHistoryParentFile(catFile);
		
		final String name=catFile.getName();
		
		final FilenameFilter filter=new FilenameFilter(){
			@Override
			public boolean accept(File arg0, String arg1) {
				return (arg1.equals(name));
			}};
		
		if(historyDir.exists()){
			for(File d:historyDir.listFiles()){
				if(d.isDirectory()){
					final File[] matched=d.listFiles(filter);
					if(matched!=null && matched.length>0){
						files.addAll(Arrays.asList(matched));
					}
				}
			}
		}
		
		return files;
	}

	public static boolean removeEntry(CatCatalogI cat,CatEntryI entry)
	{
		for(int i=0;i<cat.getEntries_entry().size();i++){
			CatEntryI e= cat.getEntries_entry().get(i);
			if(e.getUri().equals(entry.getUri())){
				cat.getEntries_entry().remove(i);
				return true;
			}
		}
		
		for(CatCatalogI subset: cat.getSets_entryset()){
			if(removeEntry(subset,entry)){
				return true;
			}
		}
		
		return false;
	}

	public static void moveToHistory(File catFile, CatCatalogBean cat, File f, CatEntryBean entry, EventMetaI ci) throws Exception {
		//move existing file to audit trail
		final File newFile=FileUtils.MoveToHistory(f,EventUtils.getTimestamp(ci));
		addCatHistoryEntry(catFile,cat,newFile.getAbsolutePath(),entry,ci);
	}

	public static void addCatHistoryEntry(File catFile, CatCatalogBean cat, String f, CatEntryBean entry, EventMetaI ci) throws Exception {
		//move existing file to audit trail
		CatEntryBean newEntryBean=(CatEntryBean)entry.copy();
		newEntryBean.setUri(f);
		if(ci!=null){
			newEntryBean.setModifiedtime(ci.getEventDate());
			if(ci.getEventId()!=null){
				newEntryBean.setModifiedeventid(ci.getEventId().toString());
			}
			if(ci.getUser()!=null){
				newEntryBean.setModifiedby(ci.getUser().getUsername());
			}
		}
		
		File newCatFile=FileUtils.BuildHistoryFile(catFile,EventUtils.getTimestamp(ci));
		CatCatalogBean newCat;
		if(newCatFile.exists()){
			newCat=CatalogUtils.getCatalog(newCatFile);
		}else{
			newCat=new CatCatalogBean();
		}
		
		newCat.addEntries_entry(newEntryBean);
		
		CatalogUtils.writeCatalogToFile(newCat, newCatFile);
	}
	
	public static boolean populateStats(XnatAbstractresource abst, String rootPath){
		Integer c=abst.getCount(rootPath);
		Long s=abst.getSize(rootPath);
		
		boolean modified=false;
		
		if(!c.equals(abst.getFileCount())){
			abst.setFileCount(c);
			modified=true;
		}
		
		if(!s.equals(abst.getFileSize())){
			abst.setFileSize(s);
			modified=true;
		}
		
		return modified;
	}
}
