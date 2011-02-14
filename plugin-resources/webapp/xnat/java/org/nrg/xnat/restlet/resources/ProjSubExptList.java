// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;

import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.model.XnatExperimentdataShareI;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatExperimentdataShare;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatSubjectassessordata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.schema.SchemaElement;
import org.nrg.xdat.security.SecurityValues;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTable;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.db.ViewManager;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.InvalidValueException;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.search.QueryOrganizer;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xft.utils.ValidationUtils.ValidationResults;
import org.nrg.xnat.helpers.xmlpath.XMLPathShortcuts;
import org.nrg.xnat.restlet.actions.TriggerPipelines;
import org.nrg.xnat.restlet.util.XNATRestConstants;
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

		this.fieldMapping.putAll(XMLPathShortcuts.getInstance().getShortcuts(XMLPathShortcuts.EXPERIMENT_DATA,true));
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
							for(XnatExperimentdataShareI pp : expt.getSharing_share()){
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
								for(XnatExperimentdataShareI pp : expt.getSharing_share()){
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
						for(XnatExperimentdataShareI pp : expt.getSharing_share()){
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
					if(this.isQueryVariableTrue(XNATRestConstants.TRIGGER_PIPELINES) || this.containsAction(XNATRestConstants.TRIGGER_PIPELINES)){
						TriggerPipelines tp = new TriggerPipelines(expt,true,this.isQueryVariableTrue(XNATRestConstants.SUPRESS_EMAIL),user);
						tp.call();
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
				table=formatHeaders(table,qo,rootElementName+"/ID","/data/experiments/");

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
