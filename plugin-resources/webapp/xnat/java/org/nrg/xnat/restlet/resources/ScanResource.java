// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources;

import java.sql.SQLException;
import java.util.ArrayList;

import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.om.XnatCtsessiondata;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.om.XnatPetsessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.Authorizer;
import org.nrg.xft.XFTItem;
import org.nrg.xft.db.PoolDBUtils;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.exception.InvalidValueException;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xft.utils.ValidationUtils.ValidationResults;
import org.nrg.xnat.helpers.xmlpath.XMLPathShortcuts;
import org.nrg.xnat.restlet.actions.PullScanDataFromHeaders;
import org.nrg.xnat.restlet.util.XNATRestConstants;
import org.nrg.xnat.utils.WorkflowUtils;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

public class ScanResource  extends ItemResource {
	final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ScanResource.class);
	XnatProjectdata proj=null;
	XnatImagesessiondata session = null;
	XnatImagescandata scan=null;
	
	String scanID=null;
	
	public ScanResource(Context context, Request request, Response response) {
		super(context, request, response);
		
			String pID= (String)getParameter(request,"PROJECT_ID");
			if(pID!=null){
				proj = XnatProjectdata.getProjectByIDorAlias(pID, user, false);
			}
			
			String assessedID= (String)getParameter(request,"ASSESSED_ID");
			if(assessedID!=null){
				if(session==null&& assessedID!=null){
				session = (XnatImagesessiondata) XnatExperimentdata
						.getXnatExperimentdatasById(assessedID, user, false);
				if (session != null
						&& (proj != null && !session.hasProject(proj.getId()))) {
					session = null;
				}
					
					if(session==null && this.proj!=null){
					session = (XnatImagesessiondata) XnatExperimentdata
							.GetExptByProjectIdentifier(this.proj.getId(),
									assessedID, user, false);
					}
				}

				scanID= (String)getParameter(request,"SCAN_ID");
				if(scanID!=null){
				this.getVariants().add(new Variant(MediaType.TEXT_HTML));
					this.getVariants().add(new Variant(MediaType.TEXT_XML));
				}
				
			}else{
			response.setStatus(Status.CLIENT_ERROR_GONE,
					"Unable to find session '" + assessedID + "'");
		}
		
		this.fieldMapping.putAll(XMLPathShortcuts.getInstance().getShortcuts(XnatImagescandata.SCHEMA_ELEMENT_NAME,false));
	}


	@Override
	public boolean allowPut() {
		return true;
	}

	@Override
	public void handlePut() {
		XFTItem item = null;			

		try {
			String dataType=null;
			if(this.session instanceof XnatMrsessiondata){
				dataType="xnat:mrScanData";
			}else if(this.session instanceof XnatPetsessiondata){
				dataType="xnat:petScanData";
			}else if(this.session instanceof XnatCtsessiondata){
				dataType="xnat:ctScanData";
			}
			item=this.loadItem(dataType,true);

			if(item==null){
				String xsiType=this.getQueryVariable("xsiType");
				if(xsiType!=null){
					item=XFTItem.NewItem(xsiType, user);
				}
			}

			if(item==null){
				this.getResponse().setStatus(Status.CLIENT_ERROR_EXPECTATION_FAILED, "Need POST Contents");
				return;
			}

			if(filepath!=null && !filepath.equals("")){
				this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return;
			}
			if(item.instanceOf("xnat:imageScanData")){
				scan = (XnatImagescandata)BaseElement.GetGeneratedItem(item);

				//MATCH SESSION
				if(this.session!=null){
					scan.setImageSessionId(this.session.getId());
				}else{
					if(scan.getImageSessionId()!=null && !scan.getImageSessionId().equals("")){
						this.session=(XnatImagesessiondata)XnatExperimentdata.getXnatExperimentdatasById(scan.getImageSessionId(), user, false);

						if(this.session==null && this.proj!=null){
							this.session=(XnatImagesessiondata)XnatExperimentdata.GetExptByProjectIdentifier(this.proj.getId(), scan.getImageSessionId(),user, false);
						}
						if(this.session!=null){
							scan.setImageSessionId(this.session.getId());
						}
					}
				}

				if(scan.getImageSessionId()==null){
					this.getResponse().setStatus(Status.CLIENT_ERROR_EXPECTATION_FAILED,"Specified scan must reference a valid image session.");
					return;
				}

				if(this.session==null){
					this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Specified image session doesn't exist.");
					return;
				}

				if(scan.getId()==null){
					scan.setId(scanID);
				}

				if(this.getQueryVariable("type")!=null){
					scan.setType(this.getQueryVariable("type"));
				}

				//FIND PRE-EXISTING
				XnatImagescandata existing=null;

				if(scan.getXnatImagescandataId()!=null){						
					existing=(XnatImagescandata)XnatImagescandata.getXnatImagescandatasByXnatImagescandataId(scan.getXnatImagescandataId(), user, completeDocument);
				}					

				if(scan.getId()!=null){
					CriteriaCollection cc= new CriteriaCollection("AND");
					cc.addClause("xnat:imageScanData/ID", scan.getId());
					cc.addClause("xnat:imageScanData/image_session_ID", scan.getImageSessionId());
					ArrayList<XnatImagescandata> scans=XnatImagescandata.getXnatImagescandatasByField(cc, user, completeDocument);
					if(scans.size()>0){
						existing=scans.get(0);
					}
				}

				if(existing==null){
					if(!user.canEdit(this.session)){
						this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Specified user account has insufficient create priviledges for sessions in this project.");
						return;
					}
					//IS NEW
					if(scan.getId()==null || scan.getId().equals("")){
						String query = "SELECT count(id) AS id_count FROM xnat_imageScanData WHERE image_session_id='" + this.session.getId() + "' AND id='";

						String login = null;
						if (user!=null){
							login=user.getUsername();
						}
						try {
							int i=1;
							Long idCOUNT= (Long)PoolDBUtils.ReturnStatisticQuery(query + i + "';", "id_count", user.getDBName(), login);
							while (idCOUNT > 0){
								i++;
								idCOUNT= (Long)PoolDBUtils.ReturnStatisticQuery(query + i + "';", "id_count", user.getDBName(), login);
							}

							scan.setId("" + i);
						} catch (Exception e) {
							logger.error("",e);
						}
					}
					
				}else{
					if(!user.canEdit(session)){
						this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Specified user account has insufficient edit priviledges for sessions in this project.");
						return;
					}
					//MATCHED
				}

				boolean allowDataDeletion=false;
				if(this.getQueryVariable("allowDataDeletion")!=null && this.getQueryVariable("allowDataDeletion").equals("true")){
					allowDataDeletion=true;
				}


				final ValidationResults vr = scan.validate();

				if (vr != null && !vr.isValid())
				{
					this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,vr.toFullString());
					return;
				}

	            Authorizer.getInstance().authorizeSave(session.getItem(), user);
				this.create(session, scan, false, allowDataDeletion, newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.getAddModifyAction(scan.getXSIType(), scan==null)));

				if(this.isQueryVariableTrue(XNATRestConstants.PULL_DATA_FROM_HEADERS) || this.containsAction(XNATRestConstants.PULL_DATA_FROM_HEADERS)){
					PersistentWorkflowI wrk=PersistentWorkflowUtils.buildOpenWorkflow(user, session.getItem(), newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.DICOM_PULL));
					EventMetaI c=wrk.buildEvent();
					try {
						PullScanDataFromHeaders pull=new PullScanDataFromHeaders(scan, user, allowDataDeletion,false,c);
						pull.call();
						WorkflowUtils.complete(wrk, c);
					} catch (Exception e) {
						WorkflowUtils.fail(wrk, c);
						throw e;
					}
				}

			}else{
				this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY,"Only Scan documents can be PUT to this address.");
			}
		} catch (InvalidValueException e) {
			this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			logger.error("",e);
		} catch (Exception e) {
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			logger.error("",e);
		}
	}


	@Override
	public boolean allowDelete() {
		return true;
	}

	@Override
	public void handleDelete(){
			if(scan==null&& scanID!=null){
				if(scan==null && this.session!=null){
					CriteriaCollection cc= new CriteriaCollection("AND");
					cc.addClause("xnat:imageScanData/ID", scanID);
					cc.addClause("xnat:imageScanData/image_session_ID", session.getId());
					ArrayList<XnatImagescandata> scans=XnatImagescandata.getXnatImagescandatasByField(cc, user, completeDocument);
					if(scans.size()>0){
						scan=scans.get(0);
					}
				}
			}
			
			if(scan==null){
			this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Unable to find the specified scan.");
				return;
			}
			
			if(filepath!=null && !filepath.equals("")){
				this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
				return;
			}
			try {
			
			if(!user.canDelete(session)){
				this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"User account doesn't have permission to modify this session.");
					return;
			}
			
			delete(session, scan,newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.getDeleteAction(scan.getXSIType())));
            
		} catch (SQLException e) {
			e.printStackTrace();
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e);
		} catch (Exception e) {
			e.printStackTrace();
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e);
		}
	}
	

	@Override
	public Representation getRepresentation(Variant variant) {	
		if(scan==null&& scanID!=null){
			if(scan==null && this.session!=null){
				CriteriaCollection cc= new CriteriaCollection("AND");
				cc.addClause("xnat:imageScanData/ID", scanID);
				cc.addClause("xnat:imageScanData/image_session_ID", session.getId());
				ArrayList<XnatImagescandata> scans=XnatImagescandata.getXnatImagescandatasByField(cc, user, completeDocument);
				if(scans.size()>0){
					scan=scans.get(0);
				}
			}
		}
		
		if(scan!=null){
	        	return this.representItem(scan.getItem(),MediaType.TEXT_XML);
		}else{
			this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,
					"Unable to find the specified scan.");
			return null;
		}

	}
}