// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Sep 26 09:10:46 CDT 2006
 *
 */
package org.nrg.xdat.om.base;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;

import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatAbstractresourceTag;
import org.nrg.xdat.om.base.auto.AutoXnatAbstractresource;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;

/**
 * @author XDAT
 *
 */
@SuppressWarnings("serial")
public abstract class BaseXnatAbstractresource extends AutoXnatAbstractresource {

	public BaseXnatAbstractresource(ItemI item)
	{
		super(item);
	}

	public BaseXnatAbstractresource(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatAbstractresource(UserI user)
	 **/
	public BaseXnatAbstractresource()
	{}

	public BaseXnatAbstractresource(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

    /**
     * Returns ArrayList of java.io.File objects
     * @return
     */
    public abstract ArrayList<File> getCorrespondingFiles(String rootPath);


    /**
     * Returns ArrayList of java.lang.String objects
     * @return
     */
    public abstract ArrayList getCorrespondingFileNames(String rootPath);


    Long size = null;
    public long getSize(String rootPath){
        if (size ==null){
            long sizeI = 0;
            int countI = 0;
            Iterator files = this.getCorrespondingFiles(rootPath).iterator();
            while (files.hasNext()){
                File f = (File)files.next();
                if (f.exists()){
                    countI++;
                    sizeI+=f.length();
                }
            }

            size = new Long(sizeI);
            count = new Integer(countI);
        }
        return size.longValue();
    }

    Integer count = null;
    public Integer getCount(String rootPath){
        if (count ==null){
            long sizeI = 0;
            int countI = 0;
            Iterator files = this.getCorrespondingFiles(rootPath).iterator();
            while (files.hasNext()){
                File f = (File)files.next();
                if (f.exists()){
                    countI++;
                    sizeI+=f.length();
                }
            }

            size = new Long(sizeI);
            count = new Integer(countI);
        }
        return count;
    }

    /**
     * Appends this path to the enclosed URI or path variables.
     * @param root
     */
    public abstract void appendToPaths(String root);

    /**
     * Relatives this path from the first occurence of the indexOf string.
     * @param indexOf
     */
    public abstract void relativizePaths(String indexOf, boolean caseSensitive);

    /**
     * Appends this path to the enclosed URI or path variables.
     * @param root
     */
    public abstract ArrayList<String> getUnresolvedPaths();

    public boolean isInRAWDirectory(){
        boolean hasRAW= false;
        for (String path : getUnresolvedPaths())
        {
            if (path.indexOf("RAW/")!=-1)
            {
                hasRAW=true;
                break;
            }
            if (path.indexOf("SCANS/")!=-1)
            {
                hasRAW=true;
                break;
            }
        }
        return hasRAW;
    }

    /**
     * Path to Files
     * @return
     */
    public String getFullPath(String rootPath){
        return "";
    }

    public String getContent(){
        return "";
    }

    public String getFormat(){
        return "";
    }


    public void deleteFromFileSystem(String rootPath){
        ArrayList<File> files = this.getCorrespondingFiles(rootPath);
        for(File f: files){
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
    
    public String getTagString(){
    	StringBuffer sb =new StringBuffer();
    	for(XnatAbstractresourceTag tag:this.getTags_tag()){
    		if(sb.length()>0){
    			sb.append(",");
    		}
    		if(tag.getName()!=null){
    			sb.append(tag.getName()).append("=");
    		}
    		sb.append(tag.getTag());
    	}
    	
    	return sb.toString();
    }
    
    private String base_URI=null;
    public String getBaseURI(){return base_URI;}
    public void setBaseURI(String b){
    	if(b.startsWith("/REST")){
    		this.base_URI=b;
    	}else{
    		this.base_URI="/REST" +b;
    	}
    }
    
    
    public abstract void moveTo(File newSessionDir, String existingSessionDir,String rootPath,XDATUser user) throws IOException,Exception;
}
