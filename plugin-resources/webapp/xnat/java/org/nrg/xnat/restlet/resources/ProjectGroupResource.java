/*
 * org.nrg.xnat.restlet.resources.ProjectMemberResource
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 5/7/14 4:26 PM
 */
package org.nrg.xnat.restlet.resources;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.NumberUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.nrg.xdat.om.XdatElementAccess;
import org.nrg.xdat.om.XdatFieldMapping;
import org.nrg.xdat.om.XdatFieldMappingSet;
import org.nrg.xdat.om.XdatUsergroup;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.ElementSecurity;
import org.nrg.xdat.security.PermissionCriteria;
import org.nrg.xdat.security.PermissionItem;
import org.nrg.xdat.security.UserGroup;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.ModifyGroupPrivileges;
import org.nrg.xdat.turbine.utils.PopulateItem;
import org.nrg.xft.XFT;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTable;
import org.nrg.xft.collections.ItemCollection;
import org.nrg.xft.event.Event;
import org.nrg.xft.event.EventManager;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.InvalidItemException;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.search.ItemSearch;
import org.nrg.xft.utils.SaveItemHelper;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ProjectGroupResource extends SecureResource {
    public static Logger logger = Logger.getLogger(ProjectGroupResource.class);
	public class InvalidValueException extends Exception {

	}

	XnatProjectdata proj=null;
	XdatUsergroup group=null;
	ArrayList<XDATUser> newUsers= new ArrayList<XDATUser>();
	ArrayList<String> unknown= new ArrayList<String>();
	String gID=null;
    boolean displayHiddenUsers = false;
	
	public ProjectGroupResource(Context context, Request request, Response response) {
		super(context, request, response);

		this.getVariants().add(new Variant(MediaType.TEXT_XML));
		this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
		this.getVariants().add(new Variant(MediaType.TEXT_HTML));
		
		String pID= (String)getParameter(request,"PROJECT_ID");
		if(pID!=null){
			proj = XnatProjectdata.getProjectByIDorAlias(pID, user, false);
		} 
	
		gID =(String)getParameter(request,"GROUP_ID");
		if(StringUtils.isNotEmpty(gID)){
			CriteriaCollection cc = new CriteriaCollection("OR");
			if(NumberUtils.isNumber(gID)){
				cc.addClause("xdat:userGroup/xdat_userGroup_id", gID);
			}
			cc.addClause("xdat:userGroup/ID", gID);
			cc.addClause("xdat:userGroup/ID", pID + "_" +gID);
			CriteriaCollection subCC = new CriteriaCollection("AND");
			subCC.addClause("xdat:userGroup/tag", pID);
			subCC.addClause("xdat:userGroup/displayName", gID);
			cc.addClause(subCC);
			
			try {
				ItemCollection items= ItemSearch.GetItems(cc, user, false);
				if(items.size()>1){
					getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,"Duplicate groups matched.");
					return;
				}
				if (items.getFirst()!=null){
					group = new XdatUsergroup(items.getFirst());
				}
			} catch (Exception e) {
				logger.error("",e);
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			}
			
		}
	}

	@Override
	public boolean allowPut() {
		return true;
	}

	@Override
	public boolean allowPost() {
		return true;
	}

	@Override
	public boolean allowDelete() {
		return true;
	}
	
	private static List<String> protected_displayNames=Lists.newArrayList("Owners", "Members","Collaborators");
	
	@Override
	public void handleDelete() {
		if(proj==null || group==null){
			getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
			return;
		}else if(protected_displayNames.contains(group.getDisplayname())){
			getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
			return;
		}else{
			try {
				if(user.canDelete(proj)) {
					SaveItemHelper.authorizedDelete(group.getItem(), user, newEventInstance(EventUtils.CATEGORY.PROJECT_ACCESS, "Removed user group."));
					group=null;
				}else{
					getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
				}
			} catch (InvalidItemException e) {
				logger.error("",e);
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			} catch (Exception e) {
				logger.error("",e);
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			}
		}
		returnDefaultRepresentation();
	}
	
	public void handlePost(){
		handlePut();
	}

	@Override
	public void handlePut() {
		if(proj==null){
			getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
		}else{
			try {
				if (user.canDelete(proj)) {
					boolean isNew = false;

					Map<String, Object> props = Maps.newHashMap();

					props.putAll(getQueryVariablesAsMap());

					props.putAll(getBodyVariableMap());

					try {
						//populate object from passed parameters
						PopulateItem populator = new PopulateItem(props, user, XdatUsergroup.SCHEMA_ELEMENT_NAME, true, (group == null) ? XFTItem.NewItem(XdatUsergroup.SCHEMA_ELEMENT_NAME, user) : group.getItem());
						XdatUsergroup tempGroup = new XdatUsergroup(populator.getItem());

						//tag must be for this project
						if (!StringUtils.equals(proj.getId(), tempGroup.getTag())) {
							tempGroup.setTag(proj.getId());
						}

						//display name is required
						if (StringUtils.isEmpty(tempGroup.getDisplayname())) {
							getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Display name is required.");
							return;
						}

						//set ID to the standard value
						if (StringUtils.isEmpty(tempGroup.getId())) {
							tempGroup.setId(proj.getId() + "_" + tempGroup.getDisplayname());
						}


						final List<ElementSecurity> elements = ElementSecurity.GetSecureElements();

						final UserGroup ug = new UserGroup(tempGroup.getId());
						ug.init(tempGroup);
						for (ElementSecurity es : elements) {
							final List<String> permissionItems = es.getPrimarySecurityFields();
							for (String securityField : permissionItems) {
								final PermissionCriteria pc = new PermissionCriteria();
								pc.setField(securityField);
								pc.setFieldValue(ug.getTag());
								final String s = es.getElementName() + "_" + securityField + "_" + ug.getTag();
								if (props.get(s + "_R") != null) {
									pc.setRead(true);
								} else {
									pc.setRead(false);
								}
								if (props.get(s + "_E") != null && !StringUtils.equals(es.getElementName(), XnatProjectdata.SCHEMA_ELEMENT_NAME)) {
									pc.setRead(true);
									pc.setEdit(true);
									pc.setCreate(true);
									pc.setActivate(true);
								} else {
									pc.setCreate(false);
									pc.setEdit(false);
									pc.setActivate(false);
								}
								if (props.get(s + "_D") != null && !StringUtils.equals(es.getElementName(), XnatProjectdata.SCHEMA_ELEMENT_NAME)) {
									pc.setRead(true);
									pc.setDelete(true);
								} else {
									pc.setDelete(false);
								}

								pc.setComparisonType("equals");

								final String wasSet = (String) props.get(s + "_wasSet");

								if ((wasSet != null && wasSet.equals("1")) || pc.getCreate() || pc.getRead() || pc.getEdit() || pc.getDelete() || pc.getActivate()) {
									tempGroup.addRootPermission(es.getElementName(), pc);

									if (StringUtils.equals(pc.getField(), es.getElementName() + "/project")) {
										//inherit project permissions to shared project permissions
										if ((wasSet != null && wasSet.equals("1")) || pc.getRead()) {

											final PermissionCriteria share = new PermissionCriteria();
											share.setField(es.getElementName() + "/sharing/share/project");
											share.setFieldValue(ug.getTag());
											share.setRead(pc.getRead());
											share.setComparisonType("equals");
											tempGroup.addRootPermission(es.getElementName(), share);
										}
									}
								}
							}
						}

						//verify that the user isn't trying to gain access to other projects.
						for (XdatElementAccess ea : tempGroup.getElementAccess()) {
							for (XdatFieldMappingSet set : ea.getPermissions_allowSet()) {
								try {
									verifyGroupPermissions(set, proj.getId());
								} catch (InvalidValueException e) {
									getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
									return;
								}
							}
						}

						if (group == null) {
							//create new group
							isNew = true;
						}

						group = tempGroup;

						//because we've checked the values in the permissions, we can use the pre-authorized save method
						SaveItemHelper.authorizedSave(tempGroup, user, false, true, newEventInstance(EventUtils.CATEGORY.PROJECT_ACCESS, (isNew) ? "Added user group" : "Modified user group."));
					} catch (Exception e) {
						logger.error("", e);
						getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e);
						return;
					}

					try {
						EventManager.Trigger(XdatUsergroup.SCHEMA_ELEMENT_NAME, group.getId(), Event.UPDATE);
						user.init();
					} catch (Exception e1) {
						logger.error("", e1);
					}

					if (props.containsKey("src")) {
						this.getResponse().setStatus(Status.REDIRECTION_SEE_OTHER);
						this.getResponse().redirectSeeOther(XFT.GetSiteURL() + "/data/projects/" + group.getTag() + "?format=html");
					} else {
						returnDefaultRepresentation();
					}
				} else {
					getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
					return;
				}
			} catch (Exception e) {
				logger.error("", e);
			}
		}
	}
	
	
	/**
	 * This checks for hacking attempts.  The user cannot add permissions to this group for other projects.
	 * @param set
	 * @param tag
	 * @throws InvalidValueException
	 */
	private void verifyGroupPermissions(XdatFieldMappingSet set, String tag) throws InvalidValueException{
		for(XdatFieldMapping map: set.getAllow()){
			if(! StringUtils.equals((String)map.getFieldValue(),proj.getId())){
				throw new InvalidValueException();
			}
		}
		
		for(XdatFieldMappingSet subset: set.getSubSet()){
			verifyGroupPermissions(subset, tag);
		}
	}

	@Override
	public Representation getRepresentation(Variant variant) {	
		XFTTable table=null;
		if(proj!=null){
			if(group==null){
				//return a list of groups
				try {
	                StringBuffer query = new StringBuffer("SELECT ug.id, ug.displayname,ug.tag,ug.xdat_usergroup_id, COUNT(map.groups_groupid_xdat_user_xdat_user_id) AS users FROM xdat_userGroup ug LEFT JOIN xdat_user_groupid map ON ug.id=map.groupid WHERE tag='").append(proj.getId()).append("' ");
	                query.append(" GROUP BY ug.id, ug.displayname,ug.tag,ug.xdat_usergroup_id  ORDER BY ug.displayname DESC;");
	                table = XFTTable.Execute(query.toString(), user.getDBName(), user.getLogin());
	                
	                Hashtable<String,Object> params=new Hashtable<String,Object>();
	        		params.put("title", "Projects");
	        		
	        		if(table!=null)params.put("totalRecords", table.size());
	        		return this.representTable(table, overrideVariant(variant), params);
				} catch (SQLException e) {
					logger.error("",e);
					getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
					return null;
				} catch (DBPoolException e) {
					logger.error("",e);
					getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
					return null;
				}
			}else{
				//return a particular group
				return this.representItem(group.getItem(), overrideVariant(variant));
			}
		}else{
			getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
			return null;
		}
	}
}