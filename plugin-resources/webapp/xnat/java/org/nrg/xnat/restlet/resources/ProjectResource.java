// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources;

import java.util.ArrayList;

import org.nrg.xdat.om.ArcProject;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.base.BaseXnatProjectdata;
import org.nrg.xdat.security.ElementSecurity;
import org.nrg.xft.XFTItem;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.exception.InvalidItemException;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
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

    private String msg=null;
    
	XnatProjectdata proj=null;
	String pID=null;
	public ProjectResource(Context context, Request request, Response response) {
		super(context, request, response);
		
			pID= (String)request.getAttributes().get("PROJECT_ID");
			if(pID!=null){
				proj = XnatProjectdata.getXnatProjectdatasById(pID, user, false);
			}
			
		if (proj == null) {
			ArrayList<XnatProjectdata> matches = XnatProjectdata
					.getXnatProjectdatasByField(
							"xnat:projectData/aliases/alias/alias", pID, user,
							false);
			if (matches.size() > 0) {
				proj = matches.get(0);
			}
		}

			if(proj!=null){
				this.getVariants().add(new Variant(MediaType.TEXT_HTML));
			this.getVariants().add(new Variant(MediaType.TEXT_XML));
			}
		
		this.fieldMapping.put("ID", "xnat:projectData/ID");
		this.fieldMapping.put("secondary_ID", "xnat:projectData/secondary_ID");
		this.fieldMapping.put("name", "xnat:projectData/name");
		this.fieldMapping.put("description", "xnat:projectData/description");
		this.fieldMapping.put("keywords", "xnat:projectData/keywords");
		this.fieldMapping.put("alias", "xnat:projectData/aliases/alias/alias");
		this.fieldMapping.put("pi_firstname", "xnat:projectData/PI/firstname");
		this.fieldMapping.put("pi_lastname", "xnat:projectData/PI/lastname");
		this.fieldMapping.put("note", "xnat:projectData/fields/field[name=note]/field");
		
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
				boolean removeFiles=this.isQueryVariableTrue("removeFiles");
				if(user.canDelete(proj)){
					this.proj.delete(removeFiles, user);
				}else{
					this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"User account doesn't have permission to delete this project.");
					return;
				}
			} catch (InvalidItemException e) {
				e.printStackTrace();
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			} catch (Exception e) {
				e.printStackTrace();
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
								ap.save(user, false, false);
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
									ap.save(user, false, false);
									ArcSpecManager.Reset();
							}
						}else if(filepath.startsWith("current_arc/")){
							String qc= filepath.substring(12);
							if(!qc.equals("")){
								ArcProject ap =project.getArcSpecification();
								ap.setCurrentArc(qc);
								ap.save(user, false, false);
								ArcSpecManager.Reset();
							}
						}else{
								this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
								return;
							}
					}else{
					boolean overrideSecurity=false;
					
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
						overrideSecurity=true;
					}
					
					try {
							project.initNewProject(user,allowDataDeletion,true);
					} catch (Exception e) {
						e.printStackTrace();
						this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY,e.getMessage());
							return;
					}
					
					project.save(user,overrideSecurity,false);
					item =project.getItem().getCurrentDBVersion(false);
					
					XnatProjectdata postSave = new XnatProjectdata(item);
	                postSave.getItem().setUser(user);

	                postSave.initGroups();
	                
	                //postSave.initBundles(user);
	                
	                String accessibility=getQueryVariable("accessibility");
	                if (accessibility==null){
	                    accessibility="protected";
	                }
	                
	                if (!accessibility.equals("private"))
	                    project.initAccessibility(accessibility, true);
	                
	                user.refreshGroup(postSave.getId() + "_" + BaseXnatProjectdata.OWNER_GROUP);

	                postSave.initArcProject(null, user);

				    user.clearLocalCache();
					MaterializedView.DeleteByUser(user);
	                ElementSecurity.refresh();
				}
				}
	        }else{
				this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"User account doesn't have permission to edit this project.");
				return;
	        }
			} catch (Exception e) {
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
