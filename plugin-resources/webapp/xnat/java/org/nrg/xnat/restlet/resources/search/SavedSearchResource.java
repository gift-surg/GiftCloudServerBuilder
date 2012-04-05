// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources.search;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.nrg.xdat.om.XdatStoredSearch;
import org.nrg.xdat.om.XdatStoredSearchAllowedUser;
import org.nrg.xdat.om.XdatStoredSearchGroupid;
import org.nrg.xdat.search.DisplaySearch;
import org.nrg.xdat.security.SecurityManager;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTable;
import org.nrg.xft.db.DBAction;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.schema.Wrappers.XMLWrapper.SAXReader;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xnat.restlet.presentation.RESTHTMLPresenter;
import org.nrg.xnat.restlet.representations.ItemXMLRepresentation;
import org.nrg.xnat.restlet.resources.ItemResource;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.xml.sax.SAXException;

import com.noelios.restlet.ext.servlet.ServletCall;

public class SavedSearchResource extends ItemResource {
	XdatStoredSearch xss = null;
	String sID=null;

	public SavedSearchResource(Context context, Request request,
			Response response) {
		super(context, request, response);
		
			sID= (String)getParameter(request,"SEARCH_ID");
			if(sID!=null){
				this.getVariants().add(new Variant(MediaType.TEXT_XML));
			}else{
				response.setStatus(Status.CLIENT_ERROR_GONE);
			}
		}
	
	@Override
	public Representation getRepresentation(Variant variant) {	
		MediaType mt = overrideVariant(variant);

		if(xss==null && sID!=null){
			if(sID.startsWith("@")){
				try {
					String dv = this.getQueryVariable("dv");
					if(dv==null){
						dv="listing";
					}
					DisplaySearch ds = new DisplaySearch();
					ds.setUser(user);
					ds.setDisplay(dv);
					ds.setRootElement(sID.substring(1));
					xss=ds.convertToStoredSearch(sID);
					xss.setId(sID);
				} catch (XFTInitException e) {
					e.printStackTrace();
				} catch (ElementNotFoundException e) {
					e.printStackTrace();
				}
			}else{
				xss= XdatStoredSearch.getXdatStoredSearchsById(sID, user, true);
			}
		}
		
		if(xss!=null){
			if(filepath !=null && filepath.startsWith("results")){
				try {
					DisplaySearch ds=xss.getDisplaySearch(user);
					String sortBy = this.getQueryVariable("sortBy");
					String sortOrder = this.getQueryVariable("sortOrder");
					if (sortBy != null){
					    ds.setSortBy(sortBy);
					    if(sortOrder != null)
					    {
					        ds.setSortOrder(sortOrder);
					    }
					}
					
					MaterializedView mv=null;
						
					if(xss.getId()!=null && !xss.getId().equals("")){
						mv = MaterializedView.GetMaterializedViewBySearchID(xss.getId(), user);
					}
					
					if(mv!=null && (xss.getId().startsWith("@") || this.isQueryVariableTrue("refresh"))){
						mv.delete();
						mv=null;
					}

					LinkedHashMap<String,Map<String,String>> cp=SearchResource.setColumnProperties(ds,user,this);
					
					XFTTable table=null;
					if(mv!=null){
						if (mt.equals(SecureResource.APPLICATION_XLIST)){
							table=(XFTTable)ds.execute(new RESTHTMLPresenter(TurbineUtils.GetRelativePath(ServletCall.getRequest(this.getRequest())),this.getCurrentURI(),user,sortBy),user.getLogin());
						}else{
						    table=mv.getData(null, null, null);
						}
					}else{
					    ds.setPagingOn(false);
					    if (mt.equals(SecureResource.APPLICATION_XLIST)){
					    	table=(XFTTable)ds.execute(new RESTHTMLPresenter(TurbineUtils.GetRelativePath(ServletCall.getRequest(this.getRequest())),this.getCurrentURI(),user,sortBy),user.getLogin());
						}else{
							table=(XFTTable)ds.execute(null,user.getLogin());
					    }
					    
					}
					
					Hashtable<String,Object> tableParams=new Hashtable<String,Object>();
					tableParams.put("totalRecords", table.getNumRows());
					
					return this.representTable(table, mt, tableParams,cp);
				} catch (Exception e) {
					e.printStackTrace();
					this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
				}
			}else{
				
	        if (mt.equals(MediaType.TEXT_XML)){
	        	ItemXMLRepresentation rep= new ItemXMLRepresentation(xss.getItem(),MediaType.TEXT_XML);
				if(sID.startsWith("@")){
					rep.setAllowDBAccess(false);
				}
				
				return rep;
			}
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
				
			boolean isNew=false;
			
				if(search.getId()==null || !search.getId().equals(sID)){
					search.setId(sID);
				isNew=true;
			}else{
				XFTItem xss= search.getCurrentDBVersion(false);
				if(xss==null){
					isNew=true;
				}else if(this.isQueryVariableTrue("saveAs")){
					while(xss!=null){
						search.setId(search.getId()+"_1");
						xss= search.getCurrentDBVersion(false);
					}
					isNew=true;
				}
			}
			
			final boolean isPrimary=(search.getTag()!=null && (search.getId().equals(search.getTag() + "_" + search.getRootElementName())));
			
			if(isNew && isPrimary){
				if(!user.canAction("xnat:projectData/ID", search.getTag(), SecurityManager.DELETE)){
					isNew=false;
				}
			}
			
			if(this.isQueryVariableTrue("saveAs")){
				while(search.getAllowedGroups_groupid().size()>0){
					search.removeAllowedGroups_groupid(0);
				}
				
				while(search.getAllowedUser().size()>0){
					search.removeAllowedUser(0);
				}
				}
				
				boolean found=false;
				for(XdatStoredSearchAllowedUser au : search.getAllowedUser()){
					if(au.getLogin().equals(user.getLogin())){
						found=true;
					}
				}
			
			for(XdatStoredSearchGroupid ag : search.getAllowedGroups_groupid()){
				if(user.containsGroup(ag.getGroupid())){
					found=true;
				}
			}
			
			if(!found && !isNew){
				if(search.getTag()!=null && !search.getTag().equals("")){
					if(!user.canEdit("xnat:projectData/ID", search.getTag())){
						this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
						return;
					}else{
						XdatStoredSearchAllowedUser au = new XdatStoredSearchAllowedUser((UserI)user);
						au.setLogin(user.getLogin());
						search.setAllowedUser(au);
					}
				}else{
					this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
					return;
				}					
			}if(isNew && !found){
					XdatStoredSearchAllowedUser au = new XdatStoredSearchAllowedUser((UserI)user);
					au.setLogin(user.getLogin());
					search.setAllowedUser(au);
				}
				
				try {
					SaveItemHelper.unauthorizedSave(search,user, false, true);
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
					XdatStoredSearchGroupid group=null;
					
					for(XdatStoredSearchAllowedUser au : search.getAllowedUser()){
						if(au.getLogin().equals(user.getLogin())){
							mine=au;
							break;
						}
					}
					
					for(XdatStoredSearchGroupid ag : search.getAllowedGroups_groupid()){
						if(user.containsGroup(ag.getGroupid())){
							group=ag;
							break;
						}
					}
					
					boolean processed=false;
					
					if(mine!=null){
						if(search.getAllowedUser().size()>1 || search.getAllowedGroups_groupid().size()>0){
							SaveItemHelper.authorizedDelete(mine.getItem(), user);
						}else{
							SaveItemHelper.authorizedDelete(search.getItem(), user);
						}
					}else if(group!=null){
						if(search.getAllowedUser().size()>0 || search.getAllowedGroups_groupid().size()>1){
							SaveItemHelper.authorizedDelete(group.getItem(), user);
						}else{
							SaveItemHelper.authorizedDelete(search.getItem(), user);
						}
					}else if(user.getGroup("ALL_DATA_ADMIN")!=null){
						SaveItemHelper.authorizedDelete(search.getItem(), user);
					}else{						
						this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
						return;
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
