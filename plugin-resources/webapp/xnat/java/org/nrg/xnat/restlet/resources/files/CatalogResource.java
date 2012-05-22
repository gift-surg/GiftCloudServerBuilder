// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources.files;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.collections.CollectionUtils;
import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.bean.CatCatalogBean;
import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatAbstractresourceTag;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xft.XFTItem;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xnat.restlet.representations.CatalogRepresentation;
import org.nrg.xnat.restlet.representations.ItemXMLRepresentation;
import org.nrg.xnat.restlet.resources.ScanResource;
import org.nrg.xnat.turbine.utils.ArchivableItem;
import org.nrg.xnat.utils.WorkflowUtils;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

public class CatalogResource extends XNATCatalogTemplate {
	final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ScanResource.class);

	public CatalogResource(Context context, Request request, Response response) {
		super(context, request, response,false);
		
			try {
				if(catalogs!=null && catalogs.size()>0){
					for(Object[] row: catalogs.rows()){
						Integer id = (Integer)row[0];
						String label = (String)row[1];
						
						for(String resourceID:this.resource_ids){
							if(id.toString().equals(resourceID) || (label!=null && label.equals(resourceID))){
								resources.add(XnatAbstractresource.getXnatAbstractresourcesByXnatAbstractresourceId(row[0], user, false));
							}
						}
					
				}
				}
				
				this.getVariants().add(new Variant(MediaType.TEXT_XML));
			} catch (Exception e) {
	            logger.error("",e);
			}
	}

	@Override
	public boolean allowPut() {
		return true;
	}
	
	@Override
	public void handlePut() {
		if(this.parent!=null && this.security!=null){
			XFTItem item = null;	
			try {	
				if(user.canEdit(this.security)){
					if(this.resources.size()>0){
						this.getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT, "Specified resource already exists.");
						return;
					}

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
						
						catResource.setLabel(resource_ids.get(0));

						PersistentWorkflowI wrk=PersistentWorkflowUtils.getWorkflowByEventId(user,getEventId());
						if(wrk==null && "SNAPSHOTS".equals(catResource.getLabel())){
							if(getSecurityItem() instanceof XnatExperimentdata){
								Collection<? extends PersistentWorkflowI> wrks=PersistentWorkflowUtils.getOpenWorkflows(user,((ArchivableItem)getSecurityItem()).getId());
								if(wrks!=null && wrks.size()==1){
									wrk=(WrkWorkflowdata)CollectionUtils.get(wrks, 0);
									if(!"xnat_tools/AutoRun.xml".equals(wrk.getPipelineName())){
										wrk=null;
									}else{
										if(StringUtils.IsEmpty(wrk.getCategory())){
											wrk.setCategory(EventUtils.CATEGORY.DATA);
											wrk.setType(EventUtils.TYPE.PROCESS);
											WorkflowUtils.save(wrk, wrk.buildEvent());
										}
									}
								}
							}
						}
						
						boolean isNew=false;
						if(wrk==null){
							isNew=true;
							wrk=PersistentWorkflowUtils.buildOpenWorkflow(user, getSecurityItem().getItem(), newEventInstance(EventUtils.CATEGORY.DATA,(getAction()!=null)?getAction():EventUtils.CREATE_RESOURCE));
						}

						EventMetaI ci=wrk.buildEvent();

						this.insertCatalag(catResource,ci);

						if(isNew){
							WorkflowUtils.complete(wrk, ci);
						}
					}else{
						this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY,"Only ResourceCatalog documents can be PUT to this address.");
					}
				}else{
					this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"User account doesn't have permission to modify this session.");
					return;
				}

			} catch (Exception e) {
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e.getMessage());
				logger.error("",e);
			}
		}
	}

	@Override
	public boolean allowDelete() {
		return true;
	}

	@Override
	public void handleDelete(){
		if(resources.size()>0 && this.parent!=null && this.security!=null){
			for(XnatAbstractresource resource:resources){
				try {
					if(user.canDelete(this.security)){
						String securityId=null;
						if(proj==null){
							if(parent.getItem().instanceOf("xnat:experimentData")){
								securityId=((XnatExperimentdata)parent).getId();
								proj = ((XnatExperimentdata)parent).getPrimaryProject(false);
							}else if(security.getItem().instanceOf("xnat:experimentData")){
								securityId=((XnatExperimentdata)security).getId();
								proj = ((XnatExperimentdata)security).getPrimaryProject(false);
							}else if(parent.getItem().instanceOf("xnat:subjectData")){
								securityId=((XnatSubjectdata)parent).getId();
								proj = ((XnatSubjectdata)parent).getPrimaryProject(false);
							}else if(security.getItem().instanceOf("xnat:subjectData")){
								securityId=((XnatSubjectdata)security).getId();
								proj = ((XnatSubjectdata)security).getPrimaryProject(false);
							}
						}else{
							if(parent.getItem().instanceOf("xnat:experimentData")){
								securityId=((XnatExperimentdata)parent).getId();
							}else if(security.getItem().instanceOf("xnat:experimentData")){
								securityId=((XnatExperimentdata)security).getId();
							}else if(parent.getItem().instanceOf("xnat:subjectData")){
								securityId=((XnatSubjectdata)parent).getId();
							}else if(security.getItem().instanceOf("xnat:subjectData")){
								securityId=((XnatSubjectdata)security).getId();
							}
						}
						
						final String rootPath=proj.getRootArchivePath();
						
						final PersistentWorkflowI workflow=PersistentWorkflowUtils.getOrCreateWorkflowData(getEventId(), user, security.getXSIType(), securityId, (proj==null)?null:proj.getId(),newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.REMOVE_CATALOG));
				    	EventMetaI ci=workflow.buildEvent();
						
						try {
							resource.deleteWithBackup(rootPath, user, ci);
							SaveItemHelper.authorizedRemoveChild(this.parent.getItem(), xmlPath, resource.getItem(), user,ci);

							PersistentWorkflowUtils.complete(workflow,ci);
						} catch (Exception e) {
							PersistentWorkflowUtils.fail(workflow,ci);
							throw e;
						}
					}else{
						this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"User account doesn't have permission to modify this session.");
						return;
					}
				} catch (Exception e) {
					logger.error("",e);
					this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e.getMessage());
					return;
				}
			}
		}
	}
	
	private void getAllMatches(){
		catalogs=null;
		resources=new ArrayList<XnatAbstractresource>();
		try {
			catalogs=this.loadCatalogs(resource_ids,false,true);
		} catch (Exception e) {
            logger.error("",e);
		}
		
		if(catalogs!=null && catalogs.size()>0){
			for(Object[] row: catalogs.rows()){
				Integer id = (Integer)row[0];
				String label = (String)row[1];
				
				for(String resourceID:this.resource_ids){
					if(id.toString().equals(resourceID) || (label!=null && label.equals(resourceID))){
						resources.add(XnatAbstractresource.getXnatAbstractresourcesByXnatAbstractresourceId(row[0], user, false));
					}
				}
				
			}
		}
	}
	

	@Override
	public Representation getRepresentation(Variant variant) {	
		this.getAllMatches();
		
		if(resources.size()==0){
			this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Unable to find the specified catalog.");
		}else if(resources.size()==1){
			XnatAbstractresource resource=resources.get(0);
			try {
				if(proj==null){
					if(parent.getItem().instanceOf("xnat:experimentData")){
						proj = ((XnatExperimentdata)parent).getPrimaryProject(false);
					}else if(security.getItem().instanceOf("xnat:experimentData")){
						proj = ((XnatExperimentdata)security).getPrimaryProject(false);
					}
				}
				
				if(resource.getItem().instanceOf("xnat:resourceCatalog")){
					boolean includeRoot=false;
					if(this.getQueryVariable("includeRootPath")!=null){
						includeRoot=true;
					}
					
					XnatResourcecatalog catResource = (XnatResourcecatalog)resource;
					CatCatalogBean cat= catResource.getCleanCatalog(proj.getRootArchivePath(),includeRoot,null,null);
			    	
			    	if(cat!=null)
						return new CatalogRepresentation(cat,MediaType.TEXT_XML);
			    	else{
			    		this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Unable to find catalog file.");
			    	}
				}else{
					return new ItemXMLRepresentation(resource.getItem(),MediaType.TEXT_XML);
				}
			} catch (ElementNotFoundException e) {
	            logger.error("",e);
			}
		}else{
			//multiple resources
		}

		return null;

	}
}