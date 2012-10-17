// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.VelocityContext;
import org.nrg.action.ActionException;
import org.nrg.xdat.om.XdatUser;
import org.nrg.xdat.om.XdatUsergroup;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTTable;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.InvalidItemException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.search.ItemSearch;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xnat.turbine.utils.ProjectAccessRequest;
import org.nrg.xnat.utils.WorkflowUtils;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

import com.noelios.restlet.ext.servlet.ServletCall;

public class ProjectMemberResource extends SecureResource {
	XnatProjectdata proj=null;
	XdatUsergroup group=null;
	ArrayList<XDATUser> newUsers= new ArrayList<XDATUser>();
	ArrayList<String> unknown= new ArrayList<String>();
	String gID=null; 
	
	public ProjectMemberResource(Context context, Request request, Response response) {
		super(context, request, response);
		
			this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
			this.getVariants().add(new Variant(MediaType.TEXT_HTML));
			this.getVariants().add(new Variant(MediaType.TEXT_XML));
			
			String pID= (String)getParameter(request,"PROJECT_ID");
			if(pID!=null){
				proj = XnatProjectdata.getProjectByIDorAlias(pID, user, false);
			}
		
			gID =(String)getParameter(request,"GROUP_ID");
			CriteriaCollection cc = new CriteriaCollection("OR");
			cc.addClause("xdat:userGroup/ID", gID);
			cc.addClause("xdat:userGroup/ID", pID + "_" +gID);
			CriteriaCollection subCC = new CriteriaCollection("AND");
			subCC.addClause("xdat:userGroup/tag", pID);
			subCC.addClause("xdat:userGroup/displayName", gID);
			cc.addClause(subCC);
			
			try {
				ItemI gItem = ItemSearch.GetItems(cc, user, false).getFirst();
				if (gItem!=null){
					group = new XdatUsergroup(gItem);
				}
			} catch (Exception e) {
				logger.error("",e);
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			}
			
			

			String tempValue =(String)getParameter(request,"USER_ID");
			try {
				String[] ids=null;
				if(tempValue.indexOf(",")>-1){
					ids=tempValue.split(",");
				}else{
					ids=new String[]{tempValue};
				}
				
				for(int i=0;i<ids.length;i++){
					String uID=ids[i].trim();
					Integer xdat_user_id= null;
					try {
						xdat_user_id=Integer.parseInt(uID);
					} catch (NumberFormatException e) {
						
					}
					
					
					if (xdat_user_id==null){
						//login or email
						XDATUser newUser=null;
						try {
							newUser = new XDATUser(uID);
						} catch (XDATUser.UserNotFoundException e) {
						}
						if (newUser==null){
							//by email
							ArrayList<ItemI> items =ItemSearch.GetItems("xdat:user/email", uID, user, false).items();
							if(items.size()>0){
								for(ItemI temp: items){
									newUsers.add(new XDATUser(temp));
								}
							}else{
								unknown.add(uID);
							}
						}else{
							newUsers.add(newUser);
						}
					}else{
						XdatUser tempUser = XdatUser.getXdatUsersByXdatUserId(xdat_user_id, user, false);
						if (tempUser!=null){
							newUsers.add(new XDATUser(tempUser.getItem()));
						}
					}
				}
			} catch (XFTInitException e) {
				logger.error("",e);
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			} catch (ElementNotFoundException e) {
				logger.error("",e);
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			} catch (DBPoolException e) {
				logger.error("",e);
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			} catch (SQLException e) {
				logger.error("",e);
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			} catch (FieldNotFoundException e) {
				logger.error("",e);
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			} catch (Exception e) {
				logger.error("",e);
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			}

		}

	@Override
	public boolean allowPut() {
		return true;
	}

	@Override
	public boolean allowDelete() {
		return true;
	}
	
	@Override
	public void handleDelete() {
		if(proj==null || group==null || newUsers.size()==0){
			getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
		}else{
			try {
				if(user.canDelete(proj)){
					try {
						for(XDATUser newUser: newUsers){
						    proj.removeGroupMember(group.getId(), newUser, user,newEventInstance(EventUtils.CATEGORY.PROJECT_ACCESS, EventUtils.REMOVE_USER_TO_PROJECT + " (" + newUser.getLogin() + ")"));													
						}
					} catch (Exception e) {
						logger.error("",e);
					}
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

	@Override
	public void handlePut() {
		HttpServletRequest request = ServletCall.getRequest(getRequest());
		if(proj==null || group==null){
			getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
		}else{
			try {
				if(user.canDelete(proj)){
					if (unknown.size()>0){
						//NEW USER                        
                        try {
							for(String uID : unknown){
								VelocityContext context = new VelocityContext();
								context.put("user",user);
							    context.put("server",TurbineUtils.GetFullServerPath(request));
							    context.put("process","Transfer to the archive.");
							    context.put("system",TurbineUtils.GetSystemName());
							    context.put("access_level",gID);
							    context.put("admin_email",AdminUtils.getAdminEmailId());
							    context.put("projectOM",proj);
							    //SEND email to user
							    final PersistentWorkflowI wrk=PersistentWorkflowUtils.getOrCreateWorkflowData(null, user, proj.SCHEMA_ELEMENT_NAME,proj.getId(),proj.getId(),newEventInstance(EventUtils.CATEGORY.PROJECT_ACCESS, EventUtils.INVITE_USER_TO_PROJECT + " (" + uID + ")"));
						    	try {
									ProjectAccessRequest.InviteUser(context, uID, user, user.getFirstname() + " " + user.getLastname() + " has invited you to join the " + proj.getName() + " project.");
									WorkflowUtils.complete(wrk, wrk.buildEvent());
								} catch (Exception e) {
									WorkflowUtils.fail(wrk, wrk.buildEvent());
									logger.error("",e);
								}
							}
						} catch (Throwable e) {
							logger.error("",e);
						}
					}
					
					if (newUsers.size()>0){
						//CURRENT USER

						String email=(this.isQueryVariableTrue("sendemail"))?"true":"false";
						
			            
						boolean sendmail=Boolean.parseBoolean(email);
						
						for(XDATUser newUser: newUsers){
							final PersistentWorkflowI wrk=PersistentWorkflowUtils.getOrCreateWorkflowData(null, user, user.getXSIType(),user.getXdatUserId().toString(),proj.getId(),newEventInstance(EventUtils.CATEGORY.PROJECT_ACCESS, EventUtils.ADD_USER_TO_PROJECT));
					    	EventMetaI c=wrk.buildEvent();
							
								try {
									proj.addGroupMember(group.getId(), newUser, user,WorkflowUtils.setStep(wrk, "Add " + newUser.getLogin()));
									WorkflowUtils.complete(wrk, c);
									
									if (sendmail){
										try {
											VelocityContext context = new VelocityContext();
											
											context.put("user",user);
											context.put("server",TurbineUtils.GetFullServerPath(request));
											context.put("process","Transfer to the archive.");
											context.put("system",TurbineUtils.GetSystemName());
											context.put("access_level","member");
											context.put("admin_email",AdminUtils.getAdminEmailId());
											context.put("projectOM",proj);
											org.nrg.xnat.turbine.modules.actions.ProcessAccessRequest.SendAccessApprovalEmail(context, newUser.getEmail(), user, TurbineUtils.GetSystemName() + " Access Granted for " + proj.getName());
										} catch (Throwable e) {
											logger.error("",e);
										}
									}
								} catch (Exception e) {
									throw e;
								}
							}
					}
				}else{
					getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
				}
			} catch (InvalidItemException e) {
				logger.error("",e);
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			} catch (ActionException e) {
				getResponse().setStatus(e.getStatus());
				return;
			} catch (Exception e) {
				logger.error("",e);
					getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			}
			returnDefaultRepresentation();
		}
	}

	@Override
	public Representation getRepresentation(Variant variant) {	
		XFTTable table=null;
		if(proj!=null){
			try {
				String query = "SELECT g.id AS \"GROUP_ID\", displayname,login,firstname,lastname,email FROM xdat_userGroup g RIGHT JOIN xdat_user_Groupid map ON g.id=map.groupid RIGHT JOIN xdat_user u ON map.groups_groupid_xdat_user_xdat_user_id=u.xdat_user_id  WHERE tag='" + proj.getId() + "' ORDER BY g.id DESC;";
				table = XFTTable.Execute(query, user.getDBName(), user.getLogin());
			} catch (SQLException e) {
				logger.error("",e);
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			} catch (DBPoolException e) {
				logger.error("",e);
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			}
		}
		
		Hashtable<String,Object> params=new Hashtable<String,Object>();
		params.put("title", "Projects");

		MediaType mt = overrideVariant(variant);
		
		if(table!=null)params.put("totalRecords", table.size());
		return this.representTable(table, mt, params);
	}
}
