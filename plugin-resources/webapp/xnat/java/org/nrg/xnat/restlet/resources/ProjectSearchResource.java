// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.util.ArrayList;

import org.nrg.xdat.om.XdatStoredSearch;
import org.nrg.xdat.om.XdatStoredSearchAllowedUser;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xft.XFTItem;
import org.nrg.xft.db.DBAction;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.schema.Wrappers.XMLWrapper.SAXReader;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.restlet.representations.ItemXMLRepresentation;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.xml.sax.SAXException;

public class ProjectSearchResource extends ItemResource {
	XdatStoredSearch xss = null;
	String sID=null;
	XnatProjectdata proj=null;
	
	public ProjectSearchResource(Context context, Request request, Response response) {
		super(context, request, response);
		
			sID= (String)request.getAttributes().get("SEARCH_ID");
			if(sID!=null){		
				
				String pID= (String)request.getAttributes().get("PROJECT_ID");
				if(pID!=null){
					proj = XnatProjectdata.getProjectByIDorAlias(pID, user, false);
					
					if(proj!=null){
						this.getVariants().add(new Variant(MediaType.TEXT_XML));				
					}else{
					response.setStatus(Status.CLIENT_ERROR_NOT_FOUND,
							"Unable to find the specified project.");
					}
				}else{
				response.setStatus(Status.CLIENT_ERROR_NOT_FOUND,
						"Unable to find the specified project");
				}
			}else{
			response.setStatus(Status.CLIENT_ERROR_NOT_FOUND,
					"Unable to find the specified project");
		}
	}
	
	@Override
	public Representation getRepresentation(Variant variant) {	
		MediaType mt = overrideVariant(variant);

		if(xss==null && sID!=null){
			if(sID.startsWith("@")){
				xss=proj.getDefaultSearch(sID.substring(1));
			}else{
				xss= XdatStoredSearch.getXdatStoredSearchsById(sID, user, true);
			}
		}
		
		if(xss!=null){
	        if (mt.equals(MediaType.TEXT_XML)){
	        	ItemXMLRepresentation rep= new ItemXMLRepresentation(xss.getItem(),MediaType.TEXT_XML);
				if(sID.startsWith("@")){
					rep.setAllowDBAccess(false);
				}
				
				return rep;
			}
//	        else if (mt.equals(MediaType.APPLICATION_JSON)){
//				return new JSONTableRepresentation(item,params,MediaType.APPLICATION_JSON);
//			}else{
//				return new HTMLTableRepresentation(item,params,MediaType.TEXT_HTML);
//			}
		}

		return null;

	}

	@Override
	public boolean allowDelete() {
		return true;
	}

	@Override
	public boolean allowPut() {
		return true;
	}

	@Override
	public void handlePut() {
			try {
				Reader sax=this.getRequest().getEntity().getReader();
				
				SAXReader reader = new SAXReader(user);
				XFTItem item = reader.parse(sax);
				
				if(!item.instanceOf("xdat:stored_search")){
					this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY);
					return;
				}
				XdatStoredSearch search = new XdatStoredSearch(item);
				
				if(search.getId()==null || !search.getId().equals(sID)){
					search.setId(sID);
				}
				
				boolean found=false;
				for(XdatStoredSearchAllowedUser au : search.getAllowedUser()){
					if(au.getLogin().equals(user.getLogin())){
						found=true;
					}
				}
				if(!found){
					XdatStoredSearchAllowedUser au = new XdatStoredSearchAllowedUser((UserI)user);
					au.setLogin(user.getLogin());
					search.setAllowedUser(au);
				}
				
				try {
					search.save(user, false, true);
				} catch (DBPoolException e) {
					e.printStackTrace();
					this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
				} catch (SQLException e) {
					e.printStackTrace();
					this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
				} catch (Exception e) {
					e.printStackTrace();
					this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
				}
					
//				try {
//					MaterializedView.DeleteBySearchID(search.getId(), user);
//				} catch (Throwable e) {
//					e.printStackTrace();
//				}
			} catch (IOException e) {
				e.printStackTrace();
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			} catch (SAXException e) {
				e.printStackTrace();
				this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY);
			} catch (ElementNotFoundException e) {
				e.printStackTrace();
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			} catch (Exception e) {
				e.printStackTrace();
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			} 
		}
	


	@Override
	public void handleDelete() {
		if(sID!=null){
			try {
				XdatStoredSearch search = XdatStoredSearch.getXdatStoredSearchsById(sID, user, false);
				
				if(search!=null){
					XdatStoredSearchAllowedUser mine=null;
					for(XdatStoredSearchAllowedUser au : search.getAllowedUser()){
						if(au.getLogin().equals(user.getLogin())){
							mine=au;
							break;
						}
					}
					
					if(mine!=null){
						if(search.getAllowedUser().size()>1 || search.getAllowedGroups_groupid().size()>0){
							DBAction.DeleteItem(mine.getItem(), user);
						}else{
							DBAction.DeleteItem(search.getItem(), user);
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			} catch (SAXException e) {
				e.printStackTrace();
				this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY);
			} catch (ElementNotFoundException e) {
				e.printStackTrace();
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			} catch (Exception e) {
				e.printStackTrace();
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			} 
		}
	}
}
