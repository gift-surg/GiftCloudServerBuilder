// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Thu Mar 29 15:09:29 CDT 2007
 *
 */
package org.nrg.xdat.om.base;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import org.nrg.xdat.bean.CatCatalogBean;
import org.nrg.xdat.bean.CatCatalogMetafieldBean;
import org.nrg.xdat.bean.CatEntryBean;
import org.nrg.xdat.bean.base.BaseElement;
import org.nrg.xdat.bean.reader.XDATXMLReader;
import org.nrg.xdat.model.CatCatalogBeanI;
import org.nrg.xdat.model.CatCatalogI;
import org.nrg.xdat.model.CatEntryBeanI;
import org.nrg.xdat.model.CatEntryI;
import org.nrg.xdat.om.base.auto.AutoXnatResourcecatalog;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xft.utils.StringUtils;
import org.xml.sax.SAXException;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatResourcecatalog extends AutoXnatResourcecatalog {

	public BaseXnatResourcecatalog(ItemI item)
	{
		super(item);
	}

	public BaseXnatResourcecatalog(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatResourcecatalog(UserI user)
	 **/
	public BaseXnatResourcecatalog()
	{}

	public BaseXnatResourcecatalog(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

    /**
     * Returns ArrayList of java.io.File objects
     * @return
     */
    public ArrayList getCorrespondingFiles(String rootPath)
    {
        if (files==null)
        {
            String fullPath = getFullPath(rootPath);
            if (fullPath.endsWith("\\")) {
                fullPath = fullPath.substring(0,fullPath.length() -1);
            }
            if (fullPath.endsWith("/")) {
                fullPath = fullPath.substring(0,fullPath.length() -1);
            }

            files = new ArrayList();

            File f =  new File(fullPath);
            if (!f.exists())
            {
                f = new File(fullPath + ".gz");
            }



            if (f.exists()){


                try {
                    InputStream fis = new FileInputStream(f);
                    if (f.getName().endsWith(".gz"))
                    {
                        fis = new GZIPInputStream(fis);
                    }
                    XDATXMLReader reader = new XDATXMLReader();
                    BaseElement base = reader.parse(fis);

                    String parentPath = f.getParent();

                    if (base instanceof CatCatalogBean){
                        for(CatEntryI entry: ((CatCatalogI)base).getEntries_entry()){
                            String entryPath = StringUtils.ReplaceStr(FileUtils.AppendRootPath(parentPath,entry.getUri()),"\\","/");
                            File temp=getFileOnLocalFileSystem(entryPath);
                            if(temp!=null)
                            	files.add(temp);
                        }
                    }
                } catch (IOException e) {
                    logger.error("",e);
                } catch (SAXException e) {
                    logger.error("",e);
                }
            }

        }
        return files;
    }
	

    /**
     * Returns ArrayList of java.lang.String objects
     * @return
     */
    public ArrayList getCorrespondingFileNames(String rootPath)
    {
        if (fileNames==null)
        {
            fileNames = new ArrayList();
            ArrayList files = getCorrespondingFiles(rootPath);
            for (int i=0;i<files.size();i++){
                File f = (File)files.get(i);
                fileNames.add(f.getName());
            }
        }
        return fileNames;
    }

    /**
     * Appends this path to the enclosed URI or path variables.
     * @param root
     */
    public void appendToPaths(String root){
        if (!FileUtils.IsAbsolutePath(this.getUri())){
            try {
                    this.setUri(root + this.getUri());
            } catch (Exception e) {
                logger.error("",e);
            }
        }
    }

    public File getCatalogFile(String rootPath){
        String fullPath = getFullPath(rootPath);
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
			if(this.getLabel()!=null){
				cat.setId(this.getLabel());
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

    public void deleteFromFileSystem(String rootPath){
        super.deleteFromFileSystem(rootPath);

        //File f = getCatalogFile(rootPath);
        File f = new File(getFullPath(rootPath));

        if (f.exists()){
            try {
		FileUtils.MoveToCache(f);
		if(FileUtils.CountFiles(f.getParentFile(),true)==0){
			FileUtils.DeleteFile(f.getParentFile());
		}
	    } catch (FileNotFoundException e) {
		e.printStackTrace();
	    } catch (IOException e) {
		e.printStackTrace();
	    }
        }
    }
    
    public int entryCount =0;
    public boolean formalizeCatalog(CatCatalogI cat, String catPath){
    	boolean modified=false;
    	for(CatCatalogI subSet:cat.getSets_entryset()){
    		if(formalizeCatalog(subSet,catPath)){
    			modified=true;
    		}
    	}
    	for(CatEntryI entry: cat.getEntries_entry()){
	    	if(entry.getId()==null || !entry.getId().equals("")){
		        String entryPath = StringUtils.ReplaceStr(FileUtils.AppendRootPath(catPath,entry.getUri()),"\\","/");
		        File f =getFileOnLocalFileSystem(entryPath);
		        if(f!=null){
			        ((CatEntryBean)entry).setId((entryCount++) + "/" + f.getName());
			        modified=true;
		        }else{
		        	logger.error("Missing Resource:" + entryPath);
		        }
	    	}
	    }
	    
	    return modified;
    }
    
    public CatCatalogBean getCleanCatalog(String rootPath,boolean includeFullPaths){
    	try {
			File catF=getCatalogFile(rootPath);
			if(catF.getName().endsWith(".gz")){
				try {
					FileUtils.GUnzipFiles(catF);
					catF=getCatalogFile(rootPath);
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
                this.entryCount=0;
                if( formalizeCatalog(cat, parentPath)){
                    //save file
//                    FileWriter writer = new FileWriter(catF);
//                    cat.toXML(writer, true);
//                    writer.close();
                }
                
                
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

    Integer count = null;
    public Integer getCount(String rootPath){
        if (count ==null){
            long sizeI = 0;
            int countI = 0;
            Iterator files = this.getCorrespondingFiles(rootPath).iterator();
            while (files.hasNext()){
                File f = (File)files.next();
               
                if (f!=null && f.exists() && !f.getName().endsWith("catalog.xml")){
                    countI++;
                    sizeI+=f.length();
                }
            }

            size = new Long(sizeI);
            count = new Integer(countI);
        }
        return count;
    }

    Long size = null;
    public long getSize(String rootPath){
        if (size ==null){
            long sizeI = 0;
            int countI = 0;
            Iterator files = this.getCorrespondingFiles(rootPath).iterator();
            while (files.hasNext()){
                File f = (File)files.next();
                if (!f.getName().endsWith("catalog.xml")){
                    countI++;
                    sizeI+=f.length();
                }
            }

            size = new Long(sizeI);
            count = new Integer(countI);
        }
        return size.longValue();
    }
    
    public void moveTo(File newSessionDir,String existingSessionDir,String rootPath,XDATUser user) throws IOException,Exception{
    	String uri = this.getUri();
    	
    	String relativePath=null;
    	if(existingSessionDir!=null && uri.startsWith(existingSessionDir)){
    		relativePath=uri.substring(existingSessionDir.length());
    	}else{
    		if(FileUtils.IsAbsolutePath(uri)){
    			if(uri.indexOf("/")>0){
    				relativePath=uri.substring(uri.indexOf("/")+1);
    			}else if(uri.indexOf("\\")>0){
    				relativePath=uri.substring(uri.indexOf("\\")+1);
    			}else{
    				relativePath=uri;
    			} 
    		}else{
    			relativePath=uri;
    		}
    	}
    	
    	File newFile = new File(newSessionDir,relativePath);
    	File parentDir=newFile.getParentFile();
    	if(!parentDir.exists())
    	{
    		parentDir.mkdirs();
    	}

    	File catalog =this.getCatalogFile(rootPath);
    	
    	InputStream fis = new FileInputStream(catalog);
        if (catalog.getName().endsWith(".gz"))
        {
            fis = new GZIPInputStream(fis);
        }
        XDATXMLReader reader = new XDATXMLReader();
        BaseElement base = reader.parse(fis);

        String parentPath = catalog.getParent();

        if (base instanceof CatCatalogBean){
        	moveCatalogEntries((CatCatalogBean)base,catalog.getParent(),newFile.getParent());
        }
        
        try {
			FileWriter fw = new FileWriter(catalog);
			base.toXML(fw, true);
			fw.close();
		} catch (IOException e) {
			logger.error("",e);
		}
		
    	FileUtils.MoveFile(catalog, newFile, true, true);
    	
    	this.setUri(newFile.getAbsolutePath());
    	this.save(user, true, false);
    }
    
    public void moveCatalogEntries(CatCatalogI cat,String existingRootPath,String newRootPath) throws IOException{
    	for(CatEntryI entry: cat.getEntries_entry()){
    		File newLocation = null;
    		File existingLocation=null;
    		String relativePath=null;
    		
    		String uri= entry.getUri();
    		if(FileUtils.IsAbsolutePath(entry.getUri())){
    			existingLocation=new File(entry.getUri());
    			if(entry.getUri().startsWith(existingRootPath)){
    	    		relativePath=uri.substring(existingRootPath.length());
    			}else{
    				if(FileUtils.IsAbsolutePath(entry.getUri())){
    	    			if(entry.getUri().indexOf("/")>0){
    	    				relativePath=uri.substring(entry.getUri().indexOf("/")+1);
    	    			}else if(uri.indexOf("\\")>0){
    	    				relativePath=uri.substring(entry.getUri().indexOf("\\")+1);
    	    			}else{
    	    				relativePath=uri;
    	    			} 
    	    		}else{
    	    			relativePath=uri;
    	    		}
    			}
    			((CatEntryBean)entry).setUri(relativePath);
    		}else{
    			existingLocation=new File(existingRootPath,uri);
    			relativePath=uri;
    		}
    		
    		
    		
    		File newFile = new File(newRootPath,relativePath);
        	File parentDir=newFile.getParentFile();
        	if(!parentDir.exists())
        	{
        		parentDir.mkdirs();
        	}
        	
        	if(!existingLocation.exists())
        		existingLocation=getFileOnLocalFileSystem(existingLocation.getAbsolutePath());
        	
        	if(existingLocation!=null && existingLocation.exists()){
            	FileUtils.MoveFile(existingLocation, newFile, true, true);
        	}
    	}
    	
    	for(CatCatalogI subset: cat.getSets_entryset()){
    		moveCatalogEntries(subset, existingRootPath, newRootPath);
    	}
    }
}
