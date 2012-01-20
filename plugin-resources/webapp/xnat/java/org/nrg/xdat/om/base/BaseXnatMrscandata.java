//Copyright 2005 Harvard University / Howard Hughes Medical Institute (HHMI) All Rights Reserved
/*
 * GENERATED FILE
 * Created on Tue Aug 16 15:08:17 CDT 2005
 *
 */
package org.nrg.xdat.om.base;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.model.XnatMrqcscandataI;
import org.nrg.xdat.om.XnatMrscandata;
import org.nrg.xdat.om.base.auto.AutoXnatMrscandata;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xnat.helpers.scanType.ScanTypeMappingI;

/**
 * @author XDAT
 * 
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class BaseXnatMrscandata extends AutoXnatMrscandata {

	public BaseXnatMrscandata(ItemI item) {
		super(item);
	}

	public BaseXnatMrscandata(UserI user) {
		super(user);
	}

	public BaseXnatMrscandata() {
	}

	public BaseXnatMrscandata(Hashtable properties, UserI user) {
		super(properties, user);
	}

	public XnatMrqcscandataI getManualQC() {
		return (XnatMrqcscandataI) super.getManualQC();
	}
	
	public ScanTypeMappingI getScanTypeMapping(final String project, final String dbName){
		return new MRScanTypeMapping(project,dbName);
	}
	
	public class MRScanTypeMapping implements ScanTypeMappingI{
	    
	    protected final String project;
	    protected final String dbName;
	    protected Map<String,ScanTypeHistory> thisProject =null;
	    
	    public MRScanTypeMapping(String project,String dbName){
	    	this.project=project;
	    	this.dbName=dbName;
	    	loadScanTypes();
	    }
		
		public void loadScanTypes() {
	    	if(thisProject==null){
		    	thisProject=new Hashtable<String,ScanTypeHistory>();
		        
		        if(this.project!=null){
		        	try {
		        		String query = "SELECT DISTINCT REPLACE(REPLACE(REPLACE(REPLACE(UPPER(scan.series_description),' ',''),'_',''),'-',''),'*','') AS series_description,scan.type,UPPER(parameters_imagetype) AS parameters_imagetype,frames FROM xnat_imagescandata scan LEFT JOIN xnat_mrscandata mr ON scan.xnat_imagescandata_id=mr.xnat_imagescandata_id LEFT JOIN xnat_experimentData isd ON scan.image_session_id=isd.id WHERE scan.series_description IS NOT NULL AND isd.project='" + project + "';";
		            	XFTTable t = XFTTable.Execute(query, dbName, "system");
		                t.resetRowCursor();
		                while(t.hasMoreRows()){
		                	Hashtable rowHash=t.nextRowHash();
		                	String sd=(String)rowHash.get("series_description");
		                	if(!thisProject.containsKey(sd)){
		                		thisProject.put(sd,new ScanTypeHistory());
		                	}
		                	
		                	thisProject.get(sd).add((String)rowHash.get("type"),(String)rowHash.get("parameters_imagetype"),(Integer)rowHash.get("frames"));
		                }
					} catch (SQLException e) {
						logger.error("",e);
					} catch (DBPoolException e) {
						logger.error("",e);
					}
		        }
	    	}
	    }

		@Override
		public void setType(XnatImagescandataI scan) {
			String series_description=scan.getSeriesDescription();
            String type=scan.getType();
            
            if ((type !=null && !type.equals("")) || (series_description==null || series_description.equals(""))){
            	return;
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
        	
        	if(scan.getType()==null){
        		scan.setType(series_description);
        	}  
		}
		
		public class ScanTypeHistory{
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
}
