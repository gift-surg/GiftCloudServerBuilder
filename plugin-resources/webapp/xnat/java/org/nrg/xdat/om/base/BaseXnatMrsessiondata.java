/**
 * Copyright 2005 Harvard University / Howard Hughes Medical Institute (HHMI) All Rights Reserved
 * Copyright (c) 2008 Washington University
 */
package org.nrg.xdat.om.base;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatMrscandata;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.om.base.auto.AutoXnatMrsessiondata;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.StringUtils;


/**
 * @author XDAT
 *
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BaseXnatMrsessiondata extends AutoXnatMrsessiondata {

    public BaseXnatMrsessiondata(ItemI item) {
        super(item);
    }

    public BaseXnatMrsessiondata(UserI user) {
        super(user);
    }

    public BaseXnatMrsessiondata() {
    }

    public BaseXnatMrsessiondata(Hashtable properties, UserI user) {
        super(properties, user);
    }

    public static Comparator GetScannerDelayComparator()
    {
	return (new BaseXnatMrsessiondata()).getScannerDelayComparator();
    }

    public Comparator getScannerDelayComparator()
    {
	return new ScannerDelayComparator();
    }

    public class ScannerDelayComparator implements Comparator{
	public ScannerDelayComparator()
	{
	}
	public int compare(Object o1, Object o2) {
	    BaseXnatMrsessiondata  value1 = (BaseXnatMrsessiondata)(o1);
	    BaseXnatMrsessiondata value2 = (BaseXnatMrsessiondata)(o2);

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

	    int i = Compare(value1.getScanner(),value2.getScanner());

	    if (i == 0)
	    {
		return Compare(value1.getDelay(),value2.getDelay());
	    }else{
		return i;
	    }
	}
    }

    private static int Compare(final Comparable o1, final Comparable o2) {
	if (o1 == null) {
	    return null == o2 ? 0 : -1;
	} else {
	    return null == o2 ? 1 : o1.compareTo(o2);
	}
    }

    public String getWorkflowStatus()
    {
        WrkWorkflowdata wkdata = null;

        CriteriaCollection cc= new CriteriaCollection("AND");
        cc.addClause("wrk:workFlowData.ID",getId());
        ArrayList al =WrkWorkflowdata.getWrkWorkflowdatasByField(cc, getUser(), false);
        if (al.size()>0){
            wkdata= (WrkWorkflowdata)al.get(al.size()-1);
            if (wkdata.getPipelineName().toLowerCase().indexOf("transfer")!=-1){
                if(wkdata.getStatus().equalsIgnoreCase("In Progress"))
                {
                    if (wkdata.getCurrentStepId().equalsIgnoreCase("Store"))
                        return "storing";
                    else
                        return "archiving";
                }if(wkdata.getStatus().equalsIgnoreCase("Running"))
                {
                    if (wkdata.getCurrentStepId().equalsIgnoreCase("Store"))
                        return "storing";
                    else
                        return "archiving";
                }else if (wkdata.getStatus().equalsIgnoreCase("Complete")){
                    return "uploaded";
                }else if (wkdata.getStatus().equalsIgnoreCase("Queued")){
                    return "queued";
                }else{
                    return  wkdata.getPipelineName() + " " + wkdata.getStatus();
                }
            }else{
                if(wkdata.getStatus().equalsIgnoreCase("Running"))
                {
                    return "processing";
                }else if (wkdata.getStatus().equalsIgnoreCase("Complete")){
                    return "processed";
                }else if (wkdata.getStatus().equalsIgnoreCase("Awaiting Action")){
                    return "waiting";
                }else if (wkdata.getStatus().equalsIgnoreCase("Queued")){
                    return "queued";
                }else{
                    return wkdata.getPipelineName() + " " + wkdata.getStatus();
                }
            }
        }

        if (this.getScanner()!=null)
        {
            return "uploading";
        }else{
            return "";
        }


    }

    public void fixScanTypes(){
        
        try {
//        	String query = "SELECT DISTINCT REPLACE(REPLACE(REPLACE(REPLACE(UPPER(scan.series_description),' ',''),'_',''),'-',''),'*','') AS series_description,scan.type,UPPER(parameters_imagetype) AS parameters_imagetype,frames FROM xnat_imagescandata scan LEFT JOIN xnat_mrscandata mr ON scan.xnat_imagescandata_id=mr.xnat_imagescandata_id WHERE scan.series_description IS NOT NULL;";
//            XFTTable t = XFTTable.Execute(query, this.getDBName(), "system");
//            Hashtable<String,ScanTypeMapping> allProjects= new Hashtable<String,ScanTypeMapping>();
//            t.resetRowCursor();
//            while(t.hasMoreRows()){
//            	Hashtable rowHash=t.nextRowHash();
//            	String sd=(String)rowHash.get("series_description");
//            	if(!allProjects.containsKey(sd)){
//            		allProjects.put(sd,new ScanTypeMapping());
//            	}
//            	
//            	allProjects.get(sd).add((String)rowHash.get("type"),(String)rowHash.get("parameters_imagetype"),(Integer)rowHash.get("frames"));
//            	
//            }
//            
            Map<String,ScanTypeMapping> thisProject =new Hashtable<String,ScanTypeMapping>();
            
            if(this.getProject()!=null){
            	String query = "SELECT DISTINCT REPLACE(REPLACE(REPLACE(REPLACE(UPPER(scan.series_description),' ',''),'_',''),'-',''),'*','') AS series_description,scan.type,UPPER(parameters_imagetype) AS parameters_imagetype,frames FROM xnat_imagescandata scan LEFT JOIN xnat_mrscandata mr ON scan.xnat_imagescandata_id=mr.xnat_imagescandata_id LEFT JOIN xnat_experimentData isd ON scan.image_session_id=isd.id WHERE scan.series_description IS NOT NULL AND isd.project='" + this.getProject() + "';";
            	XFTTable t = XFTTable.Execute(query, this.getDBName(), "system");
                t.resetRowCursor();
                while(t.hasMoreRows()){
                	Hashtable rowHash=t.nextRowHash();
                	String sd=(String)rowHash.get("series_description");
                	if(!thisProject.containsKey(sd)){
                		thisProject.put(sd,new ScanTypeMapping());
                	}
                	
                	thisProject.get(sd).add((String)rowHash.get("type"),(String)rowHash.get("parameters_imagetype"),(Integer)rowHash.get("frames"));
                }
            }
            
            String[] types = new String[]{"FLASH5","FLASH20","FLASH30","FLASH3","BDYMAP100","BDYMAP","BOLD","DTI","FLAIR","FLASH","FST_MEF","HDMAP100","HDMAP","LO_RES","MEF30","MEF5","MPRAGE","MTC5","TSE","LOCALIZER","AASCOUT","3DT2"};
            List<XnatImagescandataI> al = this.getScans_scan();
            if (al != null) {
                for (int i = 0; i < al.size(); i++) {
                    XnatImagescandata scan = (XnatImagescandata) al.get(i);
                    
                    String series_description=scan.getSeriesDescription();
                    String type=scan.getType();
                    
                    if ((type !=null && !type.equals("")) || (series_description==null || series_description.equals(""))){
                        continue;
                    }
                    
                    if (series_description.startsWith("INVALID: "))
                    {
                        series_description = series_description.substring(9);
                    }
                    
                    String formatted_series_description =series_description.toUpperCase();
                    formatted_series_description=StringUtils.ReplaceStr(formatted_series_description, " ", "");
                    formatted_series_description=StringUtils.ReplaceStr(formatted_series_description, "_", "");
                    formatted_series_description=StringUtils.ReplaceStr(formatted_series_description, "-", "");
                    formatted_series_description=StringUtils.ReplaceStr(formatted_series_description, "*", "");
                    
                    String imgtype=null;
                    if(scan instanceof XnatMrscandata){
                    	imgtype=((XnatMrscandata)scan).getParameters_imagetype();
                    }
                    
                	if (thisProject.containsKey(formatted_series_description)){
                        scan.setType(thisProject.get(formatted_series_description).match(series_description, imgtype, scan.getFrames()));
                	}
//                	else if (allProjects.containsKey(formatted_series_description)){
//                        scan.setType(allProjects.get(formatted_series_description).match(series_description, imgtype, scan.getFrames()));
//                    }REMOVE cross project mapping per Dan 5/29/09
                	else{
                        try {
                            boolean matched = false;
                            for(int j=0;j<types.length;j++)
                            {
                                if (series_description.indexOf(types[j])!=-1)
                                {
                                     scan.setType(types[j]);
                                     matched=true;
                                     break;
                                }
                            }

                            if (!matched){
                                if (series_description.indexOf("MPR")!=-1)
                                {
                                     scan.setType("MPRAGE");
                                }
                            }
                        } catch (Throwable e) {
                            logger.error("",e);
                        }
                    }
                	
                	if(scan.getType()==null){
                		scan.setType(series_description);
                	}                	

            		final List<XnatAbstractresource> files=scan.getFile();
                	
                	for(XnatAbstractresource abstRes:files){
                		try{
	        			    if (files.size()==1 || (abstRes.getContent()!=null && abstRes.getContent().endsWith("_RAW")))
	        			    	abstRes.setProperty("content", "RAW");
                		}catch(Exception ex){
            			    logger.error("",ex);
                		}
                	}
                }
            }
        } catch (SQLException e) {
            logger.error("",e);
        } catch (DBPoolException e) {
            logger.error("",e);
        }
    }
    
    /**
     * 
     * @param otherImageSession
     * @throws Exception
     */
    public void copyValuesFrom(final XnatImagesessiondata otherImageSession) throws Exception {
	super.copyValuesFrom(otherImageSession);
        
	final XnatMrsessiondata otherMR = (XnatMrsessiondata)otherImageSession;
	
        if (null != otherMR.getStabilization()){
            this.setStabilization(otherMR.getStabilization());
        }
        
        if (null != otherMR.getMarker()){
            this.setMarker(otherMR.getMarker());
        }
        
        if (null != otherMR.getCoil()){
            this.setCoil(otherMR.getCoil());
        }
    }
        
    public String getDefaultIdentifier(){
        return this.getDcmpatientname();
    }

    public ArrayList getUnionOfScansByType(String csvType) {
        ArrayList _return = new ArrayList();
        String[] types = csvType.split(",");
        if (types != null && types.length > 0) {
        	for(int i = 0; i < types.length; i++) {
        	    ArrayList rtn = getScansByType(types[i].trim());
        	    if (rtn.size() > 0 )_return.addAll(rtn);
        	}
        }
        _return.trimToSize();
        return _return;
    }

    public ArrayList getUnionOfScansByType(String csvType, boolean chronological) {
        ArrayList _return = new ArrayList();
        if (chronological) {
            String[] types = csvType.split(",");
            Hashtable scanTypes = new Hashtable();
            if (types != null && types.length > 0) {
            	for(int i = 0; i < types.length; i++) {
            		scanTypes.put(types[i].trim(), "");
            	}
            }
            for(XnatImagescandata scan :  this.getScans_scan()){
            	if (scan.getType() != null && scanTypes.containsKey(scan.getType())) {
            		_return.add(scan);
            	}
            }
            _return.trimToSize();
            return _return;
        }else
        	return getUnionOfScansByType(csvType);
    }

    public ArrayList getUnionOfScansByType(String csvType, String chronological) {
    	return getUnionOfScansByType(csvType, new Boolean(chronological).booleanValue());
    }
    
    public Map<String,String> getCustomScanFields(String project){
    	Map<String,String> customheaders= super.getCustomScanFields(project);
    	
    	customheaders.put("parameters/tr","");
    	customheaders.put("parameters/te","");
    	customheaders.put("parameters/ti","");
    	customheaders.put("parameters/flip","");
    	customheaders.put("parameters/sequence","");
    	customheaders.put("parameters/scanTime","");
    	customheaders.put("parameters/imageType","");
    	customheaders.put("parameters/scanSequence","");
    	customheaders.put("parameters/seqVariant","");
    	customheaders.put("parameters/scanOptions","");
    	customheaders.put("parameters/acqType","");
    	
    	return customheaders;
    }
    public class ScanTypeMapping{
	    private List<ScanType> types=new ArrayList<ScanType>();
		
	    public void add(ScanType st){
	    	types.add(st);
	    }
	    
	    public void add(String t, String it, Integer f){
	    	types.add(new ScanType(t,it,f));
	    }
	    
		public String match(String desc,String imgtype,Integer frames){
			if(types.size()==1){
				return types.get(0).getType();
			}
			//match by imgtype
			if(imgtype!=null && !imgtype.equals("")){
				for(ScanType st: types){
					if(imgtype.equalsIgnoreCase(st.getImgtype())){
						return st.getType();
					}
				}
			}
			
			//match by frames
			if(frames!=null){
				for(ScanType st: types){
					if(frames.equals(st.getFrames())){
						return st.getType();
					}
				}
			}
			
			if(imgtype==null){
				for(ScanType st: types){
					if(st.getImgtype()==null){
						return st.getType();
					}
				}
			}
			
			if(frames==null){
				for(ScanType st: types){
					if(st.getFrames()==null){
						return st.getType();
					}
				}
			}
			
			return types.get(0).getType();
		}
	}
    public class ScanType{
	    private String _type=null;
	    private String _imgtype=null;
	    private Integer _frames=null;
		
	    public ScanType(String t, String it, Integer f){
	    	_type=t;
	    	_imgtype=it;
	    	_frames=f;
	    }
	    
		public Integer getFrames() {
			return _frames;
		}
		public void setFrames(Integer frames) {
			this._frames = frames;
		}
		public String getImgtype() {
			return _imgtype;
		}
		public void setImgtype(String imgtype) {
			this._imgtype = imgtype;
		}
		public String getType() {
			return _type;
		}
		public void setType(String type) {
			this._type = type;
		}
	}
}


