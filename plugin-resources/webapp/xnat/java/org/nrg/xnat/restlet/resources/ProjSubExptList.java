// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatExperimentdataShare;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatSubjectassessordata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.schema.SchemaElement;
import org.nrg.xdat.security.ElementSecurity;
import org.nrg.xdat.security.SecurityValues;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTable;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.db.ViewManager;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.InvalidValueException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.schema.design.SchemaElementI;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.search.QueryOrganizer;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xft.utils.ValidationUtils.ValidationResults;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

public class ProjSubExptList extends SubjAssessmentAbst {
	XnatProjectdata proj=null;
	XnatSubjectdata subject=null;

	String pID=null;
	String subID=null;
	public ProjSubExptList(Context context, Request request, Response response) {
		super(context, request, response);

		pID = (String) request.getAttributes().get("PROJECT_ID");
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

			if (proj == null) {
				response.setStatus(Status.CLIENT_ERROR_NOT_FOUND,
						"Unable to identify project " + pID);
				return;
			}

			subID = (String) request.getAttributes().get("SUBJECT_ID");
				if(subID!=null){
				subject = XnatSubjectdata.GetSubjectByProjectIdentifier(proj
						.getId(), subID, user, false);

					if(subject==null){
					subject = XnatSubjectdata.getXnatSubjectdatasById(subID,
							user, false);
					if (subject != null
							&& (proj != null && !subject.hasProject(proj
									.getId()))) {
						subject = null;
					}
					}

					if(subject!=null){
					this.getVariants().add(
							new Variant(MediaType.APPLICATION_JSON));
						this.getVariants().add(new Variant(MediaType.TEXT_HTML));
						this.getVariants().add(new Variant(MediaType.TEXT_XML));
					}else{
					response.setStatus(Status.CLIENT_ERROR_NOT_FOUND,
							"Unable to identify subject " + subID);
					}
				}else{
				this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
				this.getVariants().add(new Variant(MediaType.TEXT_HTML));
				this.getVariants().add(new Variant(MediaType.TEXT_XML));
			}
		}else{
			response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
		}

		this.fieldMapping.put("ID", "xnat:experimentdata/ID");
		this.fieldMapping.put("visit_id", "xnat:experimentdata/visit_id");
		this.fieldMapping.put("date", "xnat:experimentdata/date");
		this.fieldMapping.put("time", "xnat:experimentdata/time");
		this.fieldMapping.put("note", "xnat:experimentdata/note");
		this.fieldMapping.put("pi_firstname", "xnat:experimentdata/investigator/firstname");
		this.fieldMapping.put("pi_lastname", "xnat:experimentdata/investigator/lastname");
		this.fieldMapping.put("validation_method", "xnat:experimentdata/validation/method");
		this.fieldMapping.put("validation_status", "xnat:experimentdata/validation/status");
		this.fieldMapping.put("validation_date", "xnat:experimentdata/validation/date");
		this.fieldMapping.put("validation_notes", "xnat:experimentdata/validation/notes");
		this.fieldMapping.put("project", "xnat:experimentdata/project");
		this.fieldMapping.put("label", "xnat:experimentdata/label");

		this.fieldMapping.put("scanner", "xnat:imageSessionData/scanner");
		this.fieldMapping.put("operator", "xnat:imageSessionData/operator");
		this.fieldMapping.put("dcmAccessionNumber", "xnat:imageSessionData/dcmAccessionNumber");
		this.fieldMapping.put("dcmPatientId", "xnat:imageSessionData/dcmPatientId");
		this.fieldMapping.put("dcmPatientName", "xnat:imageSessionData/dcmPatientName");
		this.fieldMapping.put("session_type", "xnat:imageSessionData/session_type");
		this.fieldMapping.put("modality", "xnat:imageSessionData/modality");
		this.fieldMapping.put("UID", "xnat:imageSessionData/UID");

		this.fieldMapping.put("coil", "xnat:mrSessionData/coil");
		this.fieldMapping.put("fieldStrength", "xnat:mrSessionData/fieldStrength");
		this.fieldMapping.put("marker", "xnat:mrSessionData/marker");
		this.fieldMapping.put("stabilization", "xnat:mrSessionData/stabilization");

		this.fieldMapping.put("studyType", "xnat:petSessionData/studyType");
		this.fieldMapping.put("patientID", "xnat:petSessionData/patientID");
		this.fieldMapping.put("patientName", "xnat:petSessionData/patientName");
		this.fieldMapping.put("stabilization", "xnat:petSessionData/stabilization");
		this.fieldMapping.put("scan_start_time", "xnat:petSessionData/start_time/scan");
		this.fieldMapping.put("injection_start_time", "xnat:petSessionData/start_time/injection");
		this.fieldMapping.put("tracer_name", "xnat:petSessionData/tracer/name");
		this.fieldMapping.put("tracer_startTime", "xnat:petSessionData/tracer/startTime");
		this.fieldMapping.put("tracer_dose", "xnat:petSessionData/tracer/dose");
		this.fieldMapping.put("tracer_sa", "xnat:petSessionData/tracer/specificActivity");
		this.fieldMapping.put("tracer_totalmass", "xnat:petSessionData/tracer/totalMass");
		this.fieldMapping.put("tracer_intermediate", "xnat:petSessionData/tracer/intermediate");
		this.fieldMapping.put("tracer_isotope", "xnat:petSessionData/tracer/isotope");
		this.fieldMapping.put("tracer_isotope", "xnat:petSessionData/tracer/isotope/half-life");
		this.fieldMapping.put("tracer_transmissions", "xnat:petSessionData/tracer/transmissions");
		this.fieldMapping.put("tracer_transmissions_start", "xnat:petSessionData/tracer/transmissions_starttime");

		this.fieldMapping.put("subject_ID", "xnat:subjectassessordata/subject_id");
		this.fieldMapping.put("subject_label", "xnat:subjectdata/label");
		this.fieldMapping.put("subject_project", "xnat:subjectdata/project");

		this.fieldMapping.put("session_ID", "xnat:imagesessiondata/id");
		this.fieldMapping.put("session_label", "xnat:imagesessiondata/label");
		this.fieldMapping.put("session_project", "xnat:imagesessiondata/project");

		this.fieldMapping.put("insert_date", "xnat:experimentData/meta/insert_date");
		this.fieldMapping.put("insert_user", "xnat:experimentData/meta/insert_user/login");
		this.fieldMapping.put("last_modified", "xnat:experimentData/meta/last_modified");
		this.fieldMapping.put("xsiType", "xnat:experimentData/extension_item/element_name");
	}

	@Override
	public boolean allowPost() {
		return true;
	}

	@Override
	public void handlePost() {
	        XFTItem item = null;

			try {
			item=this.loadItem(null,true);

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

				if(item.instanceOf("xnat:subjectAssessorData")){
					XnatSubjectassessordata expt = (XnatSubjectassessordata)BaseElement.GetGeneratedItem(item);

					//MATCH PROJECT
					if(this.proj==null && expt.getProject()!=null){
						proj = XnatProjectdata.getXnatProjectdatasById(expt.getProject(), user, false);
					}

					if(this.proj!=null){
						if(expt.getProject()==null || expt.getProject().equals("")){
							expt.setProject(this.proj.getId());
						}else if(expt.getProject().equals(this.proj.getId())){
						}else{
							boolean matched=false;
							for(XnatExperimentdataShare pp : expt.getSharing_share()){
								if(pp.getProject().equals(this.proj.getId())){
									matched=true;
									break;
								}
							}

							if(!matched){
								XnatExperimentdataShare pp= new XnatExperimentdataShare((UserI)user);
								pp.setProject(this.proj.getId());
								expt.setSharing_share(pp);
							}
						}
					}else{
					this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY,"Submitted experiment record must include the project attribute.");
						return;
					}

					//MATCH SUBJECT
					if(this.subject!=null){
						expt.setSubjectId(this.subject.getId());
					}else{
						if(expt.getSubjectId()!=null && !expt.getSubjectId().equals("")){
							this.subject=XnatSubjectdata.getXnatSubjectdatasById(expt.getSubjectId(), user, false);

							if(this.subject==null && expt.getProject()!=null && expt.getLabel()!=null){
							this.subject=XnatSubjectdata.GetSubjectByProjectIdentifier(expt.getProject(), expt.getSubjectId(),user, false);
							}

							if(this.subject==null){
								for(XnatExperimentdataShare pp : expt.getSharing_share()){
								this.subject=XnatSubjectdata.GetSubjectByProjectIdentifier(pp.getProject(), expt.getSubjectId(),user, false);
									if(this.subject!=null){
										break;
									}
								}
							}

						if(this.subject==null){
							this.subject = new XnatSubjectdata((UserI)user);
							this.subject.setProject(this.proj.getId());
							this.subject.setLabel(expt.getSubjectId());
							this.subject.setId(XnatSubjectdata.CreateNewID());
							this.subject.save(user, false, true);
							expt.setSubjectId(this.subject.getId());
						}
					}
				}

				if(this.subject==null){
					this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY,"Submitted experiment record must include the subject.");
					return;
				}

					//FIND PRE-EXISTING
					XnatSubjectassessordata existing=null;
					if(expt.getId()!=null){
						existing=(XnatSubjectassessordata)XnatExperimentdata.getXnatExperimentdatasById(expt.getId(), user, completeDocument);
					}

					if(existing==null && expt.getProject()!=null && expt.getLabel()!=null){
					existing=(XnatSubjectassessordata)XnatExperimentdata.GetExptByProjectIdentifier(expt.getProject(), expt.getLabel(),user, completeDocument);
					}

					if(existing==null){
						for(XnatExperimentdataShare pp : expt.getSharing_share()){
						existing=(XnatSubjectassessordata)XnatExperimentdata.GetExptByProjectIdentifier(pp.getProject(), pp.getLabel(),user, completeDocument);
							if(existing!=null){
								break;
							}
						}
					}

					if(existing==null){
						if(!user.canCreate(expt)){
						this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Specified user account has insufficient create priviledges for experiments in this project.");
						return;
						}
						//IS NEW
						if(expt.getId()==null || expt.getId().equals("")){
						expt.setId(XnatExperimentdata.CreateNewID());
						}
					}else{
						this.getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT,"Specified experiment already exists.");
					return;
						//MATCHED
					}

					boolean allowDataDeletion=false;
					if(this.getQueryVariable("allowDataDeletion")!=null && this.getQueryVariable("allowDataDeletion").equals("true")){
						allowDataDeletion=true;
					}

				if(!StringUtils.IsEmpty(expt.getLabel()) && !StringUtils.IsAlphaNumericUnderscore(expt.getId())){
					this.getResponse().setStatus(Status.CLIENT_ERROR_EXPECTATION_FAILED,"Invalid character in experiment label.");
					return;
				}



				final ValidationResults vr = expt.validate();

	            if (vr != null && !vr.isValid())
	            {
	            	this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,vr.toFullString());
					return;
	            }

				if(expt.save(user,false,allowDataDeletion)){
					MaterializedView.DeleteByUser(user);

					if(this.proj.getArcSpecification().getQuarantineCode().equals(1)){
						expt.quarantine(user);
					}
				}

				if(this.getQueryVariable("activate")!=null && this.getQueryVariable("activate").equals("true")){
					if(user.canActivate(expt.getItem()))expt.activate(user);
					else this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Specified user account has insufficient activation priviledges for experiments in this project.");
				}

				if(this.getQueryVariable("quarantine")!=null && this.getQueryVariable("quarantine").equals("true")){
					if(user.canActivate(expt.getItem()))expt.quarantine(user);
					else this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Specified user account has insufficient activation priviledges for experiments in this project.");
				}

				if(user.canEdit(expt.getItem())){
					if(this.isQueryVariableTrue("triggerPipelines")){
						triggerPipelines(expt,true,this.isQueryVariableTrue("supressEmail"),user);
					}
				}

				this.returnSuccessfulCreateFromList(expt.getId());
				}else{
					this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY,"Only xnat:Subject documents can be PUT to this address.");
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
	public Representation getRepresentation(Variant variant) {
		Representation rep=super.getRepresentation(variant);
		if(rep!=null)return rep;

		XFTTable table = null;

		if(proj==null){
			this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Unable to identify project " + pID);
			return null;
		}

		try {
			final SecurityValues values = new SecurityValues();
			values.put("xnat:subjectData/project", proj.getId());
			values.put("xnat:subjectData/sharing/share/project", proj.getId());

			final SchemaElement se= SchemaElement.GetElement(XnatSubjectdata.SCHEMA_ELEMENT_NAME);

			if (!user.canReadByXMLPath(se,values))
			{
				this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Unable to read experiments for Project: " + proj.getId());
				return null;
			}
		} catch (Exception e1) {
			logger.error("", e1);
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			return null;
		}

		if(subject!=null){
			subID=subject.getId();
		}

		try {
			final String rootElementName=this.getRootElementName();
			final QueryOrganizer qo = new QueryOrganizer(rootElementName,user,ViewManager.ALL);

			this.populateQuery(qo);

			CriteriaCollection where=new CriteriaCollection("AND");

			CriteriaCollection cc= new CriteriaCollection("OR");
			cc.addClause(rootElementName+"/project", proj.getId());
			cc.addClause(rootElementName+"/sharing/share/project", proj.getId());
			where.addClause(cc);

			if(subID!=null){
				CriteriaCollection cc2= new CriteriaCollection("AND");
				cc2.addClause("xnat:subjectAssessorData/subject_id", subID);
				where.addClause(cc2);
			}

			qo.setWhere(where);

			String query=qo.buildQuery();

			table=XFTTable.Execute(query, user.getDBName(), userName);

			if(table.size()>0){
				table=formatHeaders(table,qo,rootElementName+"/ID","/REST/experiments/");

				final Integer labelI=table.getColumnIndex("label");
				final Integer idI=table.getColumnIndex(rootElementName+"/ID");
				if(labelI!=null && idI!=null){
					final XFTTable t= XFTTable.Execute("SELECT sharing_share_xnat_experimentda_id as id,label FROM xnat_experimentData_share WHERE project='"+ proj.getId() + "'", user.getDBName(), user.getUsername());
					final Hashtable lbls=t.toHashtable("id", "label");
					for(Object[] row:table.rows()){
						final String id=(String)row[idI];
						if(lbls.containsKey(id)){
							final String lbl=(String)lbls.get(id);
							if(null!=lbl && !lbl.equals("")){
								row[labelI]=lbl;
							}
						}
					}
				}
			}
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (DBPoolException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}

		Hashtable<String,Object> params=new Hashtable<String,Object>();
		if (table != null)
			params.put("totalRecords", table.size());
		return this.representTable(table, overrideVariant(variant), params);
	}
}
