// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.nrg.dcm.xnat.SessionBuilder;
import org.nrg.dcm.xnat.XnatAttrDef;
import org.nrg.ecat.xnat.PETSessionBuilder;
import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatCtsessiondata;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.om.XnatPetsessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTItem;
import org.nrg.xft.db.DBAction;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.db.PoolDBUtils;
import org.nrg.xft.exception.InvalidValueException;
import org.nrg.xft.schema.Wrappers.XMLWrapper.SAXReader;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.ValidationUtils.ValidationResults;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.xml.sax.SAXException;

public class ScanResource  extends ItemResource {
	final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ScanResource.class);
	XnatProjectdata proj=null;
	XnatImagesessiondata session = null;
	XnatImagescandata scan=null;
	
	String scanID=null;
	
	public ScanResource(Context context, Request request, Response response) {
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

				scanID= (String)request.getAttributes().get("SCAN_ID");
				if(scanID!=null){
				this.getVariants().add(new Variant(MediaType.TEXT_HTML));
					this.getVariants().add(new Variant(MediaType.TEXT_XML));
				}
				
			}else{
			response.setStatus(Status.CLIENT_ERROR_GONE,
					"Unable to find session '" + assessedID + "'");
		}
		
		this.fieldMapping.put("ID", "xnat:imageScanData/ID");
		this.fieldMapping.put("type", "xnat:imageScanData/type");
		this.fieldMapping.put("UID", "xnat:imageScanData/UID");
		this.fieldMapping.put("note", "xnat:imageScanData/note");
		this.fieldMapping.put("quality", "xnat:imageScanData/quality");
		this.fieldMapping.put("condition", "xnat:imageScanData/condition");
		this.fieldMapping.put("series_description", "xnat:imageScanData/series_description");
		this.fieldMapping.put("documentation", "xnat:imageScanData/documentation");
		this.fieldMapping.put("scanner", "xnat:imageScanData/scanner");
		this.fieldMapping.put("modality", "xnat:imageScanData/modality");
		this.fieldMapping.put("frames", "xnat:imageScanData/frames");
		this.fieldMapping.put("validation_method", "xnat:imageScanData/validation/method");
		this.fieldMapping.put("validation_status", "xnat:imageScanData/validation/status");
		this.fieldMapping.put("validation_date", "xnat:imageScanData/validation/date");
		this.fieldMapping.put("validation_notes", "xnat:imageScanData/validation/notes");

		this.fieldMapping.put("coil", "xnat:mrScanData/coil");
		this.fieldMapping.put("fieldStrength", "xnat:mrScanData/fieldStrength");
		this.fieldMapping.put("marker", "xnat:mrScanData/marker");
		this.fieldMapping.put("stabilization", "xnat:mrScanData/stabilization");     
		
		this.fieldMapping.put("orientation","xnat:petScanData/parameters/orientation");                                                         
		this.fieldMapping.put("scanTime","xnat:petScanData/parameters/scanTime");                                                         
		this.fieldMapping.put("originalFileName","xnat:petScanData/parameters/originalFileName");                                                         
		this.fieldMapping.put("systemType","xnat:petScanData/parameters/systemType");                                                         
		this.fieldMapping.put("fileType","xnat:petScanData/parameters/fileType");                                                         
		this.fieldMapping.put("transaxialFOV","xnat:petScanData/parameters/transaxialFOV");                                                         
		this.fieldMapping.put("acqType","xnat:petScanData/parameters/acqType");                                                         
		this.fieldMapping.put("facility","xnat:petScanData/parameters/facility");                                                         
		this.fieldMapping.put("numPlanes","xnat:petScanData/parameters/numPlanes");                                                         
		this.fieldMapping.put("numFrames","xnat:petScanData/parameters/frames/numFrames");                                                         
		this.fieldMapping.put("numGates","xnat:petScanData/parameters/numGates");                                                         
		this.fieldMapping.put("planeSeparation","xnat:petScanData/parameters/planeSeparation");                                                         
		this.fieldMapping.put("binSize","xnat:petScanData/parameters/binSize");                                                         
		this.fieldMapping.put("dataType","xnat:petScanData/parameters/dataType");  
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
				
					scan.save(user,false,allowDataDeletion);
					
					MaterializedView.DeleteByUser(user);
				
				if(this.isQueryVariableTrue("pullDataFromHeaders")){
					pullDataFromHeaders(scan,user,allowDataDeletion);
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
			
				String removeFiles=this.getQueryVariable("removeFiles");
	            if (removeFiles!=null){
	            	final List<XFTItem> hash = scan.getItem().getChildrenOfType("xnat:abstractResource");
	                
	                for (XFTItem resource : hash){
	                    ItemI om = BaseElement.GetGeneratedItem((XFTItem)resource);
	                    if (om instanceof XnatAbstractresource){
	                        XnatAbstractresource resourceA = (XnatAbstractresource)om;
	                        resourceA.deleteFromFileSystem(proj.getRootArchivePath());
	                    }
	                }
	            }
	            DBAction.DeleteItem(scan.getItem().getCurrentDBVersion(), user);
	            
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
	


	public static void pullDataFromHeaders(XnatImagescandata tempMR, XDATUser user,boolean allowDataDeletion) throws IOException,SAXException,Exception{
		Date d = Calendar.getInstance().getTime();
        File scanDir=new File(tempMR.deriveScanDir());
		
        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat ("yyyyMMdd_HHmmss");
        String timestamp=formatter.format(d);
		File xml= new File(scanDir,tempMR.getId()+ "_"+ timestamp+".xml");
		SessionBuilder builder = new SessionBuilder(scanDir,new FileWriter(xml),new XnatAttrDef.Constant("project", tempMR.getImageSessionData().getProject()));
	    builder.run();
		
	    if(!xml.exists() || xml.length()==0){
	    	new PETSessionBuilder(scanDir,new FileWriter(xml),tempMR.getImageSessionData().getProject()).run();
	    }
	      
	    if(!xml.exists() || xml.length()==0){
	    	new Exception("Unable to locate DICOM or ECAT files");
	    }
	    
		SAXReader reader = new SAXReader(user);
		XFTItem temp2 = reader.parse(xml.getAbsolutePath());
		XnatImagesessiondata newmr = (XnatImagesessiondata)BaseElement.GetGeneratedItem(temp2);
        XnatImagescandata newscan=null;
        
		
    	if(newmr.getScans_scan().size()>1){
    		throw new Exception("Multiple Scans in single scan folder");
    	}else{
    		newscan=newmr.getScans_scan().get(0);
    	}
             
        newscan.copyValuesFrom(tempMR);
        newscan.setImageSessionId(tempMR.getImageSessionId());
        newscan.setId(tempMR.getId());
        newscan.setXnatImagescandataId(tempMR.getXnatImagescandataId());
    	
	    if(!allowDataDeletion){
    		while(newscan.getFile().size()>0)newscan.removeFile(0);
		}

        final ValidationResults vr = newmr.validate();        
        
        if (vr != null && !vr.isValid())
        {
            throw new Exception(vr.toString());
        }else{
        	XnatImagesessiondata mr=tempMR.getImageSessionData();
        	XnatProjectdata proj = mr.getProjectData();
        	if(newscan.save(user,false,allowDataDeletion)){
				MaterializedView.DeleteByUser(user);

				if(proj.getArcSpecification().getQuarantineCode()!=null && proj.getArcSpecification().getQuarantineCode().equals(1)){
					mr.quarantine(user);
				}
			}
            
            try {
  				WrkWorkflowdata workflow = new WrkWorkflowdata((UserI)user);
  				workflow.setDataType(mr.getXSIType());
  				workflow.setExternalid(proj.getId());
  				workflow.setId(mr.getId());
  				workflow.setPipelineName("Header Mapping: Scan "+newscan.getId());
  				workflow.setStatus("Complete");
  				workflow.setLaunchTime(Calendar.getInstance().getTime());
  				workflow.save(user, false, false);
  			} catch (Throwable e) {
  				e.printStackTrace();
  			}
        }

	}
}