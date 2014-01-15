/*
 * org.nrg.xnat.restlet.resources.ProjectResource
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:40 PM
 */
package org.nrg.xnat.restlet.resources;

import java.util.List;

import org.nrg.xdat.om.ArcProject;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.base.BaseXnatProjectdata;
import org.nrg.xft.XFT;
import org.nrg.xft.XFTItem;
import org.nrg.xft.db.PoolDBUtils;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.exception.InvalidItemException;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xnat.helpers.xmlpath.XMLPathShortcuts;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.nrg.xnat.utils.WorkflowUtils;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

public class ProjectResource extends ItemResource {
    static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ProjectResource.class);
    
	XnatProjectdata proj=null;
	String pID=null;
	public ProjectResource(Context context, Request request, Response response) {
		super(context, request, response);
		
			pID= (String)getParameter(request,"PROJECT_ID");
			if(pID!=null){
				proj = XnatProjectdata.getProjectByIDorAlias(pID, user, false);
			}


			if(proj!=null){
				this.getVariants().add(new Variant(MediaType.TEXT_HTML));
				this.getVariants().add(new Variant(MediaType.TEXT_XML));
			}
		
			this.fieldMapping.putAll(XMLPathShortcuts.getInstance().getShortcuts(XMLPathShortcuts.PROJECT_DATA,false));
		
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
	public void handleDelete(){
		if(filepath!=null && !filepath.equals("")){
			this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			return;
		}
		
		if(proj!=null){
		    try {
				final boolean removeFiles=this.isQueryVariableTrue("removeFiles");
				if(user.canDelete(proj)){
					final PersistentWorkflowI workflow=WorkflowUtils.getOrCreateWorkflowData(getEventId(), user, XnatProjectdata.SCHEMA_ELEMENT_NAME, this.proj.getId(),this.proj.getId(),newEventInstance(EventUtils.CATEGORY.PROJECT_ADMIN, EventUtils.getDeleteAction(proj.getXSIType())));
					final EventMetaI ci=workflow.buildEvent();
			    	
					try {
						this.proj.delete(removeFiles, user,ci);
						PersistentWorkflowUtils.complete(workflow, ci);
					} catch (Exception e) {
						logger.error("",e);
						PersistentWorkflowUtils.fail(workflow, ci);
						throw e;
					}
				}else{
					this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"User account doesn't have permission to delete this project.");
					return;
				}
			} catch (InvalidItemException e) {
				logger.error("",e);
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			} catch (Exception e) {
				logger.error("",e);
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			}
		}
	}



	@Override
	public void handlePut() {
		XFTItem item = null;

		try {
			if(this.proj==null || user.canEdit(this.proj)){
				item=this.loadItem("xnat:projectData",true);

				if(item==null){
					String xsiType=this.getQueryVariable("xsiType");
					if(xsiType!=null){
						item=XFTItem.NewItem(xsiType, user);
					}
				}

				if(item==null){
					if(proj!=null){
						item=proj.getItem();
					}
				}

				if(item==null){
					this.getResponse().setStatus(Status.CLIENT_ERROR_EXPECTATION_FAILED, "Need PUT Contents");
					return;
				}

				boolean allowDataDeletion =false;
				if(this.getQueryVariable("allowDataDeletion")!=null && this.getQueryVariable("allowDataDeletion").equalsIgnoreCase("true")){
					allowDataDeletion =true;
				}

				if(item.instanceOf("xnat:projectData")){
					XnatProjectdata project = new XnatProjectdata(item);

					if(filepath!=null && !filepath.equals("")){
						if(project.getId()==null){
							item = proj.getItem();
							project=proj;
						}

						if(!user.canEdit(item)){
							this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"User account doesn't have permission to edit this project.");
							return;
						}
						if(filepath.startsWith("quarantine_code/")){
							String qc= filepath.substring(16);
							if(!qc.equals("")){
								ArcProject ap =project.getArcSpecification();
								try {
									Integer qcI = Integer.valueOf(qc);
									ap.setQuarantineCode(qcI);
								} catch (NumberFormatException e) {
									if(qc.equals("true")){
										ap.setQuarantineCode(new Integer(1));
									}else if(qc.equals("false")){
										ap.setQuarantineCode(new Integer(0));
									}else{
										this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,"Prearchive code must be an integer.");
										return;
									}
								}

								create(project,ap,false,false,newEventInstance(EventUtils.CATEGORY.PROJECT_ADMIN,"Configured quarantine code"));
								ArcSpecManager.Reset();
							}
						}else if(filepath.startsWith("prearchive_code/")){
							String qc= filepath.substring(16);
							if(!qc.equals("")){
								ArcProject ap =project.getArcSpecification();
								try {
									Integer qcI = Integer.valueOf(qc);
									ap.setPrearchiveCode(qcI);
								} catch (NumberFormatException e) {
									if(qc.equals("true")){
										ap.setPrearchiveCode(new Integer(1));
									}else if(qc.equals("false")){
										ap.setPrearchiveCode(new Integer(0));
									}else{
										this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,"Prearchive code must be an integer.");
										return;
									}
								}
								create(project,ap,false,false,newEventInstance(EventUtils.CATEGORY.PROJECT_ADMIN,"Configured prearchive code"));
								ArcSpecManager.Reset();
							}
						}else if(filepath.startsWith("current_arc/")){
							String qc= filepath.substring(12);
							if(!qc.equals("")){
								ArcProject ap =project.getArcSpecification();
								ap.setCurrentArc(qc);

								create(project,ap,false,false,newEventInstance(EventUtils.CATEGORY.PROJECT_ADMIN,"Configured current arc"));
								ArcSpecManager.Reset();
							}
						}else{
							this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
							return;
						}
					}else{
						if(StringUtils.IsEmpty(project.getId())){
							project.setId(this.pID);
						}

						if(StringUtils.IsEmpty(project.getId())){
							this.getResponse().setStatus(Status.CLIENT_ERROR_EXPECTATION_FAILED,"Requires XNAT ProjectData ID");
							return;
						}

						if(!StringUtils.IsAlphaNumericUnderscore(project.getId()) && !this.isQueryVariableTrue("testHyphen") ){
							this.getResponse().setStatus(Status.CLIENT_ERROR_EXPECTATION_FAILED,"Invalid character in project ID.");
							return;
						}

						if(item.getCurrentDBVersion()!=null){
							if(!user.canEdit(item)){
								this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"User account doesn't have permission to edit this project.");
								return;
							}
						}else{
			                Long count=(Long)PoolDBUtils.ReturnStatisticQuery("SELECT COUNT(ID) FROM xnat_projectdata_history WHERE ID='"+project.getId()+"';", "COUNT", null, null);
			                if(count>0){
			                   this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "Project '"+project.getId() + "' was used in a previously deleted project and cannot be reused.");
			                   return;
			                }
			            }
						
						if(XFT.getBooleanProperty("UI.allow-non-admin-project-creation", true) || user.isSiteAdmin()){
							project.preSave();
							BaseXnatProjectdata.createProject(project, user, allowDataDeletion,true,newEventInstance(EventUtils.CATEGORY.PROJECT_ADMIN),getQueryVariable("accessibility"));
						}else{
							this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"User account doesn't have permission to edit this project.");
							return;
						}
					}
				}
			}else{
				this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"User account doesn't have permission to edit this project.");
				return;
			}
		} catch(IllegalArgumentException e){
			this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,e.getMessage());
			return;
		}catch (Exception e) {
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			e.printStackTrace();
		}
	}


	@Override
	public Representation getRepresentation(Variant variant) {	
		MediaType mt = overrideVariant(variant);
		
		if(proj!=null){
			if(filepath!=null && !filepath.equals("")){
				if(filepath.equals("quarantine_code")){
					try {
						return new StringRepresentation(proj.getArcSpecification().getQuarantineCode().toString(),mt);
					} catch (Throwable e) {
						logger.error("",e);
						this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e.getMessage());
					    return null;
					}
				}else if(filepath.startsWith("prearchive_code")){
					try {
						return new StringRepresentation(proj.getArcSpecification().getPrearchiveCode().toString(),mt);
					} catch (Throwable e) {
						logger.error("",e);
						this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e.getMessage());
					    return null;
					}
				}else if(filepath.startsWith("current_arc")){
					try {
						return new StringRepresentation(proj.getArcSpecification().getCurrentArc(),mt);
					} catch (Throwable e) {
						logger.error("",e);
						this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e.getMessage());
					    return null;
					}
				}else{
					this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
					return null;
				}
			}else{
				return this.representItem(proj.getItem(),mt);
			}
		}else{
			this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Unable to find the specified experiment.");
			return null;
		}

	}
}
