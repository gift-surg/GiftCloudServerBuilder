// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources.files;

import java.util.Hashtable;

import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatAbstractresourceTag;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xnat.restlet.resources.ScanList;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

public class CatalogResourceList extends XNATTemplate {
	final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ScanList.class);
	
	public CatalogResourceList(Context context, Request request, Response response) {
		super(context, request, response);
		
		if(recons.size()>0 || scans.size()>0 || expts.size()>0 || sub!=null || proj!=null){
				this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
				this.getVariants().add(new Variant(MediaType.TEXT_HTML));
				this.getVariants().add(new Variant(MediaType.TEXT_XML));
			}else{
			response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
		}
	}


	@Override
	public boolean allowPost() {
		return true;
	}
	
	@Override
	public void handlePost() {
	        XFTItem item = null;			

			try {
			item=this.loadItem("xnat:resourceCatalog",true);
				
				if(item==null){
					this.getResponse().setStatus(Status.CLIENT_ERROR_EXPECTATION_FAILED, "Need POST Contents");
					return;
				}
				
				if(item.instanceOf("xnat:resourceCatalog")){
					XnatResourcecatalog catResource = (XnatResourcecatalog)BaseElement.GetGeneratedItem(item);
					
					if(catResource.getXnatAbstractresourceId()!=null){
						XnatAbstractresource existing=XnatAbstractresource.getXnatAbstractresourcesByXnatAbstractresourceId(catResource.getXnatAbstractresourceId(), user, false);
						if(existing!=null){
							this.getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT,"Specified catalog already exists.");
							//MATCHED
							return;
						}else{
							this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY,"Contains erroneous generated fields (xnat_abstractresource_id).");
							//MATCHED
							return;
						}
					}
					
					
				
				if(this.getQueryVariable("description")!=null){
					catResource.setDescription(this.getQueryVariable("description"));
			    }
				
			    if(this.getQueryVariable("format")!=null){
			    	catResource.setFormat(this.getQueryVariable("format"));
			    }
			    
			    if(this.getQueryVariable("content")!=null){
			    	catResource.setContent(this.getQueryVariable("content"));
			    }
			    
			    if(this.getQueryVariables("tags")!=null){
			    	String[] tags = this.getQueryVariables("tags");
			    	for(String tag: tags){
			    		tag = tag.trim();
			    		if(!tag.equals("")){
			    			for(String s:StringUtils.CommaDelimitedStringToArrayList(tag)){
			    				s=s.trim();
			    				if(!s.equals("")){
			    					XnatAbstractresourceTag t = new XnatAbstractresourceTag((UserI)user);
			    		    		if(s.indexOf("=")>-1){
			    		    			t.setName(s.substring(0,s.indexOf("=")));
			    		    			t.setTag(s.substring(s.indexOf("=")+1));
			    		    		}else{
			    		    			if(s.indexOf(":")>-1){
				    		    			t.setName(s.substring(0,s.indexOf(":")));
				    		    			t.setTag(s.substring(s.indexOf(":")+1));
				    		    		}else{
				    		    			t.setTag(s);
				    		    		}
			    		    		}
			    		    		catResource.setTags_tag(t);
			    				}
			    			}
			    			
			    		}
			    	}
			    }
				
					this.insertCatalag(catResource);
					
				
				this.returnSuccessfulCreateFromList(catResource.getXnatAbstractresourceId() + "");
				}else{
					this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY,"Only ResourceCatalog documents can be PUT to this address.");
				}
			} catch (Exception e) {
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e.getMessage());
	            logger.error("",e);
			}
		}


	@Override
	public Representation getRepresentation(Variant variant) {	
			XFTTable table = null;
					
		if(recons.size()>0 || scans.size()>0 || expts.size()>0 || sub!=null || proj!=null){
			try {
				table=this.loadCatalogs(null,false,true);
			} catch (Exception e) {
				logger.error("",e);
			}
		}
					
		if(this.getQueryVariable("file_stats")!=null && this.getQueryVariable("file_stats").equals("true")){
			try {
				if(proj==null){
					if(parent.getItem().instanceOf("xnat:experimentData")){
						proj = ((XnatExperimentdata)parent).getPrimaryProject(false);
						// Per FogBugz 4746, prevent NPE when user doesn't have access to resource (MRH)
						// Check access through shared project when user doesn't have access to primary project
						if (proj == null) {
							proj = (XnatProjectdata)((XnatExperimentdata)parent).getFirstProject();
						}
					}else if(security.getItem().instanceOf("xnat:experimentData")){
						proj = ((XnatExperimentdata)security).getPrimaryProject(false);
						// Per FogBugz 4746, ....
						if (proj == null) {
							proj = (XnatProjectdata)((XnatExperimentdata)security).getFirstProject();
						}
					}else if(security.getItem().instanceOf("xnat:subjectData")){
						proj = ((XnatSubjectdata)security).getPrimaryProject(false);
						// Per FogBugz 4746, ....
						if (proj == null) {
							proj = (XnatProjectdata)((XnatSubjectdata)security).getFirstProject();
						}
					}else if(security.getItem().instanceOf("xnat:projectData")){
						proj = (XnatProjectdata)security;
					}
				}
				
				XFTTable t = new XFTTable();
				String [] fields={"xnat_abstractresource_id","label","element_name","category","cat_id","cat_desc","file_count","file_size","tags","content","format"};
				t.initTable(fields);
				table.resetRowCursor();
				while(table.hasMoreRows() ){
					Object[] old=table.nextRow();
					Object[] _new=new Object[11];
					_new[0]=old[0];
					_new[1]=old[1];
					_new[2]=old[2];
					_new[3]=old[3];
					_new[4]=old[4];
					_new[5]=old[5];
					
					XnatAbstractresource res= XnatAbstractresource.getXnatAbstractresourcesByXnatAbstractresourceId(old[0], user, false);
					// Per FogBugz 4746, prevent NPE when user doesn't have access to resource (MRH)
					// Just in case project is still inaccessable....
					// NOTE: Accessing getCount() and getSize() through shared project returns correct results, however it would not be 
					// expected to if there were ever separate archives for each project
					if (proj!=null) {
						_new[6]=res.getCount(proj.getRootArchivePath());
						_new[7]=res.getSize(proj.getRootArchivePath());
					} else {
						_new[6]="UNKNOWN";
						_new[7]="UNKNOWN";
					} 
					_new[8]=res.getTagString();
					_new[9]=res.getContent();
					_new[10]=res.getFormat();
				
					t.rows().add(_new);
				}

				table=null;
				table=t;
			} catch (ElementNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		Hashtable<String,Object> params=new Hashtable<String,Object>();
		params.put("title", "Resources");

		MediaType mt = overrideVariant(variant);

		if(table!=null)params.put("totalRecords", table.size());
		return this.representTable(table, mt, params);
	}
}