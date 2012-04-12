package org.nrg.xnat.helpers.scanType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.utils.StringUtils;

public class ScanTypeMapping implements ScanTypeMappingI{
	static Logger logger = Logger.getLogger(ScanTypeMapping.class);
    
    protected final String project;
    protected final String dbName;
    
    public ScanTypeMapping(String project,String dbName){
    	this.project=project;
    	this.dbName=dbName;
    	loadScanTypes();
    }

    
	
    public class ScanType{
	    private String _type=null;
	    private Integer _frames=null;
		
	    public ScanType(String t, Integer f){
	    	_type=t;
	    	_frames=f;
	    }
	    
		public Integer getFrames() {
			return _frames;
		}
		public void setFrames(Integer frames) {
			this._frames = frames;
		}
		public String getType() {
			return _type;
		}
		public void setType(String type) {
			this._type = type;
		}
	}
    
    public class ScanTypeHistory{
    	private List<ScanType> types=new ArrayList<ScanType>();
        public void add(ScanType st){
        	types.add(st);
        }
        
        public void add(String t, Integer f){
        	types.add(new ScanType(t,f));
        }
        
    	public String match(String desc,Integer frames){
    		if(types.size()==1){
    			return types.get(0).getType();
    		}
    		
    		//match by frames
    		if(frames!=null){
    			for(ScanType st: types){
    				if(frames.equals(st.getFrames())){
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
    
    protected Map<String,ScanTypeHistory> thisProject =null;
    
    @SuppressWarnings("rawtypes")
	public void loadScanTypes() {
    	if(thisProject==null){
	    	thisProject=new Hashtable<String,ScanTypeHistory>();
	        
	        if(this.project!=null){
	        	try {
					String query = "SELECT DISTINCT REPLACE(REPLACE(REPLACE(REPLACE(UPPER(scan.series_description),' ',''),'_',''),'-',''),'*','') AS series_description,scan.type,frames FROM xnat_imagescandata scan LEFT JOIN xnat_experimentData isd ON scan.image_session_id=isd.id WHERE scan.series_description IS NOT NULL AND isd.project='" + project + "';";
					XFTTable t = XFTTable.Execute(query, dbName, "system");
					t.resetRowCursor();
					while(t.hasMoreRows()){
						Hashtable rowHash=t.nextRowHash();
						String sd=(String)rowHash.get("series_description");
						if(!thisProject.containsKey(sd)){
							thisProject.put(sd,new ScanTypeHistory());
						}
						
						thisProject.get(sd).add((String)rowHash.get("type"),(Integer)rowHash.get("frames"));
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
        
    	if (thisProject.containsKey(formatted_series_description)){
            scan.setType(thisProject.get(formatted_series_description).match(series_description, scan.getFrames()));
    	}

    	
    	if(scan.getType()==null){
    		scan.setType(series_description);
    	}
	}
}