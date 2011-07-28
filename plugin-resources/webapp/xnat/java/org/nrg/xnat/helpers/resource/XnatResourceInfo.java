/**
 * Copyright 2010 Washington University
 */
package org.nrg.xnat.helpers.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nrg.xft.utils.StringUtils;

/**
 * @author Timothy R Olsen -- WUSTL
 * Used to encapsulate the data typically associated with the resource and file elements in XNAT.
 */
public class XnatResourceInfo {
	private String description,format,content=null;
	private List<String> tags=new ArrayList<String>();
	private Map<String,String> meta=new HashMap<String,String>();
	
	public String getDescription() {
		return description;
	}
	public void setDescription(final String description) {
		this.description = description;
	}
	public String getFormat() {
		return format;
	}
	public void setFormat(final String format) {
		this.format = format;
	}
	public String getContent() {
		return content;
	}
	public void setContent(final String content) {
		this.content = content;
	}
	public List<String> getTags() {
		return tags;
	}
	public void setMeta(final Map<String, String> meta) {
		this.meta = meta;
	}
	
	public void addMeta(final String key, final String value){
		this.meta.put(key, value);
	}
	public Map<String, String> getMeta() {
		return meta;
	}
	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public void addTag(String tag) {
		this.tags.add(tag);
	}
	
	

	
    public static XnatResourceInfo buildResourceInfo(final String description, final String format, final String content, final String[] tags){
		XnatResourceInfo info = new XnatResourceInfo();
        
	    if(description!=null){
	    	info.setDescription(description);
	    }
	    if(format!=null){
	    	info.setFormat(format);
	    }
	    if(content!=null){
	    	info.setContent(content);
	    }
	    
	    if(tags!=null){
	    	for(String tag: tags){
	    		tag = tag.trim();
	    		if(!tag.equals("")){
	    			for(String s:StringUtils.CommaDelimitedStringToArrayList(tag)){
	    				s=s.trim();
	    				if(!s.equals("")){
	    		    		if(s.indexOf("=")>-1){
	    		    			info.addMeta(s.substring(0,s.indexOf("=")),s.substring(s.indexOf("=")+1));
	    		    		}else{
	    		    			if(s.indexOf(":")>-1){
		    		    			info.addMeta(s.substring(0,s.indexOf(":")),s.substring(s.indexOf(":")+1));
		    		    		}else{
		    		    			info.addTag(s);
		    		    		}
	    		    		}
	    				}
	    			}
	    			
	    		}
	    	}
	    }
		
		return info;
	}
}
