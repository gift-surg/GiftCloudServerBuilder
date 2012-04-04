// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Sep 26 09:10:46 CDT 2006
 *
 */
package org.nrg.xdat.om.base;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.regex.Pattern;

import org.nrg.xdat.om.base.auto.AutoXnatResourceseries;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xft.utils.StringUtils;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BaseXnatResourceseries extends AutoXnatResourceseries {

	public BaseXnatResourceseries(ItemI item)
	{
		super(item);
	}

	public BaseXnatResourceseries(UserI user)
	{
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatResourceseries(UserI user)
	 **/
	public BaseXnatResourceseries()
	{}

	public BaseXnatResourceseries(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

    protected ArrayList files=null;
    protected ArrayList fileNames=null;

    private String fullPath = null;


    public String getFullPath(String rootpath){
        if (fullPath==null)
        {
            fullPath = StringUtils.ReplaceStr(FileUtils.AppendRootPath(rootpath,this.getPath()),"\\","/");
            while (fullPath.indexOf("//")!=-1)
            {
                fullPath =StringUtils.ReplaceStr(fullPath,"//","/");
            }

            if(!fullPath.endsWith("/"))
            {
                fullPath+="/";
            }
        }
        return fullPath;
    }

    /**
     * Returns ArrayList of java.io.File objects
     * @return
     */
    public ArrayList<File> getCorrespondingFiles(String rootPath)
    {
        if (files==null)
        {
            files = new ArrayList();
            String fullPath = getFullPath(rootPath);
            if (!fullPath.endsWith(File.separator))
            {
                fullPath += File.separator;
            }
            File dir = new File(fullPath);
            if (dir.exists())
            {
                String pattern = this.getPattern();
                if (pattern==null)
                {
                    files = new ArrayList(Arrays.asList(dir.listFiles()));
                }else{
                    XNATFileFilter filter= new XNATFileFilter();
                    filter.setPattern(pattern);
                    files = new ArrayList(Arrays.asList(dir.listFiles(filter)));
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
            String fullPath = getFullPath(rootPath);
            if (!fullPath.endsWith(File.separator))
            {
                fullPath += File.separator;
            }
            File dir = new File(fullPath);
            if (dir.exists())
            {
                String pattern = this.getPattern();
                if (pattern==null)
                {
                    fileNames = new ArrayList(Arrays.asList(dir.list()));
                }else{
                    XNATFileNameFilter filter= new XNATFileNameFilter();
                    filter.setPattern(pattern);
                    fileNames = new ArrayList(Arrays.asList(dir.list(filter)));
                }
            }
        }
        return fileNames;
    }

    public class XNATFileFilter implements java.io.FileFilter {
        private Pattern _pattern=null;
        public XNATFileFilter()
        {
        }

        public void setPattern(String pattern){
            _pattern=java.util.regex.Pattern.compile(pattern);
        }

        public boolean accept(File f)
        {
            if (_pattern.matcher(f.getName()).find())
            {
                return true;
            }else{
                return false;
            }

        }
    }

    public class XNATFileNameFilter implements FilenameFilter {
        private Pattern _pattern=null;
        public XNATFileNameFilter()
        {
        }

        public void setPattern(String pattern){
            _pattern=java.util.regex.Pattern.compile(pattern);
        }

        public boolean accept(File dir,String name)
        {
            if (_pattern.matcher(name).find())
            {
                return true;
            }else{
                return false;
            }
        }
    }

    /**
     * Prepends this path to the enclosed URI or path variables.
     * @param root
     */
    public void prependPathsWith(String root){
        if (!FileUtils.IsAbsolutePath(this.getPath())){
            try {
                this.setPath(root + this.getPath());
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
        String uri = this.getPath();
        uri= uri.replace('\\', '/');
        if (uri.indexOf(indexOf)==-1){
            if (!caseSensitive){
                int index = uri.toLowerCase().indexOf(indexOf.toLowerCase());
                if (index!=-1){
                    this.setPath(uri.substring(index + 1));
                }
            }
        }else{
            this.setPath(uri.substring(uri.indexOf(indexOf)+ 1));
        }
    }

    /**
     * Appends this path to the enclosed URI or path variables.
     * @param root
     */
    public ArrayList<String> getUnresolvedPaths(){
        ArrayList<String> al = new ArrayList<String>();
        String p = getPath();
        p.replace('\\', '/');
        if (!p.endsWith("/"))
        {
            p +="/";
        }
        al.add(p + getPattern());
        return al;
    }


    public void moveTo(File newSessionDir,String existingSessionDir,String rootPath,XDATUser user) throws IOException,Exception{
    	String uri = this.getPath();
    	
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
    	if(!newFile.exists())
    	{
    		newFile.mkdirs();
    	}
    	
    	for(File f: this.getCorrespondingFiles(rootPath)){
    		FileUtils.MoveFile(f, new File(newFile,f.getName()), true, true);
    	}
    	
    	this.setPath(newFile.getAbsolutePath());
    	SaveItemHelper.authorizedSave(this,user, true, false);
    }


	@Override
	public String getPath() {
		if( super.getPath()!=null){
			return super.getPath().replace('\\', '/');
		}else{
			return null;
		}
	}
}
