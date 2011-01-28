// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatReconstructedimagedata;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTItem;
import org.nrg.xft.db.DBAction;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.db.PoolDBUtils;
import org.nrg.xft.exception.InvalidValueException;
import org.nrg.xft.utils.ValidationUtils.ValidationResults;
import org.nrg.xnat.helpers.xmlpath.XMLPathShortcuts;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

public class ReconResource extends ItemResource {
	final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ScanResource.class);
	XnatProjectdata proj=null;
	XnatImagesessiondata session = null;
	XnatReconstructedimagedata recon=null;
	
	String exptID=null;
	
	public ReconResource(Context context, Request request, Response response) {
		super(context, request, response);
		
			String pID= (String)request.getAttributes().get("PROJECT_ID");
			if(pID!=null){
				proj = XnatProjectdata.getXnatProjectdatasById(pID, user, false);

			if (proj == null) {
				ArrayList<XnatProjectdata> matches = XnatProjectdata
						.getXnatProjectdatasByField(
								"xnat:projectData/aliases/alias/alias", pID,
								user, false);
				if (matches.size() > 0) {
					proj = matches.get(0);
				}
			}
			}
			
			String assessedID= (String)request.getAttributes().get("ASSESSED_ID");
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

				exptID= (String)request.getAttributes().get("RECON_ID");
				if(exptID!=null){
				this.getVariants().add(new Variant(MediaType.TEXT_HTML));
					this.getVariants().add(new Variant(MediaType.TEXT_XML));
				}
				
			}else{
			response.setStatus(Status.CLIENT_ERROR_GONE,
					"Unable to find session '" + assessedID + "'");
		}
		
			this.fieldMapping.putAll(XMLPathShortcuts.getInstance().getShortcuts(XMLPathShortcuts.RECON_DATA));
	}


	@Override
	public boolean allowPut() {
		return true;
	}

	@Override
	public void handlePut() {
	        XFTItem item = null;			

			try {
			item=this.loadItem("xnat:reconstructedImageData",true);
			
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
				if(item.instanceOf("xnat:reconstructedImageData")){
					recon = (XnatReconstructedimagedata)BaseElement.GetGeneratedItem(item);
					
					//MATCH SESSION
					if(this.session!=null){
						recon.setImageSessionId(this.session.getId());
					}else{
						if(recon.getImageSessionId()!=null && !recon.getImageSessionId().equals("")){
							this.session=(XnatImagesessiondata)XnatExperimentdata.getXnatExperimentdatasById(recon.getImageSessionId(), user, false);
							
							if(this.session==null && this.proj!=null){
							this.session=(XnatImagesessiondata)XnatExperimentdata.GetExptByProjectIdentifier(this.proj.getId(), recon.getImageSessionId(),user, false);
							}
							if(this.session!=null){
								recon.setImageSessionId(this.session.getId());
							}
						}
					}
					
					if(recon.getImageSessionId()==null){
						this.getResponse().setStatus(Status.CLIENT_ERROR_EXPECTATION_FAILED,"Specified reconstruction must reference a valid image session.");
						return;
					}
					
					if(this.session==null){
						this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Specified image session doesn't exist.");
						return;
					}
					
					if(recon.getId()==null){
						recon.setId(exptID);
					}

				if(this.getQueryVariable("type")!=null){
					recon.setType(this.getQueryVariable("type"));
				}

					//FIND PRE-EXISTING
					XnatReconstructedimagedata existing=null;
					
					if(recon.getXnatReconstructedimagedataId()!=null){						
						existing=(XnatReconstructedimagedata)XnatReconstructedimagedata.getXnatReconstructedimagedatasByXnatReconstructedimagedataId(recon.getXnatReconstructedimagedataId(), user, completeDocument);
					}					
					
					if(recon.getId()!=null){						
						existing=(XnatReconstructedimagedata)XnatReconstructedimagedata.getXnatReconstructedimagedatasById(recon.getId(), user, completeDocument);
					}		
					
					if(existing==null){
						if(!user.canEdit(this.session)){
						this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Specified user account has insufficient create priviledges for sessions in this project.");
							return;
						}
						//IS NEW
						if(recon.getId()==null || recon.getId().equals("")){
							String query = "SELECT count(id) AS id_count FROM xnat_reconstructedimagedata WHERE id='";

					        String login = null;
					        if (user!=null){
					            login=user.getUsername();
					        }
					        try {
					        	int i=1;
					            Long idCOUNT= (Long)PoolDBUtils.ReturnStatisticQuery(query + this.session.getId() + "_RECON_" +i + "';", "id_count", user.getDBName(), login);
					            while (idCOUNT > 0){
					                i++;
					                idCOUNT= (Long)PoolDBUtils.ReturnStatisticQuery(query +this.session.getId() + "_RECON_" + i + "';", "id_count", user.getDBName(), login);
					            }
					            	
					            recon.setId("" + i);
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
					

				
				final ValidationResults vr = recon.validate();
	            
	            if (vr != null && !vr.isValid())
	            {
	            	this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,vr.toFullString());
					return;
	            }
				
					recon.save(user,false,allowDataDeletion);
					
					MaterializedView.DeleteByUser(user);
				}else{
					this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY,"Only reconstruction documents can be PUT to this address.");
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
			if(recon==null&& exptID!=null){
					recon=(XnatReconstructedimagedata)XnatReconstructedimagedata.getXnatReconstructedimagedatasById(exptID, user, completeDocument);
				}	
			
			if(recon==null){
			this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Unable to find the specified reconstruction.");
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
			
				String removeFiles=this.getQueryVariable("removeFiles");
	            if (removeFiles!=null){
	            	final List<XFTItem> hash = recon.getItem().getChildrenOfType("xnat:abstractResource");
	                
	                for (XFTItem resource : hash){
	                    ItemI om = BaseElement.GetGeneratedItem((XFTItem)resource);
	                    if (om instanceof XnatAbstractresource){
	                        XnatAbstractresource resourceA = (XnatAbstractresource)om;
	                        resourceA.deleteFromFileSystem(proj.getRootArchivePath());
	                    }
	                }
	            }
	            DBAction.DeleteItem(recon.getItem().getCurrentDBVersion(), user);
	            
			    user.clearLocalCache();
				MaterializedView.DeleteByUser(user);
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
		MediaType mt = overrideVariant(variant);

		if(recon==null&& exptID!=null){
				recon=(XnatReconstructedimagedata)XnatReconstructedimagedata.getXnatReconstructedimagedatasById(exptID, user, completeDocument);
			}
		
		if(recon!=null){
			return this.representItem(recon.getItem(),MediaType.TEXT_XML);
	
		}else{
			this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,
					"Unable to find the specified reconstruction.");
			return null;
		}

	}
}