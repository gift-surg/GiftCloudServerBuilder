/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.util;

import org.nrg.action.InvalidParamsException;

import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

public class GoogleUtils {
	public static Object getFirstParam(final Multimap<String,Object> params,final String key){
		if(contains(params,key)){
			return Iterables.get(params.get(key),0);
		}else{
			return null;
		}
	}
	
	public static Object getFirstParamREQ(final Multimap<String,Object> params,final String key) throws InvalidParamsException{
		if(contains(params,key)){
			return Iterables.get(params.get(key),0);
		}else{
			throw new InvalidParamsException(key, "Required Parameter.");
		}
	}

	public static Boolean getFirstBooleanParam(final Multimap<String,Object> params,final String key,final Boolean defaultValue){
		if(contains(params,key)){
			return Boolean.parseBoolean((String)Iterables.get(params.get(key),0));
		}else{
			return defaultValue;
		}
	}
	
	public static Boolean getFirstBooleanParamREQ(final Multimap<String,Object> params,final String key) throws InvalidParamsException{
		if(contains(params,key)){
			return Boolean.parseBoolean((String)Iterables.get(params.get(key),0));
		}else{
			throw new InvalidParamsException(key, "Required Parameter.");
		}
	}
	public static boolean contains(final Multimap<String,Object> params,final String key){
		if(params.containsKey(key) && params.get(key).size()>0){
			return true;
		}else{
			return false;
		}
	}
}
