// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.om.XnatCtsessiondata;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.om.XnatPetsessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTable;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.db.PoolDBUtils;
import org.nrg.xft.db.ViewManager;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.InvalidValueException;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.search.QueryOrganizer;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xft.utils.ValidationUtils.ValidationResults;
import org.nrg.xnat.restlet.representations.ItemXMLRepresentation;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

public class ScanList extends QueryOrganizerResource {
	final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ScanList.class);

	XnatProjectdata proj=null;
	XnatSubjectdata sub=null;
	XnatImagesessiondata session=null;
	XnatImagescandata scan=null;

	public ScanList(Context context, Request request, Response response) {
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

				String subID= (String)request.getAttributes().get("SUBJECT_ID");
				if(subID!=null){
				sub = XnatSubjectdata.GetSubjectByProjectIdentifier(proj
						.getId(), subID, user, false);

					if(sub==null){
					sub = XnatSubjectdata.getXnatSubjectdatasById(subID, user,
							false);
					if (sub != null
							&& (proj != null && !sub.hasProject(proj.getId()))) {
						sub = null;
					}
					}

					if(sub!=null){
					String exptID = (String) request.getAttributes().get(
							"ASSESSED_ID");
					session = XnatImagesessiondata
							.getXnatImagesessiondatasById(exptID, user, false);
					if (session != null
							&& (proj != null && !session.hasProject(proj
									.getId()))) {
						session = null;
					}

						if(session==null){
						session = (XnatImagesessiondata) XnatImagesessiondata
								.GetExptByProjectIdentifier(proj.getId(),
										exptID, user, false);
						}

						if(session!=null){
						this.getVariants().add(
								new Variant(MediaType.APPLICATION_JSON));
						this.getVariants()
								.add(new Variant(MediaType.TEXT_HTML));
							this.getVariants().add(new Variant(MediaType.TEXT_XML));
						}else{
						response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
						}
					}else{
					response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
					}
				}else{
				response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
				}
			}else{
				String exptID= (String)request.getAttributes().get("ASSESSED_ID");
			session = XnatImagesessiondata.getXnatImagesessiondatasById(exptID,
					user, false);

				if(session==null){
				session = (XnatImagesessiondata) XnatImagesessiondata
						.GetExptByProjectIdentifier(proj.getId(), exptID, user,
								false);
				}

				if(session!=null){
					this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
					this.getVariants().add(new Variant(MediaType.TEXT_HTML));
					this.getVariants().add(new Variant(MediaType.TEXT_XML));
				}else{
				response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
				}
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

		this.fieldMapping.put("xnat_imagescandata_id", "xnat:imageScanData/xnat_imagescandata_id");

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

		this.fieldMapping.put("insert_date", "xnat:imageScanData/meta/insert_date");
		this.fieldMapping.put("insert_user", "xnat:imageScanData/meta/insert_user/login");
		this.fieldMapping.put("last_modified", "xnat:imageScanData/meta/last_modified");

		this.fieldMapping.put("xsiType", "xnat:imageScanData/extension_item/element_name");
	}


	@Override
	public boolean allowPost() {
		return true;
	}

	@Override
	public void handlePost() {
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
						this.getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT,"Specified scan already exists.");
					return;
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

				this.returnSuccessfulCreateFromList(scan.getId());
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
	public ArrayList<String> getDefaultFields(GenericWrapperElement e) {
		ArrayList<String> al=new ArrayList<String>();
		al.add("xnat_imagescandata_id");
		al.add("ID");
		al.add("type");
		al.add("quality");
		al.add("xsiType");
		al.add("note");
		al.add("series_description");

		return al;
	}

	public String getDefaultElementName(){
		return "xnat:imageScanData";
	}

	@Override
	public Representation getRepresentation(Variant variant) {
		if(scan!=null){
			return new ItemXMLRepresentation(scan.getItem(),MediaType.TEXT_XML);
		}else{
			Representation rep=super.getRepresentation(variant);
			if(rep!=null)return rep;

			XFTTable table;
				try {
				final String re=this.getRootElementName();

				final QueryOrganizer qo = new QueryOrganizer(re, user,
						ViewManager.ALL);

				this.populateQuery(qo);

				CriteriaCollection cc= new CriteriaCollection("AND");
				cc.addClause(re+"/image_session_id", session.getId());
				qo.setWhere(cc);

				String query = qo.buildQuery();

					table=XFTTable.Execute(query, user.getDBName(), userName);

				table = formatHeaders(table, qo, "xnat:imageScanData/ID",
						String.format("/REST/experiments/%s/scans/",session.getId()));
				} catch (Exception e) {
				e.printStackTrace();
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
				return null;
				}

			MediaType mt = overrideVariant(variant);
			Hashtable<String, Object> params = new Hashtable<String, Object>();
			if (table != null)
				params.put("totalRecords", table.size());
			return this.representTable(table, mt, params);
		}
	}
}
