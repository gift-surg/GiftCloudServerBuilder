// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Wed Sep 27 12:10:56 CDT 2006
 *
 */
package org.nrg.xdat.om.base;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import org.nrg.xdat.om.base.auto.AutoXnatDicomseriesImage;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;

/**
 * @author XDAT
 *
 */
@SuppressWarnings("serial")
public class BaseXnatDicomseriesImage extends AutoXnatDicomseriesImage {

	public BaseXnatDicomseriesImage(ItemI item)
	{
		super(item);
	}

	public BaseXnatDicomseriesImage(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatDicomseriesImage(UserI user)
	 **/
	public BaseXnatDicomseriesImage()
	{}

	public BaseXnatDicomseriesImage(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

    public java.io.File getFile(String rootPath){
        java.io.File f = new java.io.File(org.nrg.xft.utils.FileUtils.AppendRootPath(rootPath,this.getUri()));
        if (!f.exists() && !getUri().endsWith(".gz"))
        {
            f = new java.io.File(org.nrg.xft.utils.FileUtils.AppendRootPath(rootPath,this.getUri()) + ".gz");
        }
        return f;
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

    /**
     * Relatives this path from the first occurence of the indexOf string.
     * @param indexOf
     */
    public void relativizePaths(String indexOf, boolean caseSensitive){
        String uri = this.getUri();
        uri= uri.replace('\\', '/');
        if (uri.indexOf(indexOf)==-1){
            if (!caseSensitive){
                int index = uri.toLowerCase().indexOf(indexOf.toLowerCase());
                if (index!=-1){
                    this.setUri(uri.substring(index + 1));
                }
            }
        }else{
            this.setUri(uri.substring(uri.indexOf(indexOf) + 1));
        }
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
    		}
    	}
    	
    	File newFile = new File(newSessionDir,relativePath);
    	File parentDir=newFile.getParentFile();
    	if(!parentDir.exists())
    	{
    		parentDir.mkdirs();
    	}
    	
    	File f=this.getFile(rootPath);
    	if(f !=null){
    		FileUtils.MoveFile(f, newFile, true);
    	}
    	
    	this.setUri(newFile.getAbsolutePath());
    }
}
