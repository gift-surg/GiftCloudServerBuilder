// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.turbine.utils;

import java.util.ArrayList;
import java.util.List;

import org.nrg.xft.XFT;
import org.nrg.xft.XFTTable;
import org.nrg.xft.identifier.IDGeneratorI;
import org.nrg.xft.utils.StringUtils;

public class IDGenerator implements IDGeneratorI {
	String column=null;
	String tableName=null;
	Integer digits=null;
	String code=null;
	private static String site_id=null;
	
	private static String getSiteID(){
		if(site_id==null){
			site_id = XFT.GetSiteID();
			site_id = StringUtils.ReplaceStr(site_id, " ", "");
			site_id = StringUtils.ReplaceStr(site_id, "-", "_");
			site_id = StringUtils.ReplaceStr(site_id, "\"", "");
			site_id = StringUtils.ReplaceStr(site_id, "'", "");
			site_id = StringUtils.ReplaceStr(site_id, "^", "");
		}
		return site_id;
	}
	
	private static List<String> claimedIDs=new ArrayList<String>();
	
	public synchronized String generateIdentifier() throws Exception{
		String site= IDGenerator.getSiteID();
		
		if(code!=null){
			site +="_"+code;
		}else if(tableName.equalsIgnoreCase("xnat_subjectData")){
			site +="_S";
		}else if(tableName.equalsIgnoreCase("xnat_experimentData")){
			site +="_E";
		}
		
		String temp_id=null;
		
		XFTTable table = org.nrg.xft.search.TableSearch.Execute("SELECT " + column + " FROM " + tableName + " WHERE " + column + " LIKE '" + site + "%';", null, null);
        ArrayList al =table.convertColumnToArrayList(column.toLowerCase());
        
        if (al.size()>0 || claimedIDs.size()>0){
            int count =al.size()+1;
            String full = org.apache.commons.lang.StringUtils.leftPad((new Integer(count)).toString(), digits, '0');
            temp_id = site+ full;

            while (al.contains(temp_id) || claimedIDs.contains(temp_id)){
                count++;
                full =org.apache.commons.lang.StringUtils.leftPad((new Integer(count)).toString(), digits, '0');
                temp_id = site+ full;
            }
            
            claimedIDs.add(temp_id);

            return temp_id;
        }else{
            int count =1;
            String full = org.apache.commons.lang.StringUtils.leftPad((new Integer(count)).toString(), digits, '0');
            temp_id = site+ full;
            return temp_id;
        }
	}

	public String getColumn() {
		return column;
	}

	public Integer getDigits() {
		return digits;
	}

	public String getTable() {
		return tableName;
	}

	public void setColumn(String s) {
		this.column=s;
	}

	public void setDigits(Integer i) {
		this.digits=i;
	}

	public void setTable(String s) {
		this.tableName=s;
	}


	public String getCode() {
		return code;
	}

	public void setCode(String s) {
		this.code=s;
	}
}
