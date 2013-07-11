/*
 * org.nrg.xnat.restlet.resources.search.SearchElementListResource
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:40 PM
 */
package org.nrg.xnat.restlet.resources.search;

import org.nrg.xdat.security.ElementSecurity;
import org.nrg.xft.XFTTable;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

import java.util.Hashtable;

public class SearchElementListResource extends SecureResource {
	public SearchElementListResource(Context context, Request request, Response response) {
		super(context, request, response);
		
			this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
			this.getVariants().add(new Variant(MediaType.TEXT_HTML));
			this.getVariants().add(new Variant(MediaType.TEXT_XML));
			
	}

	@Override
	public Representation getRepresentation(Variant variant) {	
		Hashtable<String,Object> params=new Hashtable<String,Object>();
		params.put("title", "Data-Types");    
		
		

		XFTTable fields = new XFTTable();
		fields.initTable(new String[]{"ELEMENT_NAME","SINGULAR","PLURAL","SECURED","COUNT"});
		
		try {
			Hashtable<String,ElementSecurity> allES = (Hashtable<String,ElementSecurity>) ElementSecurity.GetElementSecurities().clone();
			
			//remove security elements
			for(ElementSecurity es: ((Hashtable<String,ElementSecurity>)allES.clone()).values()){
				if(es.getElementName().startsWith("xdat:")){
					allES.remove(es.getElementName());
				}
			}
			
			//remove unwanted elements
			if(this.getQueryVariable("secured")!=null){
				for(ElementSecurity es: ((Hashtable<String,ElementSecurity>)allES.clone()).values()){
					if(!es.isSecure()){
						allES.remove(es.getElementName());
					}
				}
			}
			
			Hashtable<String,Long> counts = null;
				
			if(this.getQueryVariable("readable")!=null){
				counts=user.getReadableCounts();
			}else{		
				counts=user.getTotalCounts();
			}

			if(this.getQueryVariable("used")!=null){
				for(ElementSecurity es: ((Hashtable<String,ElementSecurity>)allES.clone()).values()){
					if(!counts.containsKey(es.getElementName())){
						allES.remove(es.getElementName());
					}
				}
			}
			
			for(ElementSecurity es: allES.values()){
				Object[] sub = new Object[5];
				sub[0]=es.getElementName();
			
				String singular = es.getSingularDescription();
				if(singular==null){
					sub[1]=es.getElementName();
				}else{
					sub[1]=singular;
				}
			
				String plural = es.getPluralDescription();
				if(plural==null){
					sub[2]=es.getElementName();
				}else{
					sub[2]=plural;
				}
				
				if(es.isSecure()){
					sub[3]="true";
				}else{
					sub[3]="false";
				}
				
				if(counts.containsKey(es.getElementName())){
					sub[4]=counts.get(es.getElementName());
				}else{
					sub[4]=new Long(0);
				}
				
				fields.rows().add(sub);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		MediaType mt = overrideVariant(variant);
		
		return this.representTable(fields, mt, params);
	}

}
