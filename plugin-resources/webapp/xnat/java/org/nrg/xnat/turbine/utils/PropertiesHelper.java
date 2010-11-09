/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.xnat.turbine.utils;

import java.io.File;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class PropertiesHelper {
    static org.apache.log4j.Logger logger = Logger.getLogger(ImageUploadHelper.class);
    
	public static Map<String,Map<String,Object>> RetrievePropertyObjects(final File props,final String identifier,final String[] fields){
		Map<String,Map<String,Object>> objects=new Hashtable<String,Map<String,Object>>();
		try {
			if(props.exists()){
				final Configuration config=new PropertiesConfiguration(props);
				
				final String[] sA=config.getStringArray(identifier);
				if(sA!=null){
					for(final String objectName: sA){
						if(!objects.containsKey(objectName))objects.put(objectName, new Hashtable<String,Object>());
						
						final String fieldID=identifier+"."+objectName;
						for(final String field:fields){
							Object o=config.getProperty(fieldID+"."+field);
							if(o!=null)objects.get(objectName).put(field, o);
						}
					}
				}
			}
		} catch (ConfigurationException e) {
			logger.error("",e);
		}
		
		return objects;
	}
}
