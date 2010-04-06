// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Fri Jan 04 15:44:10 CST 2008
 *
 */
package org.nrg.xdat.om.base;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;

import org.nrg.xdat.bean.CatCatalogBean;
import org.nrg.xdat.bean.CatEntryBean;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatDicomseries;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatQcscandataI;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xdat.om.XnatResourceseries;
import org.nrg.xdat.om.base.auto.AutoXnatImagescandata;
import org.nrg.xft.ItemI;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xnat.exceptions.InvalidArchiveStructure;

/**
 * @author XDAT
 *
 */
@SuppressWarnings("serial")
public class BaseXnatImagescandata extends AutoXnatImagescandata {

	public BaseXnatImagescandata(ItemI item) {
		super(item);
	}

	public BaseXnatImagescandata(UserI user) {
		super(user);
	}

	/*
	 * @deprecated Use BaseXnatImagescandata(UserI user)
	 **/
	public BaseXnatImagescandata()
	{}

	public BaseXnatImagescandata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}
    private ArrayList _cachedXnatFiles= null;

    public String getPreCount()
    {
        try {
            String temp = this.getId();
            if (temp != null)
            {
                if (temp.indexOf("-")!=-1)
                {
                    return temp.substring(0,temp.indexOf("-"));
                }else{
                    return temp;
                }
            }else{
                return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    public String getPostCount()
    {
        try {
            String temp = this.getId();
            if (temp != null)
            {
                if (temp.indexOf("-")!=-1)
                {
                    return temp.substring(temp.indexOf("-")+1);
                }else{
                    return "";
                }
            }else{
                return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    public Integer getPreCountI()
    {
        String s = getPreCount();
        try{
            if (s == "")
            {
                return new Integer(0);
            }else{
                return new Integer(s);
            }
        } catch (NumberFormatException e) {
            return new Integer(0);
        }
    }

    public Integer getPostCountI()
    {
        String s = getPostCount();
        try {
            if (s == "")
            {
                return new Integer(0);
            }else{
                return new Integer(s);
            }
        } catch (NumberFormatException e) {
            return new Integer(0);
        }
    }

    public boolean isInRAWDirectory(){
        boolean hasRAW=false;
        Iterator files = getFile().iterator();
        while (files.hasNext()){
            XnatAbstractresource file = (XnatAbstractresource)files.next();
            if (file.isInRAWDirectory())
            {
                hasRAW=true;
                break;
            }
        }
        return hasRAW;
    }




    public void deleteFilesFromFileSystem(String rootPath){
        ArrayList<XnatAbstractresource> files = this.getFile();
        if (!files.isEmpty())
        for (XnatAbstractresource resource:files){
            resource.deleteFromFileSystem(rootPath);
        }
    }

    public ArrayList<java.io.File> getJavaFiles(String rootPath){
        ArrayList<File> jFiles = new ArrayList<File>();
        ArrayList scanFiles= this.getFile();
        if (scanFiles.size()>0)
        {
            Iterator files = scanFiles.iterator();
            while (files.hasNext())
            {
                XnatAbstractresource xnatFile = (XnatAbstractresource) files.next();
                jFiles.addAll(xnatFile.getCorrespondingFiles(rootPath));
            }
        }
        return jFiles;
    }

    public static Comparator GetComparator()
    {
        return (new BaseXnatImagescandata()).getComparator();
    }

    public Comparator getComparator()
    {
        return new ImageScanComparator();
    }
    
    public class ImageScanComparator implements Comparator{
        public ImageScanComparator()
        {
        }
        public int compare(Object o1, Object o2) {
            BaseXnatImagescandata  value1 = (BaseXnatImagescandata)(o1);
            BaseXnatImagescandata value2 = (BaseXnatImagescandata)(o2);

            if (value1 == null){
                if (value2 == null)
                {
                    return 0;
                }else{
                    return -1;
                }
	    }
            if (value2== null)
            {
		return 1;
	    }

            if (value1.getPreCountI().equals(value2.getPreCountI()))
            {
                return value1.getPostCountI().compareTo(value2.getPostCountI());
            }else{
                Integer i1 = value1.getPreCountI();
                Integer i2 = value2.getPreCountI();
                int _return = i1.compareTo(i2);
                return _return;
            }
        }
    }
    private String scan_dir = null;
    public String deriveScanDir(){
        if (scan_dir==null)
        {
            final String rootPath =this.getImageSessionData().getArchiveRootPath();
            String last_dir = null;
                for (XnatAbstractresource xnatFile:this.getFile())
                {
                    if (xnatFile instanceof org.nrg.xdat.om.XnatResource){
                        XnatResource resource = (XnatResource)xnatFile;
                        String uri =resource.getFullPath(rootPath);
                        if (last_dir==null){
                            last_dir= uri;
                            int index = last_dir.toUpperCase().indexOf("/RAW/");
                            if (index!=-1){
                            	if(last_dir.toUpperCase().indexOf("/"+this.getId().toUpperCase()+"/",index)>-1){
                                    scan_dir = last_dir.substring(0,last_dir.toUpperCase().indexOf("/"+this.getId().toUpperCase()+"/",index)+this.getId().length()+1);
	    } else {
                            		scan_dir = last_dir.substring(0,index+4);
                            	}
                                return scan_dir;
                            }else{
                            	index = last_dir.toUpperCase().indexOf("/SCANS/");
                                if (index!=-1){
                                    if(last_dir.toUpperCase().indexOf("/"+this.getId().toUpperCase()+"/",index)>-1){
                                        scan_dir = last_dir.substring(0,last_dir.toUpperCase().indexOf("/"+this.getId().toUpperCase()+"/",index)+this.getId().length()+1);
                                	}else{
                                		scan_dir = last_dir.substring(0,index+6);
                                	}
                                    return scan_dir;
                                }
                            }
                        }else{

                        }
                    }else if(xnatFile instanceof org.nrg.xdat.om.XnatDicomseries){
                        XnatDicomseries resource = (XnatDicomseries)xnatFile;
                        String uri =resource.getFullPath(rootPath);
                        if (last_dir==null){
                            last_dir= uri;
                            int index = last_dir.toUpperCase().indexOf("/RAW/");
                            if (index!=-1){
                            	if(last_dir.toUpperCase().indexOf("/"+this.getId().toUpperCase()+"/",index)>-1){
                                    scan_dir = last_dir.substring(0,last_dir.toUpperCase().indexOf("/"+this.getId().toUpperCase()+"/",index)+this.getId().length()+1);
                            	}else{
                            		scan_dir = last_dir.substring(0,index+4);
                            	}
                                return scan_dir;
                            }else{
                            	index = last_dir.toUpperCase().indexOf("/SCANS/");
                                if (index!=-1){
                                    if(last_dir.toUpperCase().indexOf("/"+this.getId().toUpperCase()+"/",index)>-1){
                                        scan_dir = last_dir.substring(0,last_dir.toUpperCase().indexOf("/"+this.getId().toUpperCase()+"/",index)+this.getId().length()+1);
                                	}else{
                                		scan_dir = last_dir.substring(0,index+6);
                                	}
                                    return scan_dir;
                                }
                            }
                        }else{

                        }
                    }else if(xnatFile instanceof org.nrg.xdat.om.XnatResourceseries){
                        XnatResourceseries resource = (XnatResourceseries)xnatFile;
                        String uri =resource.getFullPath(rootPath);
                        if (last_dir==null){
                            last_dir= uri;
                            int index = last_dir.toUpperCase().indexOf("/RAW/");
                            if (index!=-1){
                            	if(last_dir.toUpperCase().indexOf("/"+this.getId().toUpperCase()+"/",index)>-1){
                                    scan_dir = last_dir.substring(0,last_dir.toUpperCase().indexOf("/"+this.getId().toUpperCase()+"/",index)+this.getId().length()+1);
                            	}else{
                            		scan_dir = last_dir.substring(0,index+4);
                            	}
                                return scan_dir;
                            }else{
                            	index = last_dir.toUpperCase().indexOf("/SCANS/");
                                if (index!=-1){
                                    if(last_dir.toUpperCase().indexOf("/"+this.getId().toUpperCase()+"/",index)>-1){
                                        scan_dir = last_dir.substring(0,last_dir.toUpperCase().indexOf("/"+this.getId().toUpperCase()+"/",index)+this.getId().length()+1);
                                	}else{
                                		scan_dir = last_dir.substring(0,index+6);
                                	}
                                    return scan_dir;
                                }
                            }
                        }else{

                        }
                    }
	    }

            if (scan_dir ==null)
            {
                scan_dir = this.getImageSessionData().deriveRawDir();
	}
    }


        return scan_dir;
    }

    private XnatImagesessiondata mr = null;

    public XnatImagesessiondata getImageSessionData()
    {
        if (mr==null)
        {
            ArrayList al = XnatImagesessiondata.getXnatImagesessiondatasByField("xnat:imageSessionData/ID",this.getImageSessionId(),this.getUser(),false);
            if (al.size()>0)
            {
                mr = (XnatImagesessiondata)al.get(0);
            }
        }

        return mr;
    }
    
    public void setImageSessionData(XnatImagesessiondata ses){
    	mr=ses;
    }
    
    Snapshot snapshot=null;
    public Snapshot getSnapshot(){
    	if(snapshot!=null){
    		return snapshot;
    	}
    	
		if(this.getFile().size()>0){
			snapshot=new Snapshot();
		    XnatImagesessiondata ses=this.getImageSessionData();
		    String label=ses.getIdentifier(ses.getProject(),false);
		    String labelImg=label + "_"+this.getId() + "_qc";

		    String path=null;
		    for(XnatAbstractresource res: this.getFile()){
				if(res.getLabel()!=null && res.getLabel().equalsIgnoreCase("SNAPSHOTS")){
				    path="/REST/experiments/"+ses.getId() + "/scans/"+this.getId() + "/resources/SNAPSHOTS/files/";
				    if(res instanceof XnatResourcecatalog){
						CatCatalogBean cat=((XnatResourcecatalog)res).getCleanCatalog(ses.getProjectData().getRootArchivePath(), false);
						for(CatEntryBean entry:cat.getEntries_entry()){
							if(entry.getContent().equalsIgnoreCase("THUMBNAIL")){
							    snapshot.setThumbnail(path + entry.getUri());
							}
							if(entry.getContent().equalsIgnoreCase("ORIGINAL")){
							    snapshot.setFull(path + entry.getUri());
							}
						}
				    }else{
					    snapshot.setFull(path + labelImg + ".gif");
					    snapshot.setThumbnail(path + labelImg + "_t.gif");
				    }
				} 
		    }
	
		    if(path==null){
		    	path="/app/template/QualityControl.vm?project=" + ses.getProject() + "&session_label=" + label + "&session_id=" + ses.getId() + "&scan="+ this.getId();
		    	snapshot.setFull(path + "&extension=.gif");
		    	snapshot.setThumbnail(path + "&extension=_t.gif");
			}
		    
		    return snapshot;
		}else{
		    return null;
		}
    }
    
    public class Snapshot{
	private String thumbnail=null;
	private String full=null;
	
	public String getFull() {
	    return full;
	}
	public void setFull(String full) {
	    this.full = full;
	}
	public String getThumbnail() {
	    return thumbnail;
	}
	public void setThumbnail(String thumbnail) {
	    this.thumbnail = thumbnail;
	}
	
	
    }
    
    public void copyValuesFrom(final XnatImagescandata other) throws Exception {
    	if (null != other.getType())
		    this.setType(other.getType());
		if (null != other.getQuality())
			this.setQuality(other.getQuality());
		if (null != other.getNote())
			this.setNote(other.getNote());
	}

	public XnatQcscandataI getManualQC() {
		final XnatImagesessiondata session = getImageSessionData();
		if (session.getManualQC() != null && session.getManualQC().getScans_scan() != null) {
			for (XnatQcscandataI qc : session.getManualQC().getScans_scan()) {
				if (getId().equals(qc.getImagescanId())) {
					return qc;
				}
			}
		}
		return null;
	}

	public File getExpectedSessionDir() throws InvalidArchiveStructure{
		return this.getImageSessionData().getExpectedSessionDir();
	}

	@Override
	public void preSave() throws Exception{
		super.preSave();

		if(this.getImageSessionData()==null){
			throw new Exception("Unable to identify image session for:" + this.getImageSessionId());
		}

		final String expectedPath=this.getExpectedSessionDir().getAbsolutePath().replace('\\', '/');
		
		for(final XnatAbstractresource res: this.getFile()){
			final String uri;
			if(res instanceof XnatResource){
				uri=((XnatResource)res).getUri();
			}else if(res instanceof XnatResourceseries){
				uri=((XnatResourceseries)res).getPath();
			}else{
				continue;
			}
			
			FileUtils.ValidateUriAgainstRoot(uri,expectedPath,"URI references data outside of the project:" + uri);
		}
	}
}
