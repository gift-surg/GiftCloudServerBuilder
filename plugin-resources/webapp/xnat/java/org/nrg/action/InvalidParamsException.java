/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.action;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

public class InvalidParamsException extends Exception {
	private final Multimap<String,String> mgs;
	
	public InvalidParamsException(final String param, final String msg){
		super();
		mgs=LinkedHashMultimap.create();
		mgs.put(param,msg);
	}
	
	public InvalidParamsException(final Multimap<String,String> map){
		super();
		mgs=map;
	}
	
	public Multimap<String,String> getMessages(){
		return mgs;
	}
}
