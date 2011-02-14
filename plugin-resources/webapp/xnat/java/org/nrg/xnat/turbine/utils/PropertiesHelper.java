/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.xnat.turbine.utils;

import java.io.File;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.nrg.xft.XFT;

public class PropertiesHelper<T extends Object>  {
    static org.apache.log4j.Logger logger = Logger.getLogger(ImageUploadHelper.class);
    
	public static Map<String,Map<String,Object>> RetrievePropertyObjects(final File props,final String identifier,final String[] fields){
		try {
			if(props.exists()){
				final Configuration config=new PropertiesConfiguration(props);
				
				return ParseStandardizedConfig(config,identifier,fields);
			}
		} catch (ConfigurationException e) {
			logger.error("",e);
		}
		
		return new Hashtable<String,Map<String,Object>>();
	}
	
	public static Configuration RetrieveConfiguration(final File props) throws ConfigurationException{
		if(props.exists()){
			return new PropertiesConfiguration(props);
		}
		
		return null;
	}
	
	public static Map<String,Map<String,Object>> ParseStandardizedConfig(final Configuration config,final String identifier,final String[] fields){
		final Map<String,Map<String,Object>> objects=new Hashtable<String,Map<String,Object>>();
		
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
		
		return objects;
	}
	
	
	
	public static String GetProperty(final File props,final String identifier){
		try {
			if(props.exists()){
				final Configuration config=new PropertiesConfiguration(props);
				
				return config.getString(identifier);
}
		} catch (ConfigurationException e) {
			logger.error("",e);
		}
		
		return null;
	}
	
	@SuppressWarnings({ "rawtypes" })
	public Map<String,T> buildObjectsFromProps(final String fileName, final String identifier,final String[] propFields, final String classNameProp, final Class[] contructorArgT, final Object[] contructorArgs) throws ConfigurationException{
		return buildObjectsFromProps(new File(XFT.GetConfDir(),fileName), identifier, propFields, classNameProp, contructorArgT, contructorArgs);
	}
	
	@SuppressWarnings({ "rawtypes" })
	public Map<String,T> buildObjectsFromProps(final File f, final String identifier,final String[] propFields, final String classNameProp, final Class[] contructorArgT, final Object[] contructorArgs) throws ConfigurationException{
		return buildObjectsFromProps(RetrieveConfiguration(f), identifier, propFields, classNameProp, contructorArgT, contructorArgs);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map<String,T> buildObjectsFromProps(final Configuration config, final String identifier,final String[] propFields, final String classNameProp, final Class[] contructorArgT, final Object[] contructorArgs){
		final Map<String,T> objs=new HashMap<String,T>();
		   
	    if(config!=null){
		   final Map<String,Map<String,Object>> confBuilders=PropertiesHelper.ParseStandardizedConfig(config, identifier, propFields);
			for(final String key:confBuilders.keySet()){
				final String className=(String)confBuilders.get(key).get(classNameProp);
				try {
					if(className!=null)
						objs.put(key,(T)Class.forName(className).getConstructor(contructorArgT).newInstance(contructorArgs));
				} catch (Exception e) {
					logger.error("",e);
				}
			}
	    }
	    
	    return objs;
	}
	
	public Map<String,Class<? extends T>> buildClassesFromProps(final String fileName, final String identifier,final String[] propFields, final String classNameProp) throws ConfigurationException{
		return buildClassesFromProps(new File(XFT.GetConfDir(),fileName), identifier, propFields, classNameProp);
	}
	
	public Map<String,Class<? extends T>> buildClassesFromProps(final File f, final String identifier,final String[] propFields, final String classNameProp) throws ConfigurationException{
		return buildClassesFromProps(RetrieveConfiguration(f), identifier, propFields, classNameProp);
	}
	
	@SuppressWarnings({ "unchecked"})
	public Map<String,Class<? extends T>> buildClassesFromProps(final Configuration config, final String identifier,final String[] propFields, final String classNameProp){
		final Map<String,Class<? extends T>> objs=new HashMap<String,Class<? extends T>>();
		   
	    if(config!=null){
		   final Map<String,Map<String,Object>> confBuilders=PropertiesHelper.ParseStandardizedConfig(config, identifier, propFields);
			for(final String key:confBuilders.keySet()){
				final String className=(String)confBuilders.get(key).get(classNameProp);
				try {
					if(className!=null)
						objs.put(key,(Class<? extends T>)Class.forName(className));
				} catch (Exception e) {
					logger.error("",e);
				}
			}
	    }
	    
	    return objs;
	}
}
