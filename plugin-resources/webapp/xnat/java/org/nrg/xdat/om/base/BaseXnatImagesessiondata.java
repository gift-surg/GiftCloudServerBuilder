/**
 * Copyright 2005 Harvard University / Howard Hughes Medical Institute (HHMI) All Rights Reserved
 * Copyright (c) 2008 Washington University
 */
package org.nrg.xdat.om.base;
import java.io.File;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.nrg.dcm.CopyOp;
import org.nrg.transaction.*;
import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.bean.CatCatalogBean;
import org.nrg.xdat.bean.CatEntryBean;
import org.nrg.xdat.bean.CatEntryMetafieldBean;
import org.nrg.xdat.model.ScrScreeningassessmentI;
import org.nrg.xdat.model.ValProtocoldataI;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatAbstractresourceTagI;
import org.nrg.xdat.model.XnatExperimentdataFieldI;
import org.nrg.xdat.model.XnatExperimentdataShareI;
import org.nrg.xdat.model.XnatImageassessordataI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.model.XnatQcassessmentdataI;
import org.nrg.xdat.model.XnatQcmanualassessordataI;
import org.nrg.xdat.model.XnatReconstructedimagedataI;
import org.nrg.xdat.om.ScrScreeningassessment;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatDicomseries;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatExperimentdataField;
import org.nrg.xdat.om.XnatExperimentdataShare;
import org.nrg.xdat.om.XnatImageassessordata;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatQcassessmentdata;
import org.nrg.xdat.om.XnatQcmanualassessordata;
import org.nrg.xdat.om.XnatReconstructedimagedata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xdat.om.XnatResourceseries;
import org.nrg.xdat.om.base.auto.AutoScrScreeningassessment;
import org.nrg.xdat.om.base.auto.AutoValProtocoldata;
import org.nrg.xdat.om.base.auto.AutoXnatImagesessiondata;
import org.nrg.xdat.om.base.auto.AutoXnatQcassessmentdata;
import org.nrg.xdat.schema.SchemaElement;
import org.nrg.xdat.security.SecurityValues;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.helpers.merge.ProjectAnonymizer;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFT;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTable;
import org.nrg.xft.db.DBAction;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.db.PoolDBUtils;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.InvalidPermissionException;
import org.nrg.xft.exception.InvalidValueException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.search.TableSearch;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileTracker;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xnat.exceptions.InvalidArchiveStructure;

import org.nrg.xnat.helpers.scanType.ScanTypeMappingI;
import org.nrg.xnat.scanAssessors.AssessorComparator;
import org.nrg.xnat.scanAssessors.ScanAssessorI;
import org.nrg.xnat.srb.XNATDirectory;
import org.nrg.xnat.srb.XNATMetaData;
import org.nrg.xnat.srb.XNATSrbSearch;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.nrg.xnat.turbine.utils.CatalogSet;

import edu.sdsc.grid.io.GeneralFile;

/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public abstract class BaseXnatImagesessiondata extends AutoXnatImagesessiondata {
    public static final String SCAN_ABBR="scan";
    public static final String RECON_ABBR="recon";
    public static final String ASSESSOR_ABBR="assess";
    public static final String RESOURCES_ABBR="uploads";
    public static final String MISC_ABBR="misc";

    private static final Map<String,String> CUSTOM_SCAN_FIELDS =
        ImmutableMap.of("Scan Time", "startTime");
	
    private String lowerCaseSessionId = null;

    private FileTracker _files = new FileTracker();
    private Hashtable fileGroups=new Hashtable();
    private Map<String,String> arcFiles = null;

    private List<XnatImageassessordataI> assessors = null;

    private List<XnatImagescandataI> scans = null;

    protected List<XnatImageassessordataI> minLoadAssessors = null;

	public BaseXnatImagesessiondata(ItemI item)
	{
		super(item);
	}

	public BaseXnatImagesessiondata(UserI user)
	{
		super(user);
	}

	public BaseXnatImagesessiondata()
	{}

	public BaseXnatImagesessiondata(Hashtable properties, UserI user)
	{
		super(properties,user);
	}

    private String _Prearchivepath=null;
	protected  XnatQcmanualassessordata manQC;
	protected  XnatQcassessmentdata qc;
	protected  ScrScreeningassessment scr;
	
    /**
     * @return Returns the prearchivePath.
     */
    public String getPrearchivepath(){
        try{
            if (_Prearchivepath==null){
                _Prearchivepath=getStringProperty("prearchivePath");
                return _Prearchivepath;
            }else {
                return _Prearchivepath;
            }
        } catch (Exception e1) {logger.error(e1);return null;}
    }

    /**
     * Sets the value for prearchivePath.
     * @param v Value to Set.
     */
    public void setPrearchivepath(String v){
        try{
        setProperty(SCHEMA_ELEMENT_NAME + "/prearchivePath",v);
        _Prearchivepath=null;
        } catch (Exception e1) {logger.error(e1);}
    }

    public int getAssessorCount() {
        return getAssessors().size();
    }

    public int getAssessorCount(String elementName) {
        return getAssessors(elementName).size();
    }

    public List<XnatImageassessordataI> getAssessors() {
        if (this.assessors == null) {
            try {
                assessors = this.getAssessors_assessor();

            } catch (Exception e) {
                logger.error("", e);
            }
        }
        return assessors;
    }

    public ArrayList<XnatImageassessordata> getAssessors(String elementName) {
        ArrayList<XnatImageassessordata> temp = new ArrayList<XnatImageassessordata>();
        Iterator iter = getAssessors().iterator();
        while (iter.hasNext()) {
            XnatImageassessordata o = (XnatImageassessordata) iter.next();
            if (o.getXSIType().equalsIgnoreCase(elementName)) {
                temp.add(o);
            }
        }
        return temp;
    }

     
    public String getArchivePath() throws UnknownPrimaryProjectException {
        return getArchivePath(getArchiveRootPath());
    }

    public String getArchivePath(String rootPath) throws UnknownPrimaryProjectException {
        
        String path = "";
        for(XnatImagescandataI scan :  this.getScans_scan()){
            List files = scan.getFile();
            if (files.size() > 0) {
                Iterator fIter = files.iterator();
                while (fIter.hasNext()) {
                    XnatAbstractresource file = (XnatAbstractresource) fIter.next();
                    String filePath = file.getFullPath(rootPath);
                    if (filePath != null && !filePath.equals("")) {
                        try {
                            String dirName = this.getArchiveDirectoryName();
                            int index = filePath.indexOf(dirName);
                            if (index == -1) {
                                index = filePath.indexOf(dirName
                                        .toLowerCase());
                            }
                            if (index == -1) {
                                index = filePath.indexOf(dirName
                                        .toUpperCase());
                            }
                            
                            
                            if (index<0){
                                path = FileUtils.AppendRootPath("",getArchiveRootPath());
                                return path;
                            }
                            if (index != -1) {
                                path = filePath.substring(0, index);
                            }
                            break;
                        } catch (Exception e1) {
                            logger.error("", e1);
                        }

                    }
                }

                if (!path.equals("")) {
                    break;
                }
            }
        }
        

        if (!path.equals("")) {
           path = FileUtils.AppendRootPath(getArchiveRootPath(),path);
        }

        return path;
    }
    public String getRelativeArchivePath() throws UnknownPrimaryProjectException {
        String path = "";
        for(XnatImagescandataI scan :  this.getScans_scan()){
            List files = scan.getFile();
            if (files.size() > 0) {
                Iterator fIter = files.iterator();
                while (fIter.hasNext()) {
                    XnatAbstractresource file = (XnatAbstractresource) fIter.next();
                    for(String filePath: file.getUnresolvedPaths()){
                        if (filePath != null && !filePath.equals("")) {
                            try {
                                String upperFilePath=filePath.toUpperCase();

                                String dirName = this.getArchiveDirectoryName();

                                int index = upperFilePath.indexOf(dirName.toUpperCase());
                                if (index==0){
                                    path = getArchiveRootPath();
                                    return path;
                                }
                                if (index != -1) {
                                    path = filePath.substring(0, index);
                                }
                                break;
                            } catch (Exception e1) {
                                logger.error("", e1);
                            }

                        }
                    }
                }

                if (!path.equals("")) {
                    break;
                }
            }
        }

        if (path.equals("")){
            try {
                String currentarc = getCurrentArchiveFolder();
                if (currentarc ==null){
                    path = getArchiveDirectoryName() + "/";
                }else{
                    currentarc = currentarc.replace('\\', '/');
                    path = currentarc + getArchiveDirectoryName() + "/";
                }
            } catch (InvalidArchiveStructure e) {
                logger.error("",e);
            }
        }else{
            path +=this.getArchiveDirectoryName() +"/";
        }


        return path;
    }

    /**
     * ArrayList of ArrayLists(String filename,String type, String
     * preArchiveSize, String archiveSize)
     *
     * @return
     */
    public ArrayList<ArrayList<String>> getExtraFiles() throws Exception {
        ArrayList<ArrayList<String>> extraFiles = new ArrayList<ArrayList<String>>();
        ArrayList<String> sub = new ArrayList<String>();
      sub.add("misc");
      sub.add("unknown");
      sub.add("0 Files, 0.00Mb");
      sub.add(this.getArchiveStats("misc"));

      extraFiles.add(sub);
      return extraFiles;
    }

    public ArrayList<String> getExtraFileNames() throws Exception{
        ArrayList<String> al =new ArrayList<String>();
        if (this.hasSRBData())
        {
            XNATDirectory dir = getSRBDirectory();
            XNATMetaData meta = new XNATMetaData();
            meta.setCategory("MISC"); //match operator is = by default
            XNATDirectory misc= dir.filterLocal(meta);
            al.addAll(misc.getRelativeFileNames());
        }else{
            ArrayList fileIds = (ArrayList)this.getFileGroups().get("misc");
            if (fileIds!=null){
                Iterator iter = fileIds.iterator();
                while (iter.hasNext()){
                    String fID = (String)iter.next();
                    al.add(this.getFileTracker().getRelativePathByID(fID));
                }
            }
        }
        return al;
    }

    /**
     * @return
     * @throws Exception
     */
    public Map<String,String> getArcFiles() throws Exception {
        if (arcFiles == null) {
            logger.debug("BEGIN LOAD ARC FILES");
            arcFiles = new Hashtable<String,String>();

            if (this.hasSRBData())
            {
                long startTime = System.currentTimeMillis();
                XNATDirectory dir = getSRBDirectory();
                if(XFT.VERBOSE)System.out.println("Time to load " + (System.currentTimeMillis()-startTime) + "ms");
                startTime = System.currentTimeMillis();
                for(XnatImagescandataI scan :  this.getScans_scan()){
                    int count = 0;
                    long size = 0;
                    try {
                        XNATMetaData meta = new XNATMetaData();
                        meta.setCategory("SCAN"); //match operator is = by default
                        meta.setExternalId(scan.getId());
                        XNATDirectory scanDIR= dir.filterLocal(meta);
                        count = scanDIR.getCount();
                        size = scanDIR.getSize();

                        NumberFormat formatter = NumberFormat.getInstance();
                        formatter.setMinimumFractionDigits(2);
                        formatter.setMaximumFractionDigits(2);

                        String stats= count
                                + " files, "
                                + formatter.format(((float) ((float)size / 1048576))) + "Mb";
                        arcFiles.put(scan.getId(), stats);
                    } catch (RuntimeException e) {
                        logger.error("",e);
                        arcFiles.put(scan.getId(),"Error");
                    }
                }

                //POPULATE EXTRA FILES
//                int count = 0;
//                long size = 0;
//                try {
//                    XNATMetaData meta = new XNATMetaData();
//                    meta.setCategory("MISC"); //match operator is = by default
//                    XNATDirectory scanDIR= dir.filterLocal(meta);
//                    count = scanDIR.getCount();
//                    size = scanDIR.getSize();
//
//                    NumberFormat formatter = NumberFormat.getInstance();
//                    formatter.setMinimumFractionDigits(2);
//                    formatter.setMaximumFractionDigits(2);
//
//                    String stats= count
//                            + " files, "
//                            + formatter.format(((float) ((float)size / 1048576))) + "Mb";
//                    arcFiles.put("misc", stats);
//                } catch (RuntimeException e) {
//                    logger.error("",e);
//                    arcFiles.put("misc","Error");
//                }
                if(XFT.VERBOSE)System.out.println("Time to sort " + (System.currentTimeMillis()-startTime) + "ms");
                startTime = System.currentTimeMillis();
            }else{
                loadLocalFiles();

                for(XnatImagescandataI scan :  this.getScans_scan()){
                    int count = 0;
                    long size = 0;
                    try {
                        String parsedScanID= StringUtils.ReplaceStr(StringUtils.ReplaceStr(scan.getId(),"-",""),"*","AST");

                        ArrayList fileGroup = (ArrayList)getFileGroups().get(SCAN_ABBR + parsedScanID);
                        if (fileGroup==null){

                        }else{
                            Iterator iter = fileGroup.iterator();
                            while(iter.hasNext())
                            {
                                String fID = (String)iter.next();
                                int idIndex = this.getFileTracker().getIDIndex(fID);
                                size += this.getFileTracker().getSize(idIndex);
                            }
                            count = fileGroup.size();
                        }

                        NumberFormat formatter = NumberFormat.getInstance();
                        formatter.setMinimumFractionDigits(2);
                        formatter.setMaximumFractionDigits(2);

                        String stats= count
                                + " files, "
                                + formatter.format(((float) ((float)size / 1048576))) + "Mb";
                        arcFiles.put(SCAN_ABBR + scan.getId(), stats);
                    } catch (RuntimeException e) {
                        logger.error("",e);
                        arcFiles.put(SCAN_ABBR + scan.getId(),"Error");
                    }
                }

                for(XnatReconstructedimagedataI recon :  this.getReconstructions_reconstructedimage()){
                    int count = 0;
                    long size = 0;
                    try {
                        String parsedScanID= StringUtils.ReplaceStr(StringUtils.ReplaceStr(recon.getId(),"-",""),"*","AST");

                        ArrayList fileGroup = (ArrayList)getFileGroups().get(RECON_ABBR + parsedScanID);
                        if (fileGroup==null){

                        }else{
                            Iterator iter = fileGroup.iterator();
                            while(iter.hasNext())
                            {
                                String fID = (String)iter.next();
                                int idIndex = this.getFileTracker().getIDIndex(fID);
                                size += this.getFileTracker().getSize(idIndex);
                            }
                            count = fileGroup.size();
                        }

                        NumberFormat formatter = NumberFormat.getInstance();
                        formatter.setMinimumFractionDigits(2);
                        formatter.setMaximumFractionDigits(2);

                        String stats= count
                                + " files, "
                                + formatter.format(((float) ((float)size / 1048576))) + "Mb";
                        arcFiles.put(RECON_ABBR + recon.getId(), stats);
                    } catch (RuntimeException e) {
                        logger.error("",e);
                        arcFiles.put(RECON_ABBR + recon.getId(),"Error");
                    }
                }

                for(XnatImageassessordataI assess :  this.getAssessors_assessor()){
                    int count = 0;
                    long size = 0;
                    try {
                        String parsedScanID= StringUtils.ReplaceStr(StringUtils.ReplaceStr(assess.getId(),"-",""),"*","AST");

                        ArrayList fileGroup = (ArrayList)getFileGroups().get(ASSESSOR_ABBR + parsedScanID);
                        if (fileGroup==null){

                        }else{
                            Iterator iter = fileGroup.iterator();
                            while(iter.hasNext())
                            {
                                String fID = (String)iter.next();
                                int idIndex = this.getFileTracker().getIDIndex(fID);
                                size += this.getFileTracker().getSize(idIndex);
                            }
                            count = fileGroup.size();
                        }

                        NumberFormat formatter = NumberFormat.getInstance();
                        formatter.setMinimumFractionDigits(2);
                        formatter.setMaximumFractionDigits(2);

                        String stats= count
                                + " files, "
                                + formatter.format(((float) ((float)size / 1048576))) + "Mb";
                        arcFiles.put(ASSESSOR_ABBR +assess.getId(), stats);
                    } catch (RuntimeException e) {
                        logger.error("",e);
                        arcFiles.put(ASSESSOR_ABBR +assess.getId(),"Error");
                    }
                }

                for(XnatAbstractresourceI res :  this.getResources_resource()){
                    int count = 0;
                    long size = 0;
                    try {
                    	String parsedScanID= res.getXnatAbstractresourceId().toString();
                        
                        ArrayList fileGroup = (ArrayList)getFileGroups().get(RESOURCES_ABBR + parsedScanID);
                        if (fileGroup==null){

                        }else{
                            Iterator iter = fileGroup.iterator();
                            while(iter.hasNext())
                            {
                                String fID = (String)iter.next();
                                int idIndex = this.getFileTracker().getIDIndex(fID);
                                size += this.getFileTracker().getSize(idIndex);
                            }
                            count = fileGroup.size();
                        }

                        NumberFormat formatter = NumberFormat.getInstance();
                        formatter.setMinimumFractionDigits(2);
                        formatter.setMaximumFractionDigits(2);

                        String stats= count
                                + " files, "
                                + formatter.format(((float) ((float)size / 1048576))) + "Mb";
                        arcFiles.put(RESOURCES_ABBR +res.getXnatAbstractresourceId(), stats);
                    } catch (RuntimeException e) {
                        logger.error("",e);
                        arcFiles.put(RESOURCES_ABBR +res.getXnatAbstractresourceId(),"Error");
                    }
                }

                int count = 0;
                long size = 0;
                try {
                    ArrayList fileGroup = (ArrayList)getFileGroups().get("misc");
                    if (fileGroup!=null && !fileGroup.isEmpty()){
                            Iterator iter = fileGroup.iterator();
                            while(iter.hasNext())
                            {
                                String fID = (String)iter.next();
                                size += this.getFileTracker().getSize(this.getFileTracker().getIDIndex(fID));
                            }
                            count = fileGroup.size();

                        NumberFormat formatter = NumberFormat.getInstance();
                        formatter.setMinimumFractionDigits(2);
                        formatter.setMaximumFractionDigits(2);

                        String stats= count
                                + " files, "
                                + formatter.format(((float) ((float)size / 1048576))) + "Mb";
                        arcFiles.put("misc", stats);
                    }
                } catch (RuntimeException e) {
                    logger.error("",e);
                    arcFiles.put("misc","Error");
                }

                logger.debug("END LOAD ARC FILES");
            }
        }
        return arcFiles;
    }

    public String getArchiveStats(String scanId) {
        try {
                String s = (String)this.getArcFiles().get(scanId);
                if (s == null) {
                    return "0 files, 0.00Mb";
                } else {
                    return s;
                }
        } catch (Exception e) {
            return "0 files, 0.00Mb";
        }
    }

    public long getDateDiff(Date d) {
        try {
            Date expt = (Date) this.getDate();

            Calendar dobC = new GregorianCalendar();
            dobC.setTime(expt);
            Calendar acqC = new GregorianCalendar();
            acqC.setTime(d);
            long days = getDateDiff(dobC, acqC);
            return Math.round(Math.floor(days));
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * @param type
     * @return ArrayList of org.nrg.xdat.om.XnatMrscandata
     */
    public ArrayList<XnatImagescandata> getScansByXSIType(String type) {
        ArrayList<XnatImagescandata> _return = new ArrayList<XnatImagescandata>();

        for(XnatImagescandataI scan :  this.getScans_scan()){
        	try {
				if(((XnatImagescandata)scan).getItem().instanceOf(type)){
					_return.add((XnatImagescandata)scan);
				}
			} catch (ElementNotFoundException e) {
				e.printStackTrace();
			}
        }
        _return.trimToSize();
        return _return;
    }

    /**
     * @param type
     * @return ArrayList of org.nrg.xdat.om.XnatMrscandata
     */
    public ArrayList<XnatImagescandata> getScansByType(String type) {
        ArrayList<XnatImagescandata> _return = new ArrayList<XnatImagescandata>();

        for(XnatImagescandataI scan :  this.getScans_scan()){
            String scan_type = scan.getType();
            if (scan_type ==null)
            {
                if (type==null)
                {
                    _return.add((XnatImagescandata)scan);
                }
            }else{
                if (scan_type.equalsIgnoreCase(type)) {
                    _return.add((XnatImagescandata)scan);
                }
            }
        }
        _return.trimToSize();
        return _return;
    }

    /**
     * @param type
     * @return ArrayList of org.nrg.xdat.om.XnatMrscandata
     */
    public XnatImagescandata getScanById(String id) {
        for(XnatImagescandataI scan : getScans_scan()){
                if (scan.getId().equalsIgnoreCase(id)) {
                    return (XnatImagescandata)scan;
                }
            
        }
        return null;
    }

    /**
     * @param type
     * @return ArrayList of org.nrg.xdat.om.XnatMrscandata
     */
    public XnatImageassessordata getAssessorById(String id) {
        for(XnatImageassessordataI scan : getAssessors()){
                if (scan.getId().equalsIgnoreCase(id)) {
                    return (XnatImageassessordata) scan;
                }
            
        }
        return null;
    }

    /**
     * @param type
     * @return ArrayList of org.nrg.xdat.om.XnatReconstructedimagedata
     */
    public List<XnatReconstructedimagedata> getReconstructionsByType(String type) {
        ArrayList _return = new ArrayList();
        List al = this.getReconstructions_reconstructedimage();
        if (al != null) {
            for (int i = 0; i < al.size(); i++) {
                XnatReconstructedimagedata scan = (XnatReconstructedimagedata) al.get(i);
                 try {
	                if (scan.getBasescantype().equalsIgnoreCase(type)) {
	                    _return.add(scan);
	                }
                }catch(NullPointerException npe) {
	                if (scan.getType().equalsIgnoreCase(type)) {
	                    _return.add(scan);
	                }
                }
            }
        }
        _return.trimToSize();
        return _return;
    }

    /**
     * @param type
     * @return org.nrg.xdat.om.XnatReconstructedimagedata
     */
    public XnatReconstructedimagedataI getReconstructionByID(String type) {
        List al = this.getReconstructions_reconstructedimage();
        if (al != null) {
            for (int i = 0; i < al.size(); i++) {
                XnatReconstructedimagedata scan = (XnatReconstructedimagedata) al.get(i);
                if (scan.getId().equalsIgnoreCase(type)) {
                    return scan;
                }
            }
        }
        return null;
    }

    /**
     * @return Returns the prearchivePath.
     */
    public String getPrearchivepath(XDATUser user) {
        String s = super.getPrearchivepath();
        if (s == null || s.equalsIgnoreCase("")) {
            s = user.getQuarantinePath();
        }
        return s;
    }

    public Collection<XnatImagescandataI> getSortedScans() {
        if (null == scans) {
            try {
            	scans =getScans_scan();
                Collections.sort(scans,BaseXnatImagescandata.GetComparator());
            } catch (Exception e) {
                logger.error("", e);
                return getScans_scan();
            }
        }

        return scans;
    }


    public int getMinimalLoadAssessorsCount(String elementName)
    {
        return getMinimalLoadAssessors(elementName).size();
    }

    public ArrayList getMinimalLoadAssessors(String elementName)
    {
        ArrayList al = new ArrayList();
        try {
            SchemaElement e = SchemaElement.GetElement(elementName);
             Iterator min = this.getMinimalLoadAssessors().iterator();
             while (min.hasNext())
             {
                 ItemI assessor = (ItemI)min.next();
                 if (assessor.getXSIType().equalsIgnoreCase(e.getFullXMLName()))
                 {
                      al.add(assessor);
                 }
             }
             
              
             
        } catch (XFTInitException e) {
            logger.error("",e);
        } catch (ElementNotFoundException e) {
            logger.error("",e);
        }

         al.trimToSize();
         return al;
    }

    public List<XnatImageassessordataI> getMinimalLoadAssessors()
    {
        if (minLoadAssessors==null)
        {
            minLoadAssessors = new ArrayList<XnatImageassessordataI>();
            if(getItem().isPreLoaded())
            {
                minLoadAssessors=this.getAssessors();
            }else{

                try {
                    XFTTable table = TableSearch.Execute("SELECT ex.id,ex.date,ex.project,me.element_name AS type,me.element_name,ex.note AS note,i.lastname, investigator_xnat_investigatorData_id AS invest_id,projects FROM xnat_imageAssessorData assessor LEFT JOIN xnat_experimentData ex ON assessor.ID=ex.ID LEFT JOIN xnat_investigatorData i ON i.xnat_investigatorData_id=ex.investigator_xnat_investigatorData_id LEFT JOIN xdat_meta_element me ON ex.extension=me.xdat_meta_element_id LEFT JOIN (SELECT xs_a_concat(project || ',') AS PROJECTS, sharing_share_xnat_experimentda_id FROM xnat_experimentData_share GROUP BY sharing_share_xnat_experimentda_id) PROJECT_SEARCH ON ex.id=PROJECT_SEARCH.sharing_share_xnat_experimentda_id WHERE assessor.imagesession_id='" + this.getId() +"' ORDER BY ex.date ASC",getDBName(),null);
                    table.resetRowCursor();
                    
                    while (table.hasMoreRows())
                    {
                        Hashtable row = table.nextRowHash();
                        String element = (String)row.get("element_name");
                        try {
                            XFTItem child = XFTItem.NewItem(element,this.getUser());

                            Object date = row.get("date");
                            Object id = row.get("id");
                            Object note = row.get("note");
                            Object invest_id = row.get("invest_id");
                            Object lastname = row.get("lastname");
                            Object project = row.get("project");
                            
                            if (date!=null)
                            {
                                try {
                                    child.setProperty("date",date);
                                } catch (XFTInitException e) {
                                    logger.error("",e);
                                } catch (ElementNotFoundException e) {
                                    logger.error("",e);
                                } catch (FieldNotFoundException e) {
                                    logger.error("",e);
                                } catch (InvalidValueException e) {
                                    logger.error("",e);
                                }
                            }
                            if (id!=null)
                            {
                                try {
                                    child.setProperty("ID",id);
                                } catch (XFTInitException e) {
                                    logger.error("",e);
                                } catch (ElementNotFoundException e) {
                                    logger.error("",e);
                                } catch (FieldNotFoundException e) {
                                    logger.error("",e);
                                } catch (InvalidValueException e) {
                                    logger.error("",e);
                                }
                            }

                            if (project!=null)
                            {
                                try {
                                    child.setProperty("project",project);
                                } catch (XFTInitException e) {
                                    logger.error("",e);
                                } catch (ElementNotFoundException e) {
                                    logger.error("",e);
                                } catch (FieldNotFoundException e) {
                                    logger.error("",e);
                                } catch (InvalidValueException e) {
                                    logger.error("",e);
                                }
                            }

                            if (note!=null)
                            {
                                try {
                                    child.setProperty("note",note);
                                } catch (XFTInitException e) {
                                    logger.error("",e);
                                } catch (ElementNotFoundException e) {
                                    logger.error("",e);
                                } catch (FieldNotFoundException e) {
                                    logger.error("",e);
                                } catch (InvalidValueException e) {
                                    logger.error("",e);
                                }
                            }
                            if (lastname!=null)
                            {
                                try {
                                    child.setProperty("investigator.lastname",lastname);
                                } catch (XFTInitException e) {
                                    logger.error("",e);
                                } catch (ElementNotFoundException e) {
                                    logger.error("",e);
                                } catch (FieldNotFoundException e) {
                                    logger.error("",e);
                                } catch (InvalidValueException e) {
                                    logger.error("",e);
                                }
                            }
                            if (invest_id!=null)
                            {
                                try {
                                    child.setProperty("investigator_xnat_investigatorData_id",invest_id);
                                } catch (XFTInitException e) {
                                    logger.error("",e);
                                } catch (ElementNotFoundException e) {
                                    logger.error("",e);
                                } catch (FieldNotFoundException e) {
                                    logger.error("",e);
                                } catch (InvalidValueException e) {
                                    logger.error("",e);
                                }
                            }
                            
                            String projects = (String)row.get("projects");
                            if (projects!=null)
                            {
                                Iterator iter= StringUtils.CommaDelimitedStringToArrayList(projects, true).iterator();
                                while(iter.hasNext())
                                {
                                   String projectName = (String)iter.next();
                                   child.setProperty("sharing.share.project", projectName);
                                }
                            }

                            XnatImageassessordata assessor= (XnatImageassessordata)BaseElement.GetGeneratedItem(child);
                            
                            if (element.equalsIgnoreCase(XnatQcmanualassessordata.SCHEMA_ELEMENT_NAME))
                            {
                                if (this.getUser().canRead(child))
                                {
                                    this.manQC = new XnatQcmanualassessordata(child.getCurrentDBVersion(false));
                                    minLoadAssessors.add(this.manQC);
                                }else{
                                    minLoadAssessors.add(new XnatQcmanualassessordata(child));
                                }
                            }else if (element.equalsIgnoreCase(XnatQcassessmentdata.SCHEMA_ELEMENT_NAME))
                            {
                                if (this.qc == null)
                                {
                                    if (this.getUser().canRead(child))
                                    {
                                        this.qc = new XnatQcassessmentdata(child.getCurrentDBVersion(false));
                                        minLoadAssessors.add(this.qc);
                                    }else{
                                        minLoadAssessors.add(new XnatQcassessmentdata(child));
                                    }
                                }else{
                                	 this.qc = new XnatQcassessmentdata(child.getCurrentDBVersion(false));
                                     minLoadAssessors.add(this.qc);
                                }
                            }else if(assessor instanceof ScanAssessorI){
                            	minLoadAssessors.add( (XnatImageassessordata)BaseElement.GetGeneratedItem(child.getCurrentDBVersion(false)));
                            }else{
                                minLoadAssessors.add(assessor);
                            }

                        } catch (XFTInitException e) {
                            logger.error("",e);
                        } catch (ElementNotFoundException e) {
                            logger.error("",e);
                        }
                    }
                } catch (Exception e) {
                    logger.error("",e);
                }
            }
        }

        return minLoadAssessors;
    }

    public void loadSRBFiles()
    {
        if (this.fileGroups.size()==0)
        {
            long startTime = System.currentTimeMillis();
                XNATDirectory dir = getSRBDirectory();
                System.out.println("Time to load " + (System.currentTimeMillis()-startTime) + "ms");
                startTime = System.currentTimeMillis();

                for(XnatImagescandataI scan : this.getSortedScans()){
                    XNATMetaData meta = new XNATMetaData();
                    meta.setCategory("SCAN"); //match operator is = by default
                    meta.setExternalId(scan.getId());
                    String parsedScanID= StringUtils.ReplaceStr(StringUtils.ReplaceStr(scan.getId(),"-",""),"*","AST");
                    fileGroups.put(SCAN_ABBR +parsedScanID,dir.filterLocal(meta));
                }

                Iterator reconIter= this.getReconstructions_reconstructedimage().iterator();
                while (reconIter.hasNext())
                {
                    XnatReconstructedimagedata recon = (XnatReconstructedimagedata)reconIter.next();

                        XNATMetaData meta = new XNATMetaData();
                        meta.setCategory("RECON"); //match operator is = by default
                        meta.setExternalId(recon.getId());
                        String parsedScanID= StringUtils.ReplaceStr(StringUtils.ReplaceStr(recon.getId(),"-",""),"*","AST");
                        fileGroups.put(RECON_ABBR +parsedScanID,dir.filterLocal(meta));

                }

                for(XnatImageassessordataI assess : this.getAssessors_assessor()){

                        XNATMetaData meta = new XNATMetaData();
                        meta.setCategory("ASSESSOR"); //match operator is = by default
                        meta.setExternalId(assess.getId());
                        String parsedScanID= StringUtils.ReplaceStr(StringUtils.ReplaceStr(assess.getId(),"-",""),"*","AST");
                        fileGroups.put("assess" +parsedScanID,dir.filterLocal(meta));

                }

                XNATMetaData meta = new XNATMetaData();
                meta.setCategory("MISC"); //match operator is = by default
                fileGroups.put("misc0",dir.filterLocal(meta));
                startTime = System.currentTimeMillis();
                System.out.println("Time to sort " + (System.currentTimeMillis()-startTime) + "ms");
        }
    }

    private void loadDefinedFiles(String rootPath)
    {
        if (!rootPath.startsWith("srb:"))
        {
        	rootPath =FileUtils.AppendSlash(rootPath);
        }
        for(XnatImagescandataI scan : this.getSortedScans()){
            final ArrayList fileGrouping = new ArrayList();
            if (!scan.getFile().isEmpty())
            {
                for (XnatAbstractresourceI xnatFile:scan.getFile())
                {
                    for (File f:((XnatAbstractresource)xnatFile).getCorrespondingFiles(rootPath))
                    {
                        if (f.exists())
                        {
                            String fileID = this._files.addFile(f.getPath(),f,FileTracker.KNOWN);
                            fileGrouping.add(fileID);
                        }
                    }

                    if (xnatFile instanceof XnatResourcecatalog){
                        File f = ((XnatResourcecatalog)xnatFile).getCatalogFile(rootPath);
                        if (f.exists())
                        {
                            String fileID = this._files.addFile(f.getPath(),f,FileTracker.KNOWN);
                            fileGrouping.add(fileID);
                        }
                    }
                }
                if (fileGrouping.size()>0){
                    String parsedScanID= StringUtils.ReplaceStr(StringUtils.ReplaceStr(scan.getId(),"-",""),"*","AST");
                    fileGroups.put(SCAN_ABBR + parsedScanID,fileGrouping);
                }
            }
        }

        Iterator reconIter= this.getReconstructions_reconstructedimage().iterator();
        while (reconIter.hasNext())
        {
            XnatReconstructedimagedata recon = (XnatReconstructedimagedata)reconIter.next();
            ArrayList fileGrouping = new ArrayList();
            List outFiles = recon.getOut_file();
            if (outFiles.size()>0)
            {
                Iterator files =outFiles.iterator();
                while (files.hasNext())
                {
                    XnatAbstractresource xnatFile = (XnatAbstractresource) files.next();
                    ArrayList jFiles = xnatFile.getCorrespondingFiles(rootPath);

                    Iterator iter = jFiles.iterator();
                    while (iter.hasNext())
                    {
                        File f = (File)iter.next();
                        if (f.exists())
                        {
                            String fileID = this._files.addFile(f.getPath(),f,FileTracker.KNOWN);
                            fileGrouping.add(fileID);
                        }

                    }

                    if (xnatFile instanceof XnatResourcecatalog){
                        File f = ((XnatResourcecatalog)xnatFile).getCatalogFile(rootPath);
                        if (f.exists())
                        {
                            String fileID = this._files.addFile(f.getPath(),f,FileTracker.KNOWN);
                            fileGrouping.add(fileID);
                        }
                    }
                }

                if (fileGrouping.size()>0){
                    if (recon.getId()!=null)
                    {
                        String parsedScanID= StringUtils.ReplaceStr(StringUtils.ReplaceStr(recon.getId(),"-",""),"*","AST");
                        fileGroups.put(RECON_ABBR + parsedScanID,fileGrouping);
                    }
                }

            }
        }

        for(XnatImageassessordataI assess : this.getAssessors_assessor()){
            ArrayList fileGrouping = new ArrayList();
            List outFiles = assess.getOut_file();
            if (outFiles.size()>0)
            {
                Iterator files =outFiles.iterator();
                while (files.hasNext())
                {
                    XnatAbstractresource xnatFile = (XnatAbstractresource) files.next();
                    ArrayList jFiles = xnatFile.getCorrespondingFiles(rootPath);

                    Iterator iter = jFiles.iterator();
                    while (iter.hasNext())
                    {
                        File f = (File)iter.next();
                        if (f.exists())
                        {
                            String fileID = this._files.addFile(f.getPath(),f,FileTracker.KNOWN);
                            fileGrouping.add(fileID);
                        }
                    }

                    if (xnatFile instanceof XnatResourcecatalog){
                        File f = ((XnatResourcecatalog)xnatFile).getCatalogFile(rootPath);
                        if (f.exists())
                        {
                            String fileID = this._files.addFile(f.getPath(),f,FileTracker.KNOWN);
                            fileGrouping.add(fileID);
                        }
                    }
                }
                if (fileGrouping.size()>0){
                    String parsedScanID= StringUtils.ReplaceStr(StringUtils.ReplaceStr(assess.getId(),"-",""),"*","AST");
                    fileGroups.put(ASSESSOR_ABBR + parsedScanID,fileGrouping);
                }
            }
        }

        Iterator resourceIter= this.getResources_resource().iterator();
        while (resourceIter.hasNext())
        {
            XnatAbstractresource xnatFile = (XnatAbstractresource)resourceIter.next();
            ArrayList fileGrouping = new ArrayList();
            ArrayList jFiles = xnatFile.getCorrespondingFiles(rootPath);

            Iterator iter = jFiles.iterator();
            while (iter.hasNext())
            {
                File f = (File)iter.next();
                if (f.exists())
                {
                    String fileID = this._files.addFile(f.getPath(),f,FileTracker.KNOWN);
                    fileGrouping.add(fileID);
                }
            }

            if (xnatFile instanceof XnatResourcecatalog){
                File f = ((XnatResourcecatalog)xnatFile).getCatalogFile(rootPath);
                if (f.exists())
                {
                    String fileID = this._files.addFile(f.getPath(),f,FileTracker.KNOWN);
                    fileGrouping.add(fileID);
                }
            }

            if (fileGrouping.size()>0){
                String parsedScanID= xnatFile.getXnatAbstractresourceId().toString();
                fileGroups.put(RESOURCES_ABBR + parsedScanID,fileGrouping);
            }
        }
    }

    /**
     */
    public void loadLocalFiles()
    {
        loadLocalFiles(true);
    }

    /**
     */
    public void loadLocalFiles(boolean loadMISCFiles)
    {
        if (_files.getSize()==0)
        {
        	String s;
			try {
				s = this.getArchiveRootPath();
			} catch (UnknownPrimaryProjectException e) {
				s=null;
			}
        	
            loadDefinedFiles(s);

            if (loadMISCFiles)
            {
                String rawdir = this.deriveSessionDir();
                if (rawdir!=null){
                    File misc = new File(rawdir);
                    if (misc.exists())
                        loadDirectoryFiles(misc,"",false,new ArrayList());
                }
            }


            _files.syncToFS() ;
        }
    }

    /**
     */
    public void loadLocalRAWFiles()
    {
        if (_files.getSize()==0)
        {
        	String s;
			try {
				s = this.getArchiveRootPath();
			} catch (UnknownPrimaryProjectException e) {
				s=null;
			}
            loadDefinedFiles(s);

            String rawdir = this.deriveRawDir();
            if (rawdir!=null){
                File misc = new File(rawdir);
                if (misc.exists())
                    loadDirectoryFiles(misc,"",false,new ArrayList());
            }


            _files.syncToFS() ;
        }
    }

    public void loadDirectoryFiles(File dir,String parents,boolean raw,ArrayList miscRaw)
    {
        if (dir.exists())
        {
            parents += dir.getName() + "/";

            if (!raw){
                if (dir.getName().equalsIgnoreCase("RAW") || dir.getName().equalsIgnoreCase("SCANS")){
                    raw = true;
                }
            }
            File[] children = dir.listFiles();
            for (int i=0;i<children.length;i++)
            {
                File child = children[i];
                if (child.isDirectory())
                {
                    loadDirectoryFiles(child,parents,raw,miscRaw);
                }else{
                    if (_files.indexOf(child)==-1)
                    {
                        if (!child.getName().equalsIgnoreCase("dcmtoxnat.log") && !child.getName().equalsIgnoreCase("dcmtoxnat.log.gz"))
                        {
                            String fileID = this._files.addFile(parents + child.getName(),child,FileTracker.MISC);
                            if (raw){
                                miscRaw.add(fileID);
                            }
                        }
                    }
                }
            }

            if (dir.getName().equalsIgnoreCase("RAW") || dir.getName().equalsIgnoreCase("SCANS")){
                fileGroups.put("misc", miscRaw);
            }

        }
    }



    public String listArchiveToHTML(String server)
    {
        String rootPath;
		try {
			rootPath = this.getArchiveRootPath();
		} catch (UnknownPrimaryProjectException e2) {
			rootPath=null;
		}
        String miscDir = null;
        File achive = new File(rootPath);
        StringBuffer sb = new StringBuffer();
        StringBuffer allFiles = new StringBuffer();
        int fileCount=0;

        allFiles.append("<script  type=\"text/javascript\" language=\"JavaScript1.3\">\n");
        allFiles.append("   function allFilesCheckAll(checkAll)\n");
        allFiles.append("   {\n");
        allFiles.append("       var change=null;\n");
        allFiles.append("       var node=null;\n");
        
        sb.append("  <TR>");
        sb.append("    <TD VALIGN=\"top\" ALIGN=\"left\"><b>SCANS</b></TD><TD>");
        for(XnatImagescandataI scan : this.getSortedScans()){
            ArrayList fileGrouping = new ArrayList();
            List scanFiles= scan.getFile();
            if (scanFiles.size()>0)
            {
                Iterator files = scanFiles.iterator();
                boolean hasContent= false;
                boolean hasFunctionText=false;
                StringBuffer scanLinkBuffer = new StringBuffer();
                while (files.hasNext())
                {
                    XnatAbstractresource xnatFile = (XnatAbstractresource) files.next();
                    ArrayList jFiles = xnatFile.getCorrespondingFiles(rootPath);
                    if (miscDir==null){
                        miscDir=xnatFile.getFullPath(rootPath);
                        int mrIndex = miscDir.toLowerCase().indexOf("/" + getArchiveDirectoryName().toLowerCase() +"/");
                        if (mrIndex==-1){
                            String sPath = rootPath.replace('\\', '/');
                            sPath = sPath.replace("//", "/");
                            if (miscDir.startsWith(sPath)){
                                int index =miscDir.indexOf(File.separator,sPath.length()+1);
                                if (index==-1){
                                    index =miscDir.indexOf("/",sPath.length()+1);
                                    if (index==-1){
                                        index =miscDir.indexOf("\\",sPath.length()+1);
                                    }
                                    if (index==-1){
                                        index =miscDir.indexOf("\\",sPath.length()+1);
                                    }else{
                                        miscDir = null;
                                    }
                                }else{
                                    miscDir = miscDir.substring(0,index);
                                }
                            }else{
                                int index = miscDir.indexOf(achive.getName());
                                if (index==-1){
                                    miscDir=null;
                                }else{
                                    index += achive.getName().length()+1;
                                    miscDir = miscDir.substring(0,index);
                                }
                            }
                        }else{
                            mrIndex += getArchiveDirectoryName().length()+2;
                            miscDir = miscDir.substring(0,mrIndex);
                        }
                    }

                    Iterator iter = jFiles.iterator();
                    while (iter.hasNext())
                    {
                        File f = (File)iter.next();
                        if (f.exists())
                        {
                            String fileID = this._files.addFile(f.getAbsolutePath(),f,FileTracker.KNOWN);
                            scanLinkBuffer.append("<b>").append(f.getName()).append("</b><BR>");
                            fileGrouping.add(fileID);
                            hasContent=true;
                            hasFunctionText=true;
                            fileCount++;
                        }else{
                            scanLinkBuffer.append("\n").append(f.getName()).append("&nbsp;(File off-line)<BR>");
                            hasContent=true;
                        }
                    }

                    if (xnatFile instanceof XnatResourcecatalog){
                        File f = ((XnatResourcecatalog)xnatFile).getCatalogFile(rootPath);
                        if (f.exists())
                        {
                            String fileID = this._files.addFile(f.getAbsolutePath(),f,FileTracker.KNOWN);
                            scanLinkBuffer.append("<b>").append(f.getName()).append("</b><BR>");
                            fileGrouping.add(fileID);
                            hasContent=true;
                            hasFunctionText=true;
                            fileCount++;
                        }else{
                            scanLinkBuffer.append("\n").append(f.getName()).append("&nbsp;(File off-line)<BR>");
                            hasContent=true;
                        }
                    }
                }
                String parsedScanID= StringUtils.ReplaceStr(StringUtils.ReplaceStr(scan.getId(),"-",""),"*","AST");
                if (hasContent){
                    sb.append("\n");
                    if (hasFunctionText)
                    {
                        sb.append("<INPUT type=\"checkbox\" id=\"scan").append(parsedScanID).append("\" name=\"scan").append(parsedScanID).append("\" VALUE=\"CHECKED\" CHECKED/>&nbsp;");
                        this.fileGroups.put("scan" +parsedScanID,fileGrouping);
                    }
                    sb.append("<span class=\"trigger\" onClick=\"blocking('scan").append(parsedScanID).append("');\">");
                    sb.append("<img ID=\"IMGscan").append(parsedScanID).append("\" src=\"").append(server).append("/images/plus.jpg\" border=0/>&nbsp;");
                    sb.append("<b>").append(scan.getId()).append("</b>&nbsp;");
                    if (scan.getType()!=null)
                    {
                        sb.append("(" + scan.getType() + ")");
                    }
                    sb.append("</span>");
                    sb.append("<BR><span class=\"branch\" ID=\"spanscan").append(parsedScanID).append("\">");
                    sb.append(scanLinkBuffer);
                    sb.append("</span><BR>");

                    if (hasFunctionText){
                        allFiles.append("       \n");
                        allFiles.append("       change = \"scan").append(parsedScanID).append("\";\n");
                        allFiles.append("       node = document.getElementById(change);\n");
                        allFiles.append("       node.checked=checkAll.checked;\n");
                    }
                }else{
                    sb.append("<span class=\"trigger\"><b>"+ scan.getId() + "</b>&nbsp;(" + scan.getType() + ")</span><br><span class=\"branch\" style=\"display: block;\">No files found for this scan.</span><BR>");
                }
            }else{
                sb.append("<span class=\"trigger\"><b>"+ scan.getId() + "</b>&nbsp;(" + scan.getType() + ")</span><br><span class=\"branch\" style=\"display: block;\">No files defined for this scan.</span><br>");
            }
        }
        sb.append("</TD></TR>");

        sb.append("<TR><TD>&nbsp;</TD></TR>");

        sb.append("  <TR>");
        int c=0;
        sb.append("    <TD VALIGN=\"top\" ALIGN=\"left\">");
        Iterator reconIter= this.getReconstructions_reconstructedimage().iterator();
        while (reconIter.hasNext())
        {
            XnatReconstructedimagedata recon = (XnatReconstructedimagedata)reconIter.next();
            ArrayList fileGrouping = new ArrayList();
            List outFiles = recon.getOut_file();
            if (outFiles.size()>0)
            {
                Iterator files =outFiles.iterator();
            boolean hasContent= false;
            boolean hasFunctionText=false;
            StringBuffer scanLinkBuffer = new StringBuffer();
            while (files.hasNext())
            {
                XnatAbstractresource xnatFile = (XnatAbstractresource) files.next();
                ArrayList jFiles = xnatFile.getCorrespondingFiles(rootPath);

                Iterator iter = jFiles.iterator();
                while (iter.hasNext())
                {
                    File f = (File)iter.next();
                    if (f.exists())
                    {
                        String fileID = this._files.addFile(f.getAbsolutePath(),f,FileTracker.KNOWN);
                        scanLinkBuffer.append("<b>").append(f.getName()).append("</b><BR>");
                        fileGrouping.add(fileID);
                        hasContent=true;
                        hasFunctionText=true;
                        fileCount++;
                    }else{
                        scanLinkBuffer.append("\n").append(f.getName()).append("&nbsp;(File off-line)<BR>");
                        hasContent=true;
                    }
                }

                if (xnatFile instanceof XnatResourcecatalog){
                    File f = ((XnatResourcecatalog)xnatFile).getCatalogFile(rootPath);
                    if (f.exists())
                    {
                        String fileID = this._files.addFile(f.getAbsolutePath(),f,FileTracker.KNOWN);
                        scanLinkBuffer.append("<b>").append(f.getName()).append("</b><BR>");
                        fileGrouping.add(fileID);
                        hasContent=true;
                        hasFunctionText=true;
                        fileCount++;
                    }else{
                        scanLinkBuffer.append("\n").append(f.getName()).append("&nbsp;(File off-line)<BR>");
                        hasContent=true;
                    }
                }
            }
            String parsedScanID= StringUtils.ReplaceStr(StringUtils.ReplaceStr(recon.getId(),"-",""),"*","AST");
            if (hasContent){
            	if(c++==0)
                    sb.append("<b>RECONSTRUCTIONS</b></TD><TD>");
                sb.append("\n");
                if (hasFunctionText){
                    sb.append("<INPUT type=\"checkbox\" id=\"recon").append(parsedScanID).append("\" name=\"recon").append(parsedScanID).append("\" CHECKED/>&nbsp;");
                    this.fileGroups.put("recon" +parsedScanID,fileGrouping);
                }
                sb.append("<span class=\"trigger\" onClick=\"blocking('recon").append(parsedScanID).append("');\">");
                sb.append("<img ID=\"IMGrecon").append(parsedScanID).append("\" src=\"").append(server).append("/images/plus.jpg\" border=0/>&nbsp;<b>").append(recon.getId()).append("</b>&nbsp;");
                if (recon.getType()!=null)
                    sb.append("(" + recon.getType() + ")");
                sb.append("</span>");
                sb.append("<BR><span class=\"branch\" ID=\"spanrecon").append(parsedScanID).append("\">");
                sb.append(scanLinkBuffer);

                sb.append("</span><BR>");

                if (hasFunctionText){
                    allFiles.append("       \n");
                    allFiles.append("       change = \"recon").append(parsedScanID).append("\";\n");
                    allFiles.append("       node = document.getElementById(change);\n");
                    allFiles.append("       node.checked=checkAll.checked;\n");
                }
            }else{
            	if(c++==0)
                    sb.append("<b>RECONSTRUCTIONS</b></TD><TD>");
                sb.append("<span class=\"trigger\"><b>"+ recon.getId() + "</b>&nbsp;(" + recon.getType() + ")</span><br><span class=\"branch\" style=\"display: block;\">No files found for this reconstruction.</span><BR>");
            }
            }else{
            	if(c++==0)
                    sb.append("<b>RECONSTRUCTIONS</b></TD><TD>");
                sb.append("<span class=\"trigger\"><b>"+ recon.getId() + "</b>&nbsp;(" + recon.getType() + ")</span><br><span class=\"branch\" style=\"display: block;\">No files defined for this reconstruction.</span><BR>");
            }
        }
        sb.append("</TD></TR>");

        try {
            sb.append("<TR><TD>&nbsp;</TD></TR>");

                sb.append("  <TR>");
                sb.append("    <TD VALIGN=\"top\" ALIGN=\"left\">");
                c=0;
                for(XnatImageassessordataI assess : this.getAssessors_assessor()){
                    ArrayList fileGrouping = new ArrayList();
                    List outFiles = assess.getOut_file();
                    if (outFiles.size()>0)
                    {
                        Iterator files =outFiles.iterator();

                        boolean hasContent= false;
                        boolean hasFunctionText=false;
                        StringBuffer scanLinkBuffer = new StringBuffer();
                        while (files.hasNext())
                        {
                            XnatAbstractresource xnatFile = (XnatAbstractresource) files.next();
                            ArrayList jFiles = xnatFile.getCorrespondingFiles(rootPath);

                            Iterator iter = jFiles.iterator();
                            while (iter.hasNext())
                            {
                                File f = (File)iter.next();
                                if (f.exists())
                                {
                                    String fileID = this._files.addFile(f.getAbsolutePath(),f,FileTracker.KNOWN);
                                    scanLinkBuffer.append("<b>").append(f.getName()).append("</b><BR>");
                                    fileGrouping.add(fileID);
                                    hasContent=true;
                                    hasFunctionText=true;
                                    fileCount++;
                                }else{
                                    scanLinkBuffer.append("\n").append(f.getName()).append("&nbsp;(File off-line)<BR>");
                                    hasContent=true;
                                }
                            }

                            if (xnatFile instanceof XnatResourcecatalog){
                                File f = ((XnatResourcecatalog)xnatFile).getCatalogFile(rootPath);
                                if (f.exists())
                                {
                                    String fileID = this._files.addFile(f.getAbsolutePath(),f,FileTracker.KNOWN);
                                    scanLinkBuffer.append("<b>").append(f.getName()).append("</b><BR>");
                                    fileGrouping.add(fileID);
                                    hasContent=true;
                                    hasFunctionText=true;
                                    fileCount++;
                                }else{
                                    scanLinkBuffer.append("\n").append(f.getName()).append("&nbsp;(File off-line)<BR>");
                                    hasContent=true;
                                }
                            }
                        }
                        String parsedScanID= StringUtils.ReplaceStr(StringUtils.ReplaceStr(assess.getId(),"-",""),"*","AST");
                        if (hasContent){
                        	if(c++==0)
                                sb.append("<b>ASSESSMENTS</b></TD><TD>");

                            sb.append("\n");
                            if (hasFunctionText){
                                sb.append("<INPUT type=\"checkbox\" id=\"assess").append(parsedScanID).append("\" name=\"assess").append(parsedScanID).append("\" VALUE=\"CHECKED\" CHECKED/>&nbsp;");
                                this.fileGroups.put("assess" +parsedScanID,fileGrouping);
                            }
                            sb.append("<span class=\"trigger\" onClick=\"blocking('assess").append(parsedScanID).append("');\">");
                            sb.append("<img ID=\"IMGassess").append(parsedScanID).append("\" src=\"").append(server).append("/images/plus.jpg\" border=0/>&nbsp;<b>").append(assess.getId()).append("</b>&nbsp;");
                            sb.append("(" + ((XnatImageassessordata)assess).getItem().getProperName() + ")");
                            sb.append("</span>");
                            sb.append("<BR>\n<span class=\"branch\" ID=\"spanassess").append(parsedScanID).append("\">");
                            sb.append(scanLinkBuffer);
                            sb.append("</span><BR>");

                            if (hasFunctionText)
                            {
                                allFiles.append("       \n");
                                allFiles.append("       change = \"assess").append(parsedScanID).append("\";\n");
                                allFiles.append("       node = document.getElementById(change);\n");
                                allFiles.append("       node.checked=checkAll.checked;\n");
                            }
                        }else{
                        	if(c++==0)
                                sb.append("<b>ASSESSMENTS</b></TD><TD>");
                            sb.append("<span class=\"trigger\"><b>"+ assess.getId() + "</b>&nbsp;(" + ((XnatImageassessordata)assess).getItem().getProperName() + ")</span><br><span class=\"branch\" style=\"display: block;\">No files found for this assessment.</span><BR>");
                        }
                    }else{
                    	if(c++==0)
                            sb.append("<b>ASSESSMENTS</b></TD><TD>");
                        sb.append("<span class=\"trigger\"><b>"+ assess.getId() + "</b>&nbsp;(" + ((XnatImageassessordata)assess).getItem().getProperName() + ")</span><br><span class=\"branch\" style=\"display: block;\">No files defined for this assessment.</span><BR>");
                    }
                }
                sb.append("</TD></TR>");
        } catch (ElementNotFoundException e1) {
            logger.error("",e1);
        }

            sb.append("<TR><TD>&nbsp;</TD></TR>");

                sb.append("  <TR>");
                sb.append("    <TD VALIGN=\"top\" ALIGN=\"left\">");
                c=0;
                Iterator uploadsIter= this.getResources_resource().iterator();
                while (uploadsIter.hasNext())
                {
                    XnatAbstractresource xnatFile = (XnatAbstractresource)uploadsIter.next();
                    ArrayList fileGrouping = new ArrayList();
                    ArrayList jFiles = xnatFile.getCorrespondingFiles(rootPath);

                    boolean hasContent= false;
                    boolean hasFunctionText=false;
                    StringBuffer scanLinkBuffer = new StringBuffer();

                    Iterator iter = jFiles.iterator();
                    while (iter.hasNext())
                    {
                        File f = (File)iter.next();
                        if (f.exists())
                        {
                            String fileID = this._files.addFile(f.getAbsolutePath(),f,FileTracker.KNOWN);
                            scanLinkBuffer.append("<b>").append(f.getName()).append("</b><BR>");
                            fileGrouping.add(fileID);
                            hasContent=true;
                            hasFunctionText=true;
                            fileCount++;
                        }else{
                            scanLinkBuffer.append("\n").append(f.getName()).append("&nbsp;(File off-line)<BR>");
                            hasContent=true;
                        }
                    }

                    if (xnatFile instanceof XnatResourcecatalog){
                        File f = ((XnatResourcecatalog)xnatFile).getCatalogFile(rootPath);
                        if (f.exists())
                        {
                            String fileID = this._files.addFile(f.getAbsolutePath(),f,FileTracker.KNOWN);
                            scanLinkBuffer.append("<b>").append(f.getName()).append("</b><BR>");
                            fileGrouping.add(fileID);
                            hasContent=true;
                            hasFunctionText=true;
                            fileCount++;
                        }else{
                            scanLinkBuffer.append("\n").append(f.getName()).append("&nbsp;(File off-line)<BR>");
                            hasContent=true;
                        }
                    }

                    String label = xnatFile.getLabel();
                    if (label==null){
                        label = xnatFile.getXnatAbstractresourceId().toString();
                    }
                    if(xnatFile instanceof XnatResourcecatalog){
                        if (((XnatResourcecatalog)xnatFile).getTags_tag().size()>0){
                            int counter =0;
                            label +="&nbsp;&nbsp;Tags: ";
                            for(XnatAbstractresourceTagI tag : xnatFile.getTags_tag()){
                                if (counter++>0)label+=", ";
                                label+=tag.getTag();
                            }
                        }
                    }

                    String parsedScanID= xnatFile.getXnatAbstractresourceId().toString();
                    if (hasContent){
                    	if(c++==0)
                            sb.append("<b>ADDITIONAL RESOURCES</b></TD><TD>");

                        sb.append("\n");
                        if (hasFunctionText){
                            sb.append("<INPUT type=\"checkbox\" id=\"uploads").append(parsedScanID).append("\" name=\"uploads").append(parsedScanID).append("\" VALUE=\"CHECKED\" CHECKED/>&nbsp;");
                            this.fileGroups.put("uploads" +parsedScanID,fileGrouping);
                        }
                        sb.append("<span class=\"trigger\" onClick=\"blocking('uploads").append(parsedScanID).append("');\">");
                        sb.append("<img ID=\"IMGuploads").append(parsedScanID).append("\" src=\"").append(server).append("/images/plus.jpg\" border=0/>&nbsp;<b>").append(label).append("</b>&nbsp;");
                        sb.append("");
                        sb.append("</span>");
                        sb.append("<BR>\n<span class=\"branch\" ID=\"spanuploads").append(parsedScanID).append("\">");
                        sb.append(scanLinkBuffer);
                        sb.append("</span><BR>");

                        if (hasFunctionText)
                        {
                            allFiles.append("       \n");
                            allFiles.append("       change = \"uploads").append(parsedScanID).append("\";\n");
                            allFiles.append("       node = document.getElementById(change);\n");
                            allFiles.append("       node.checked=checkAll.checked;\n");
                        }
                    }else{
                    	if(c++==0)
                            sb.append("<b>ADDITIONAL RESOURCES</b></TD><TD>");
                        sb.append("<span class=\"trigger\"><b>"+ label + "</b>&nbsp;</span><br><span class=\"branch\" style=\"display: block;\">No files found for this upload.</span><BR>");
                    }
                }
                sb.append("</TD></TR>");

        if (miscDir!=null){
            File misc = new File(miscDir);
            if(misc.exists()){
                sb.append("<TR><TD>&nbsp;</TD></TR>");

                sb.append("  <TR>");
                sb.append("    <TD VALIGN=\"top\" ALIGN=\"left\"><b>MISC FILES</b></TD><TD>");
                String dirListing = listDirectoryToHTML(misc,server,"",0).toString();
                sb.append(dirListing);
                sb.append("</TD></TR>");
                sb.append("</TABLE>");

                if (dirListing.length()>0){
                    fileCount++;
                    allFiles.append("       \n");
                    allFiles.append("       f").append(misc.getName() + "0").append("CheckAll(checkAll);\n");
                    allFiles.append("       change = \"dir_").append(misc.getName() + "/").append("\";\n");
                    allFiles.append("       node = document.getElementById(change);\n");
                    allFiles.append("       node.checked=checkAll.checked;\n");
                }
            }
        }


        allFiles.append("   }");
        allFiles.append("   </script>");

        sb.insert(0,"    </TH></TR>");
        sb.insert(0,"    <TD></TD><TH VALIGN=\"top\" ALIGN=\"left\">");

        if (fileCount>0)
        {
            sb.insert(0,"<INPUT type=\"checkbox\" name=\"all_files\" VALUE=\"CHECKED\" ONCLICK=\"allFilesCheckAll(this)\" CHECKED/>&nbsp;Select All");
        }
        sb.insert(0,"  <TR>");
        sb.insert(0,"<TABLE>");
        return allFiles.toString() + "<BR>" + sb.toString();
    }

    /**
     * @return Returns the fileGroups.
     */
    public Hashtable getFileGroups() {
        return fileGroups;
    }

    public StringBuffer listDirectoryToHTML(File dir,String server,String parents,int count)
    {
        StringBuffer sb = new StringBuffer();
        StringBuffer function = new StringBuffer();

        int fileCount=0;

        if (dir.exists())
        {
            int local_count=count;
            parents += dir.getName() + "/";
            function.append("<script  type=\"text/javascript\" language=\"JavaScript1.3\">\n");
            function.append("   function f").append(dir.getName() + local_count).append("CheckAll(checkAll)\n");
            function.append("   {\n");
            function.append("       var change=null;\n");
            function.append("       var node=null;\n");

            sb.append("<INPUT type=\"checkbox\" id=\"dir_").append(parents).append("\" name=\"dir_").append(parents).append("\" VALUE=\"CHECKED\" ONCLICK=\"f").append(dir.getName()).append(local_count + "CheckAll(this)\" CHECKED/>");
            sb.append("&nbsp;<span class=\"trigger\" onClick=\"blocking('").append(dir.getName() + local_count).append("');\">");
            sb.append("<img ID=\"IMG").append(dir.getName() + local_count).append("\" src=\"").append(server).append("/images/plus.jpg\" border=0/>&nbsp;<b>").append(dir.getName()).append("</b></span>");
            sb.append("<span class=\"branch\" ID=\"span").append(dir.getName() + local_count).append("\">");
            File[] children = dir.listFiles();
            for (int i=0;i<children.length;i++)
            {
                File child = children[i];
                if (child.isDirectory())
                {
                    count++;
                    StringBuffer functionTemp = new StringBuffer();
                    functionTemp.append("       \n");
                    functionTemp.append("       f").append(child.getName() + count).append("CheckAll(checkAll);\n");
                    functionTemp.append("       change = \"dir_").append(parents + child.getName()+ "/").append("\";\n");
                    functionTemp.append("       node = document.getElementById(change);\n");
                    functionTemp.append("       node.checked=checkAll.checked;\n");
                    StringBuffer temp =listDirectoryToHTML(child,server,parents,count);
                    if (temp.length()>0){
                        sb.append("\n").append(temp).append("<BR>");
                        function.append(functionTemp);
                        fileCount++;
                    }
                }else{
                    if (_files.indexOf(child)==-1)
                    {
                        if (!child.getName().equalsIgnoreCase("dcmtoxnat.log") && !child.getName().equalsIgnoreCase("dcmtoxnat.log.gz"))
                        {
                            String fileID = this._files.addFile(parents + child.getName(),child,FileTracker.MISC);
                            function.append("       \n");
                            function.append("       change = \"dir_file_").append(fileID).append("\";\n");
                            function.append("       node = document.getElementById(change);\n");
                            function.append("       node.checked=checkAll.checked;\n");
                            sb.append("<INPUT type=\"checkbox\" id=\"dir_file_").append(fileID).append("\" name=\"dir_file_").append(fileID).append("\" VALUE=\"CHECKED\" CHECKED/>&nbsp;").append(child.getName()).append("<BR>");
                            fileCount++;
                        }
                    }
                }
            }
            function.append("   }");
            function.append("   </script>");
            sb.append("</span>");

        }

        if (fileCount>0){
            function.append(sb);
            return function;
        }else{
            return new StringBuffer();
        }
    }

    public ArrayList getAllFilePaths()
    {
        return this._files.getAbsolutePaths();
    }

    /**
     * @return Returns the _files.
     */
    public FileTracker getFileTracker() {
        return _files;
    }


    /**
     * @return Returns the lowerCaseSessionId.
     */
    public String getLowerCaseSessionId() {
        if (lowerCaseSessionId==null)
            lowerCaseSessionId = getId().toLowerCase();
        return lowerCaseSessionId;
    }

    XNATDirectory srbDIR = null;
    public XNATDirectory getSRBDirectory()
    {
        if (srbDIR==null)
        {
            String sessionDIR = deriveSessionDir();

            int index = sessionDIR.indexOf("/home/");
            sessionDIR= sessionDIR.substring(index);

            //LOAD ALL RAW IMAGES
            srbDIR = XNATSrbSearch.getFilteredFiles(sessionDIR,null);
        }

        return srbDIR;
    }

    public boolean hasSRBData(){
        String rootPath;
		try {
			rootPath = getArchiveRootPath();
		} catch (UnknownPrimaryProjectException e) {
			rootPath=null;
		}
        for(XnatImagescandataI scan : this.getSortedScans()){
            List scanFiles= scan.getFile();
            Iterator files = scanFiles.iterator();
            while (files.hasNext())
            {
                XnatAbstractresource xnatFile = (XnatAbstractresource) files.next();
                if (xnatFile instanceof org.nrg.xdat.om.XnatResource){
                    XnatResource resource = (XnatResource)xnatFile;
                    String uri =resource.getFullPath(rootPath);
                    if (uri.startsWith("srb:"))
                        return true;
                }else if(xnatFile instanceof org.nrg.xdat.om.XnatDicomseries){
                    XnatDicomseries resource = (XnatDicomseries)xnatFile;
                    String uri =resource.getFullPath(rootPath);
                    if (uri.startsWith("srb:"))
                        return true;
                }else if(xnatFile instanceof org.nrg.xdat.om.XnatResourceseries){
                    XnatResourceseries resource = (XnatResourceseries)xnatFile;
                    String uri =resource.getFullPath(rootPath);
                    if (uri.startsWith("srb:"))
                        return true;
                }
            }
        }

        return false;
    }

    private String raw_dir = null;
    public String deriveRawDir(){
        if (raw_dir==null)
        {
            String rootPath;
			try {
				rootPath = getArchiveRootPath();
			} catch (UnknownPrimaryProjectException e) {
				rootPath=null;
			}
            String last_dir = null;
            for(XnatImagescandataI scan : this.getSortedScans()){
            	for (XnatAbstractresourceI xnatFile:scan.getFile())
                {
                    if (xnatFile instanceof org.nrg.xdat.om.XnatResource){
                        XnatResource resource = (XnatResource)xnatFile;
                        String uri =resource.getFullPath(rootPath);
                        if (last_dir==null){
                            last_dir= uri;
                            int index = last_dir.toUpperCase().indexOf("/RAW/");
                            if (index!=-1){
                                raw_dir = last_dir.substring(0,index+4);
                                return raw_dir;
                            }else{
                            	index = last_dir.toUpperCase().indexOf("/SCANS/");
                                if (index!=-1){
                                    raw_dir = last_dir.substring(0,index+6);
                                    return raw_dir;
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
                                raw_dir = last_dir.substring(0,index+4);
                                return raw_dir;
                            }else{
                            	index = last_dir.toUpperCase().indexOf("/SCANS/");
                                if (index!=-1){
                                    raw_dir = last_dir.substring(0,index+6);
                                    return raw_dir;
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
                                raw_dir = last_dir.substring(0,index+4);
                                return raw_dir;
                            }else{
                            	index = last_dir.toUpperCase().indexOf("/SCANS/");
                                if (index!=-1){
                                    raw_dir = last_dir.substring(0,index+6);
                                    return raw_dir;
                                }
                            }
                        }else{

                        }
                    }
                }
            }

            if (raw_dir ==null)
            {
                raw_dir = deriveSessionDir();
            }
        }


        return raw_dir;
    }

    private String session_dir=null;
    public String deriveSessionDir(){
        if (session_dir==null)
        {
            String rootPath;
			try {
				rootPath = getArchiveRootPath();
			} catch (UnknownPrimaryProjectException e) {
				rootPath=null;
			}
            for(XnatImagescandataI scan : this.getSortedScans()){
            	logger.debug("CHECKING SCAN: "+scan.getId());
                List scanFiles= scan.getFile();
                Iterator files = scanFiles.iterator();
                while (files.hasNext())
                {
                    XnatAbstractresource xnatFile = (XnatAbstractresource) files.next();
                    if (xnatFile instanceof org.nrg.xdat.om.XnatResource){
                        XnatResource resource = (XnatResource)xnatFile;
                        String uri =resource.getFullPath(rootPath);
                        logger.debug("CHECKING RESOURCE: " + uri);
                        String last_dir = null;
                        if (last_dir==null){
                            last_dir= uri;
                            String UPPER_dir=last_dir.toUpperCase();
                            int index = UPPER_dir.indexOf("/" + getArchiveDirectoryName().toUpperCase() + "/");
                            if (index!=-1){
                                session_dir = last_dir.substring(0,index+(2+getArchiveDirectoryName().length()));

                            	logger.debug("MATCHED BY getArchiveDirectoryName():" + session_dir);
                                return session_dir;
                            }else{
                                index = UPPER_dir.indexOf(getArchiveDirectoryName().toUpperCase());
                                if (index!=-1){
                                    int unixSepIndex = last_dir.indexOf("/", index);
                                    int winSepIndex = last_dir.indexOf("\\", index);
                                    if (unixSepIndex ==-1 && winSepIndex==-1)
                                    {
                                        session_dir= last_dir + File.separator;
                                    }else if(unixSepIndex ==-1){
                                        session_dir = last_dir.substring(0,winSepIndex+1);
                                    }else if(winSepIndex ==-1){
                                        session_dir = last_dir.substring(0,unixSepIndex+1);
                                    }else if(winSepIndex < unixSepIndex){
                                        session_dir = last_dir.substring(0,winSepIndex+1);
                                    }else if(winSepIndex > unixSepIndex){
                                        session_dir = last_dir.substring(0,unixSepIndex+1);
                                    }
                                }else{
                                	logger.debug("MOT MATCHED");
                                }
                            }
                        }
                    }else if(xnatFile instanceof org.nrg.xdat.om.XnatDicomseries){
                        XnatDicomseries resource = (XnatDicomseries)xnatFile;
                        String uri =resource.getFullPath(rootPath);
                        String last_dir = null;
                        if (last_dir==null){
                            last_dir= uri;
                            String UPPER_dir=last_dir.toUpperCase();
                            int index = UPPER_dir.indexOf("/" + getArchiveDirectoryName().toUpperCase() + "/");
                            if (index!=-1){
                                session_dir = last_dir.substring(0,index+(2+getArchiveDirectoryName().length()));
                                return session_dir;
                            }else{
                                index = UPPER_dir.indexOf(getArchiveDirectoryName().toUpperCase());
                                if (index!=-1){
                                    int unixSepIndex = last_dir.indexOf("/", index);
                                    int winSepIndex = last_dir.indexOf("\\", index);
                                    if (unixSepIndex ==-1 && winSepIndex==-1)
                                    {
                                        session_dir= last_dir + File.separator;
                                    }else if(unixSepIndex ==-1){
                                        session_dir = last_dir.substring(0,winSepIndex+1);
                                    }else if(winSepIndex ==-1){
                                        session_dir = last_dir.substring(0,unixSepIndex+1);
                                    }else if(winSepIndex < unixSepIndex){
                                        session_dir = last_dir.substring(0,winSepIndex+1);
                                    }else if(winSepIndex > unixSepIndex){
                                        session_dir = last_dir.substring(0,unixSepIndex+1);
                                    }
                                }
                            }
                        }
                    }else if(xnatFile instanceof org.nrg.xdat.om.XnatResourceseries){
                        XnatResourceseries resource = (XnatResourceseries)xnatFile;
                        String uri =resource.getFullPath(rootPath);
                        String last_dir = null;
                        if (last_dir==null){
                            last_dir= uri;
                            String UPPER_dir=last_dir.toUpperCase();
                            int index = UPPER_dir.indexOf("/" + getArchiveDirectoryName().toUpperCase() + "/");
                            if (index!=-1){
                                session_dir = last_dir.substring(0,index+(2+getArchiveDirectoryName().length()));
                                return session_dir;
                            }else{
                                index = UPPER_dir.indexOf(getArchiveDirectoryName().toUpperCase());
                                if (index!=-1){
                                    int unixSepIndex = last_dir.indexOf("/", index);
                                    int winSepIndex = last_dir.indexOf("\\", index);
                                    if (unixSepIndex ==-1 && winSepIndex==-1)
                                    {
                                        session_dir= last_dir + File.separator;
                                    }else if(unixSepIndex ==-1){
                                        session_dir = last_dir.substring(0,winSepIndex+1);
                                    }else if(winSepIndex ==-1){
                                        session_dir = last_dir.substring(0,unixSepIndex+1);
                                    }else if(winSepIndex < unixSepIndex){
                                        session_dir = last_dir.substring(0,winSepIndex+1);
                                    }else if(winSepIndex > unixSepIndex){
                                        session_dir = last_dir.substring(0,unixSepIndex+1);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return session_dir;
    }



    /**
     * @param type
     * @return ArrayList of org.nrg.xdat.om.XnatMrscandata
     */
    public List<XnatImagescandataI> getScansByCondition(String c) {
        List _return = new ArrayList();
        for(XnatImagescandataI scan : this.getSortedScans()){
            String condition = scan.getCondition();
            if (condition ==null)
            {
                if (c==null)
                {
                    _return.add(scan);
                }
            }else{
                if (condition.equalsIgnoreCase(c)) {
                    _return.add(scan);
                }
            }
        }
        return _return;
    }


    public List<XnatAbstractresourceI> getAllResources(){
        List<XnatAbstractresourceI> resources = new ArrayList<XnatAbstractresourceI>();
        for(XnatImagescandataI scan : this.getSortedScans()){
            Iterator files = scan.getFile().iterator();
            while (files.hasNext()){
                XnatAbstractresource file = (XnatAbstractresource)files.next();
                resources.add(file);
            }
        }

        Iterator recons = getReconstructions_reconstructedimage().iterator();
        while(recons.hasNext())
        {
            XnatReconstructedimagedata scan = (XnatReconstructedimagedata)recons.next();
            Iterator outfiles = scan.getOut_file().iterator();
            while (outfiles.hasNext()){
                XnatAbstractresource file = (XnatAbstractresource)outfiles.next();
                resources.add(file);
            }

            Iterator infiles = scan.getIn_file().iterator();
            while (infiles.hasNext()){
                XnatAbstractresource file = (XnatAbstractresource)infiles.next();
                resources.add(file);
            }
        }

        for(XnatImageassessordataI assess : this.getAssessors_assessor()){
            Iterator outfiles = assess.getOut_file().iterator();
            while (outfiles.hasNext()){
                XnatAbstractresource file = (XnatAbstractresource)outfiles.next();
                resources.add(file);
            }

            Iterator infiles = assess.getIn_file().iterator();
            while (infiles.hasNext()){
                XnatAbstractresource file = (XnatAbstractresource)infiles.next();
                resources.add(file);
            }
        }

        Iterator misc = getResources_resource().iterator();
        while(misc.hasNext())
        {
            XnatAbstractresource file = (XnatAbstractresource)misc.next();
            resources.add(file);
        }
        return resources;
    }

    /**
     * Appends this path to the enclosed URI or path variables.
     * @param root
     */
    public void prependPathsWith(String session_path){
        Iterator files= getAllResources().iterator();
        while(files.hasNext())
        {
            XnatAbstractresource file = (XnatAbstractresource)files.next();
            file.prependPathsWith(session_path);
        }
    }




    /**
     * Relatives this path from the first occurence of the indexOf string.
     * @param indexOf
     */
    public void relativePaths(String indexOf){
        Iterator files= getAllResources().iterator();
        while(files.hasNext())
        {
            XnatAbstractresource file = (XnatAbstractresource)files.next();
            file.relativizePaths(indexOf,false);
        }
    }

    public void preLoadFiles(){
        if(this.hasSRBData()){
            this.loadSRBFiles();
        }else{
            this.loadLocalFiles();
        }
    }

    public CatalogSet getCatalogBean(String url){
        XnatProjectdata project = this.getPrimaryProject(false);

        this.preLoadFiles();

        Hashtable<String,Object> fileMap = new Hashtable<String,Object>();
        CatCatalogBean catalog = new CatCatalogBean();

        catalog.setId(this.getId());

        if (hasSRBData()){

            Hashtable fileGroups = getFileGroups();
            for (Enumeration e = fileGroups.keys(); e.hasMoreElements();) {
                String key = (String)e.nextElement();

                ArrayList groupFiles = (ArrayList)fileGroups.get(key);
                int counter=0;
                for(Iterator iter=groupFiles.iterator();iter.hasNext();){
                    Object o = iter.next();
                    if (o instanceof String){

                        String id = (String)o;

                        int index = getFileTracker().getIDIndex(id);
                        File f = getFileTracker().getFile(index);
                        String identifier = "/file/" + id;
                        CatEntryBean entry = new CatEntryBean();
                        entry.setUri(url + identifier);

                        fileMap.put(identifier, f);

                        String path = f.getAbsolutePath();
                        if (path.indexOf(File.separator + project.getId())!=-1){
                            path = path.substring(path.indexOf(File.separator + project.getId()) + 1);
                        }else{
                            if (path.indexOf(File.separator + getArchiveDirectoryName())!=-1){
                                path = path.substring(path.indexOf(File.separator + getArchiveDirectoryName()) + 1);
                            }
                        }

                        entry.setCachepath(path);
                        entry.setName(f.getName());

                        CatEntryMetafieldBean meta = new CatEntryMetafieldBean();
                        meta.setMetafield(path);
                        meta.setName("RELATIVE_PATH");
                        entry.addMetafields_metafield(meta);


                        meta = new CatEntryMetafieldBean();
                        meta.setMetafield(key);
                        meta.setName("GROUP");
                        entry.addMetafields_metafield(meta);

                        meta = new CatEntryMetafieldBean();
                        meta.setMetafield(new Long(f.length()).toString());
                        meta.setName("SIZE");
                        entry.addMetafields_metafield(meta);

                        catalog.addEntries_entry(entry);
                    }else{
                        XNATDirectory dir = (XNATDirectory)o;


                        for (Map.Entry<String,GeneralFile> entryF: dir.getRelativeFiles().entrySet()) {

                            String relative = entryF.getKey();

                            if(relative.indexOf(getArchiveDirectoryName())!=-1)
                            {
                                relative = relative.substring(relative.indexOf(getArchiveDirectoryName()));
                            }

                            String identifier = "/file/" + counter++;
                            CatEntryBean entry = new CatEntryBean();
                            entry.setUri(url + identifier);

                            fileMap.put(identifier, entryF.getValue());

                            entry.setCachepath(relative);
                            entry.setName(entryF.getValue().getName());

                            CatEntryMetafieldBean meta = new CatEntryMetafieldBean();
                            meta.setMetafield(relative);
                            meta.setName("RELATIVE_PATH");
                            entry.addMetafields_metafield(meta);

                            meta = new CatEntryMetafieldBean();
                            meta.setMetafield(key);
                            meta.setName("GROUP");
                            entry.addMetafields_metafield(meta);

                            meta = new CatEntryMetafieldBean();
                            meta.setMetafield(new Long(entryF.getValue().length()).toString());
                            meta.setName("SIZE");
                            entry.addMetafields_metafield(meta);

                            catalog.addEntries_entry(entry);
                        }
                    }
                }
            }
        }else{
            FileTracker ft =getFileTracker();
            for(String id: ft.getIds()){
                int index = getFileTracker().getIDIndex(id);
                File f = getFileTracker().getFile(index);
                String identifier = "/file/" + id;
                CatEntryBean entry = new CatEntryBean();
                entry.setUri(url + identifier);

                fileMap.put(identifier, f);

                String path = f.getAbsolutePath();
                if (path.indexOf(File.separator + project.getId())!=-1){
                    path = path.substring(path.indexOf(File.separator + project.getId()) + 1);
                }else{
                    if (path.indexOf(File.separator + getArchiveDirectoryName())!=-1){
                        path = path.substring(path.indexOf(File.separator + getArchiveDirectoryName()) + 1);
                    }
                }

                entry.setCachepath(path);
                entry.setName(f.getName());

                CatEntryMetafieldBean meta = new CatEntryMetafieldBean();
                meta.setMetafield(path);
                meta.setName("RELATIVE_PATH");
                entry.addMetafields_metafield(meta);

                meta = new CatEntryMetafieldBean();
                meta.setMetafield(new Long(f.length()).toString());
                meta.setName("SIZE");
                entry.addMetafields_metafield(meta);

                catalog.addEntries_entry(entry);
            }
        }

        return new CatalogSet(catalog,fileMap);
    }
    


 public void fixScanTypes(){
        Map<String,ScanTypeMappingI> mappers = new Hashtable<String,ScanTypeMappingI>();

        String project=this.getProject();
        
        List al = this.getScans_scan();
        if (al != null) {
            for (int i = 0; i < al.size(); i++) {
                XnatImagescandata scan = (XnatImagescandata) al.get(i);
                
                if(!mappers.containsKey(scan.getXSIType())){
                	mappers.put(scan.getXSIType(), scan.getScanTypeMapping(project, this.getDBName()));
                }
                
                
                mappers.get(scan.getXSIType()).setType(scan);
            	
            	if(scan.getFile().size()>0){
        			XnatAbstractresourceI abstRes=scan.getFile().get(0);
            		if(abstRes instanceof XnatResource){
            			if(((XnatResource)abstRes).getContent()==null || ((XnatResource)abstRes).getContent().equals("")){
            				((XnatResource)abstRes).setContent("RAW");
            			}
            			if(abstRes.getLabel()!=null && ((XnatResource)abstRes).getFormat()==null){
            				((XnatResource)abstRes).setFormat(abstRes.getLabel());
            			}
            		}
        		}
            }
        }
    }
    
    public void defaultQuality(String s){
    	for(XnatImagescandataI scan:this.getScans_scan()){
    		if(scan.getQuality()==null)
    			((XnatImagescandata)scan).setQuality(s);
    	}
    }
    
    public String getDefaultIdentifier(){
        return null;
    }
    
    /**
     * Copies assigned field values from the indicated image session object to
     * this one, potentially overwriting existing values.
     * @param other object from which assigned values will be copied
     * @throws Exception from XnatExperimentdata.setProjects_project()
     */
    public void copyValuesFrom(final XnatImagesessiondata other) throws Exception {
    	
        if (null != other.getSessionType()){
            this.setSessionType(other.getSessionType());
        }

        if (null != other.getScanner()){
            this.setScanner(other.getScanner());
        }   

        if (null != other.getOperator()){
            this.setOperator(other.getOperator());
        }

        if (null != other.getDate()){
            this.setDate(other.getDate());
        }

        if (null != other.getAcquisitionSite()){
            this.setAcquisitionSite(other.getAcquisitionSite());
        }
        
        if (null != other.getNote()){
            this.setNote(other.getNote());
        }
        
        if (null != other.getInvestigatorFK()){
            this.setInvestigatorFK(other.getInvestigatorFK());
        }
        
        if (null != other.getSubjectId()){
            this.setSubjectId(other.getSubjectId());
        }

        while (this.getSharing_share().size()>0){
            this.removeSharing_share(0);
        }
        
        for (final XnatExperimentdataShareI project : other.getSharing_share()) {
            this.setSharing_share((XnatExperimentdataShare)project);
        }

        if (null != other.getProject()){
            this.setProject(other.getProject());
        }

        if (null != other.getLabel()){
            this.setLabel(other.getLabel());
        }else{
        	if(this.getLabel()!=null){
        		this.setLabel("NULL");
        	}
        }
        
        for(final XnatExperimentdataFieldI otherField : other.getFields_field()){
        	final XnatExperimentdataField field=new XnatExperimentdataField(this.getUser());
        	if (otherField.getName() != null){
        		field.setName(otherField.getName());
        		field.setField(otherField.getField());
        		this.setFields_field(field);
        	}
        }
	
		int scancounter = 0;
		for (final XnatImagescandataI scan : this.getScans_scan()){
		    final XnatImagescandata otherScan = other.getScanById(scan.getId());
		    if (null != otherScan){
			if (null != otherScan.getType())
				((XnatImagescandata)scan).setType(otherScan.getType());
			if (null != otherScan.getQuality())
				((XnatImagescandata)scan).setQuality(otherScan.getQuality());
			if (null != otherScan.getNote())
				((XnatImagescandata)scan).setNote(otherScan.getNote());
		    }
		    scancounter++;
		}
    }
//
//    /**
//     * Generates assessor id to be used for xnat_imageAssessorData.
//     * @return
//     * @throws SQLException
//     */
//    public String createNewAssessorId(String type) throws SQLException{
//        String newID= "";
//        String prefix= "";
//        int i = this.getAssessors(type).size()+1;
//        prefix +=this.getId();
//        
//        String code =ElementSecurity.GetCode(type);
//        if(code!=null && !code.equals(""))
//            prefix+="_" + code;
//        
//        newID=prefix + "_"+ i;
//        String query = "SELECT count(ID) AS id_count FROM xnat_experimentdata WHERE ID='";
//
//        String login = null;
//        if (this.getUser()!=null){
//            login=this.getUser().getUsername();
//        }
//        try {
//            Long idCOUNT= (Long)PoolDBUtils.ReturnStatisticQuery(query + newID + "';", "id_count", this.getDBName(), login);
//            while (idCOUNT > 0){
//                i++;
//                newID=prefix + "_"+ i;
//                idCOUNT= (Long)PoolDBUtils.ReturnStatisticQuery(query + newID + "';", "id_count", this.getDBName(), login);
//            }
//        } catch (Exception e) {
//            logger.error("",e);
//        }
//
//        return newID;
//    }


//    /**
//     * Generates assessor id to be used for xnat_imageAssessorData.
//     * @return
//     * @throws SQLException
//     */
//    public String createNewAssessorId(String visitNum) throws SQLException{
//        String newID= "";
//        int i = this.getAssessorCount()+1;
//        newID=this.getId() + "_"+ visitNum + "_"+ i;
//        String query = "SELECT count(ID) AS id_count FROM xnat_experimentdata WHERE ID='";
//
//        String login = null;
//        if (this.getUser()!=null){
//            login=this.getUser().getUsername();
//        }
//        try {
//            Long idCOUNT= (Long)PoolDBUtils.ReturnStatisticQuery(query + newID + "';", "id_count", this.getDBName(), this.getUser().getUsername());
//            while (idCOUNT > 0){
//                i++;
//                newID=this.getId() + "_"+ visitNum + "_"+ i;
//                idCOUNT= (Long)PoolDBUtils.ReturnStatisticQuery(query + newID + "';", "id_count", this.getDBName(), this.getUser().getUsername());
//            }
//        } catch (Exception e) {
//            logger.error("",e);
//        }
//
//        return newID;
//    }
    
    public boolean validateSubjectId(){
        String subjectid = this.getSubjectId();
        if (subjectid!=null){
            subjectid=StringUtils.RemoveChar(subjectid, '\'');
            String query = "SELECT ID FROM xnat_subjectdata WHERE ID='";
            String login =null;
            if (this.getUser()!=null){
                login = this.getUser().getUsername();
            }
            
            try {
                final String idCOUNT= (String)PoolDBUtils.ReturnStatisticQuery(query + subjectid + "';", "id", this.getDBName(), login);
                if (idCOUNT!=null){
                    return true;
                }
                
                final String project = this.getProject();
                if (project!=null){
                	//CHECK by primary label
                    query = "SELECT id FROM xnat_subjectdata WHERE label='" +
                            subjectid +"' AND project='" + project + "';";
                    String new_subjectid= (String)PoolDBUtils.ReturnStatisticQuery(query, "id", this.getDBName(), login);
                    if (new_subjectid!=null){
                        this.setSubjectId(new_subjectid);
                        return true;
                    }

                    //CHECK by secondary labels
                    query = "SELECT subject_id FROM xnat_projectParticipant WHERE label='" +
                            subjectid +"' AND project='" + project + "';";
                    new_subjectid= (String)PoolDBUtils.ReturnStatisticQuery(query, "subject_id", this.getDBName(), login);
                    if (new_subjectid!=null){
                        this.setSubjectId(new_subjectid);
                        return true;
                    }
                }
            } catch (SQLException e) {
                logger.error("",e);
            } catch (Exception e) {
                logger.error("",e);
            }
        }
        
        return false;
    }


    
    
    public void correctArchivePaths() throws InvalidArchiveStructure, UnknownPrimaryProjectException{
        this.correctArchivePaths(true);
    }
     
    
    public void correctArchivePaths(boolean relativePaths)
    throws InvalidArchiveStructure, UnknownPrimaryProjectException {
	final String session_path = getCurrentSessionFolder(false);
	for (final XnatImagescandataI scan : scans) {
		final List<XnatAbstractresource> files=scan.getFile();
	    for (final XnatAbstractresource file : files) {
			file.prependPathsWith(session_path);
	
			try {
			    if (files.size()==1 || (file.getContent()!=null && file.getContent().endsWith("_RAW")))
			    	file.setProperty("content", "RAW");
			} catch (Throwable e) {
			    logger.error("",e);
			
			}
	    }
	}
    }

    public Map<String,String> getCustomScanFields(String project){
        return Maps.newLinkedHashMap(CUSTOM_SCAN_FIELDS);
    }
    
    public void moveToProject(final XnatProjectdata newProject, final String label , final XDATUser user,final EventMetaI c) throws Exception{
    	if(!this.getProject().equals(newProject.getId()))
    	{
    		if (!MoverMaker.check(this, user)) {
    			throw new InvalidPermissionException(this.getXSIType());
    		}
    		
    		final File rootBackup=MoverMaker.createPrimaryBackupDirectory("move",this.getProject(),getId());
    		final String existingRootPath=this.getProjectData().getRootArchivePath();
    		//FIXME: Is this correct?
    		final String newLabel = label == null? (this.getLabel() == null ? this.getId() : this.getLabel()) : label;
    		final File newSessionDir = new File(new File(newProject.getRootArchivePath(),newProject.getCurrentArc()),newLabel);
    		final String current_label=this.getLabel() == null ? this.getId() : this.getLabel();
    		final BaseXnatImagesessiondata base = this;
    		
    		Map<String,File> fs = new HashMap<String,File>();
    		fs.put("src", this.getSessionDir());
    		CopyOp scanOp = new CopyOp(new OperationI<Map<String,File>>(){
				public void run(Map<String,File> fs) throws Exception {
					new ProjectAnonymizer(base,newProject.getId(), base.getArchivePath(existingRootPath)).call();
					for(XnatImagescandataI scan: getScans_scan()){
		    			for(XnatAbstractresourceI abstRes: scan.getFile()){
		    				MoverMaker.Mover m = MoverMaker.moveResource(abstRes, current_label, base, newSessionDir, existingRootPath, user,c);
		    				m.setResource((XnatAbstractresource)abstRes);
		    				m.call();
		    			}
		    		}
		    		
		    		for(XnatReconstructedimagedataI recon:base.getReconstructions_reconstructedimage()){
		    			for(XnatAbstractresourceI abstRes: recon.getOut_file()){
		    				MoverMaker.Mover m = MoverMaker.moveResource(abstRes, current_label, base, newSessionDir, existingRootPath, user,c);
		    				m.setResource((XnatAbstractresource)abstRes);
		    				m.call();
		    			}
		    		}
		    		
		    		for(XnatImageassessordataI assessor:base.getAssessors_assessor()){
		    			for(XnatAbstractresourceI abstRes: assessor.getOut_file()){
		    				MoverMaker.Mover m = MoverMaker.moveResource(abstRes, current_label, base, newSessionDir, existingRootPath, user,c);
		    				m.setResource((XnatAbstractresource)abstRes);
		    				m.call();
		    			}
		    		}
		    		BaseXnatImagesessiondata.super.moveToProject(newProject, newLabel, user,c);
				}
			}, new File(rootBackup, "src_backup"), fs);
    		
    		try {
    			Run.runTransaction(scanOp);
    		}
    		catch (TransactionException e) {
    			throw new Exception(e);
    		}
    		catch (RollbackException e) {
    			throw new Exception(e);
    		}
    	}
    }
    
    public ArrayList getCatalogSummary() throws Exception{
		String query="SELECT * FROM (SELECT xnat_abstractresource_id,label,element_name, 'resources'::TEXT AS category, NULL::TEXT AS cat_id"+
		 " FROM xnat_experimentdata_resource res_map"+
		 " JOIN xnat_abstractresource abst ON res_map.xnat_abstractresource_xnat_abstractresource_id=abst.xnat_abstractresource_id" +
		 " JOIN xdat_meta_element xme ON abst.extension=xme.xdat_meta_element_id"+
		 " WHERE res_map.xnat_experimentdata_id='"+this.getId() + "'"+
		 "  UNION"+
		 " SELECT xnat_abstractresource_id,label,element_name, 'scans'::TEXT,isd.id"+
		 " FROM xnat_imagescanData isd  "+
		 " JOIN xnat_abstractresource abst ON isd.xnat_imagescandata_id=abst.xnat_imagescandata_xnat_imagescandata_id"+
		 " JOIN xdat_meta_element xme ON abst.extension=xme.xdat_meta_element_id"+
		 " WHERE isd.image_session_id='"+this.getId() + "'"+
		 " UNION"+
		 " SELECT xnat_abstractresource_id,label,element_name, 'reconstructions'::TEXT,recon.id"+
		 " FROM xnat_reconstructedimagedata recon"+
		 " JOIN recon_out_resource map ON recon.xnat_reconstructedimagedata_id=map.xnat_reconstructedimagedata_xnat_reconstructedimagedata_id"+
		 " JOIN xnat_abstractresource abst ON map.xnat_abstractresource_xnat_abstractresource_id=abst.xnat_abstractresource_id"+
		 " JOIN xdat_meta_element xme ON abst.extension=xme.xdat_meta_element_id"+
		 " WHERE image_session_id='"+this.getId() + "'"+
		 " UNION"+
		 " SELECT xnat_abstractresource_id,label,element_name, 'assessments'::TEXT,iad.id"+
		 " FROM xnat_imageAssessorData iad"+
		 " JOIN img_assessor_out_resource map ON iad.id=map.xnat_imageassessordata_id"+
		 " JOIN xnat_abstractresource abst ON map.xnat_abstractresource_xnat_abstractresource_id=abst.xnat_abstractresource_id"+
		 " JOIN xdat_meta_element xme ON abst.extension=xme.xdat_meta_element_id"+
		 " WHERE imagesession_id='"+this.getId() + "'"+
					 " UNION"+
					 " SELECT xnat_abstractresource_id,label,element_name, 'assessments'::TEXT,iad.id"+
					 " FROM xnat_imageAssessorData iad"+
					 " JOIN xnat_experimentdata_resource map ON iad.id=map.xnat_experimentdata_id"+
					 " JOIN xnat_abstractresource abst ON map.xnat_abstractresource_xnat_abstractresource_id=abst.xnat_abstractresource_id" +
					 " JOIN xdat_meta_element xme ON abst.extension=xme.xdat_meta_element_id"+
					 " WHERE imagesession_id='"+this.getId() + "') all_resources";
		
		XFTTable t = XFTTable.Execute(query, this.getDBName(), "system");
		
		return t.rowHashs();
    }
    

    
    public String canDelete(XnatProjectdata proj, XDATUser user){

    	BaseXnatImagesessiondata expt=this;
    	if(this.getItem().getUser()!=null){
    		expt=new XnatImagesessiondata(this.getCurrentDBVersion(true));
    	}
    	if(!expt.hasProject(proj.getId())){
    		return null;
    	}else {

			try {
				SecurityValues values = new SecurityValues();
				values.put(this.getXSIType() + "/project", proj.getId());
				SchemaElement se= SchemaElement.GetElement(this.getXSIType());
				
				if (!user.canDeleteByXMLPath(se,values))
				{
					return "User cannot delete experiments for project " + proj.getId();
				}
			} catch (Exception e1) {
				return "Unable to delete subject.";
			}

    		for(XnatImageassessordataI sad: expt.getAssessors_assessor()){
    			String msg=((XnatImageassessordata)sad).canDelete(proj,user);
    			if(msg!=null){
    				return msg;
    			}
    		}
    	}
		return null;
    }
    

    
    public String delete(XnatProjectdata proj, XDATUser user, boolean removeFiles,EventMetaI c){
    	BaseXnatImagesessiondata expt=this;
    	if(this.getItem().getUser()!=null){
    		expt=new XnatImagesessiondata(this.getCurrentDBVersion(true));
    	}
    	
    	String msg=expt.canDelete(proj,user);

    	if(msg!=null){
    		logger.error(msg);
    		return msg;
    	}
    	
    	if(!expt.getProject().equals(proj.getId())){
			try {
				SecurityValues values = new SecurityValues();
				values.put(this.getXSIType() + "/project", proj.getId());
				
				if (!user.canDelete(expt) && !user.canDeleteByXMLPath(this.getSchemaElement(),values))
				{
					return "User cannot delete experiments for project " + proj.getId();
				}
				
				int index = 0;
				int match = -1;
				for(XnatExperimentdataShareI pp : expt.getSharing_share()){
					if(pp.getProject().equals(proj.getId())){
						SaveItemHelper.authorizedRemoveChild(expt.getItem(), "xnat:experimentData/sharing/share", ((XnatExperimentdataShare)pp).getItem(), user,c);
						match=index;
						break;
					}
					index++;
				}
				
				if(match==-1)return null;
				
				this.removeSharing_share(match);

				final  List<XnatImageassessordataI> expts = expt.getAssessors_assessor();
		        for (XnatImageassessordataI iad : expts){
		        	((XnatImageassessordata)iad).delete(proj,user,false,c);
		        }
		        
				return null;
			} catch (SQLException e) {
				logger.error("",e);
				return e.getMessage();
			} catch (Exception e) {
				logger.error("",e);
				return e.getMessage();
			}
		}else{
			try {
			
				if(!user.canDelete(this)){
					return "User account doesn't have permission to delete this experiment.";
				}
							
				if(removeFiles){
					this.deleteFiles(user,c);
				}

				final  List<XnatImageassessordata> expts = expt.getAssessors_assessor();
		        for (XnatImageassessordata iad : expts){
		        	msg=iad.delete(proj,user,removeFiles,c);
		            if(msg!=null)return msg;
		        }
		        
		        SaveItemHelper.authorizedDelete(expt.getItem().getCurrentDBVersion(), user,c);
				
			    user.clearLocalCache();
				MaterializedView.DeleteByUser(user);
			} catch (SQLException e) {
				logger.error("",e);
				return e.getMessage();
			} catch (Exception e) {
				logger.error("",e);
				return e.getMessage();
			}
		}
    	return null;
    }
    
    public void deleteFiles(UserI user, EventMetaI ci) throws Exception{
    	for(XnatAbstractresourceI abstRes:this.getResources_resource()){
    		((XnatAbstractresource)abstRes).deleteWithBackup(ArcSpecManager.GetInstance().getArchivePathForProject(this.getProject()), user,ci);
    	}
    	
    	String rootPath=ArcSpecManager.GetInstance().getArchivePathForProject(this.getProject());
    	
    	for(final XnatImagescandataI scan: this.getScans_scan()){
        	for(XnatAbstractresourceI abstRes:scan.getFile()){
        		((XnatAbstractresource)abstRes).deleteWithBackup(rootPath, user, ci);
        	}
    	}
    	
    	for(XnatReconstructedimagedataI scan: this.getReconstructions_reconstructedimage()){
        	for(XnatAbstractresourceI abstRes:scan.getOut_file()){
        		((XnatAbstractresource)abstRes).deleteWithBackup(rootPath, user, ci);
            }
    	}
    	
    	for(XnatImageassessordataI scan: this.getAssessors_assessor()){
        	for(XnatAbstractresourceI abstRes:scan.getResources_resource()){
        		((XnatAbstractresource)abstRes).deleteWithBackup(rootPath, user, ci);
            }
        	
        	for(XnatAbstractresourceI abstRes:scan.getOut_file()){
        		((XnatAbstractresource)abstRes).deleteWithBackup(rootPath, user, ci);
            }
    	}
    	
    	File dir=this.getSessionDir();
    	if(dir!=null){
    		FileUtils.MoveToCache(dir);
    	}
    }
    
    public int getAssessmentCount(String project){
    	int count=0;
    	for(int i=0;i<this.getMinimalLoadAssessors().size();i++){
    		XnatExperimentdata expt=(XnatExperimentdata)this.getMinimalLoadAssessors().get(i);
    		if(expt.getProject().equals(project)){
    			count++;
    		}
    	}
    	return count;
    }

    
	public XnatQcmanualassessordataI getManualQC() {
		final List<XnatImageassessordata> assessors = getMinimalLoadAssessors(XnatQcmanualassessordata.SCHEMA_ELEMENT_NAME);
		if (assessors != null && assessors.size() > 0) {
			return (XnatQcmanualassessordata) assessors.get(assessors.size()-1);
		}
		return null;
	}

	public XnatQcassessmentdataI getQCByType(String type) {
		final List<XnatImageassessordata> assessors = getMinimalLoadAssessors(XnatQcassessmentdata.SCHEMA_ELEMENT_NAME);
		final List<XnatImageassessordata> qcassessorOfType = new ArrayList<XnatImageassessordata>();
		for (int i = 0; i < assessors.size(); i++) {
			if (((AutoXnatQcassessmentdata)assessors.get(i)).getType().equals(type)) {
				qcassessorOfType.add(assessors.get(i));
			}
		}
		if (qcassessorOfType != null && qcassessorOfType.size() > 0) {
			return (XnatQcassessmentdata) qcassessorOfType.get(qcassessorOfType.size()-1);
		}
		return null;
	}
	
	public ValProtocoldataI getProtocolValidation() {
		final List<XnatImageassessordata> protocolData = getMinimalLoadAssessors(AutoValProtocoldata.SCHEMA_ELEMENT_NAME);
		if (protocolData != null && protocolData.size() > 0) {
			return (ValProtocoldataI) protocolData.get(protocolData.size()-1);
		}
		return null;
	}
	
	public ScrScreeningassessmentI getScreeningAssessment() {
		final List<XnatImageassessordata> screeningAssessment = getMinimalLoadAssessors(AutoScrScreeningassessment.SCHEMA_ELEMENT_NAME);
		if (screeningAssessment != null && screeningAssessment.size() > 0) {
			return (ScrScreeningassessmentI) screeningAssessment.get(screeningAssessment.size()-1);
		}
		return null;
	}
	
	public List<ScanAssessorI> getScanAssessors(){
		List al = new ArrayList();
         Iterator min = this.getMinimalLoadAssessors().iterator();
         while (min.hasNext())
         {
             ItemI assessor = (ItemI)min.next();
             if (assessor instanceof ScanAssessorI)
             {
                  al.add(assessor);
             }
         }
         Collections.sort(al, new AssessorComparator());
         return al;
	}
	
	@Override
	public void preSave() throws Exception{
		super.preSave();
		
		final String expectedPath=this.getExpectedSessionDir().getAbsolutePath().replace('\\', '/');
		
		for(final XnatImagescandataI scan:this.getScans_scan()){
			((XnatImagescandata)scan).setImageSessionData((XnatImagesessiondata)this);
			((XnatImagescandata)scan).validate(expectedPath);
		
		}

		for(final XnatReconstructedimagedataI recon:this.getReconstructions_reconstructedimage()){
			((XnatReconstructedimagedata)recon).setImageSessionData((XnatImagesessiondata)this);
			((XnatReconstructedimagedata)recon).validate(expectedPath);
		}

		for(final XnatImageassessordataI assess:this.getAssessors_assessor()){
			((XnatImageassessordata)assess).setImageSessionData((XnatImagesessiondata)this);
			((XnatImageassessordata)assess).preSave();
		}
	}
}
