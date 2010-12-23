// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatExperimentdataShare;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatProjectdataI;
import org.nrg.xdat.om.XnatSubjectassessordata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTable;
import org.nrg.xft.db.DBAction;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.InvalidValueException;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xft.utils.ValidationUtils.ValidationResults;
import org.nrg.xnat.archive.Rename;
import org.nrg.xnat.archive.Rename.DuplicateLabelException;
import org.nrg.xnat.archive.Rename.FolderConflictException;
import org.nrg.xnat.archive.Rename.LabelConflictException;
import org.nrg.xnat.archive.Rename.ProcessingInProgress;
import org.nrg.xnat.exceptions.InvalidArchiveStructure;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

public class SubjAssessmentResource extends SubjAssessmentAbst {
	XnatProjectdata proj=null;
	XnatSubjectdata subject=null;
	XnatSubjectassessordata expt = null;
	String exptID=null;
	XnatSubjectassessordata existing = null;
	
	public SubjAssessmentResource(Context context, Request request, Response response) {
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
		
		if(proj==null){
			response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
			return;
			}

			String subID= (String)request.getAttributes().get("SUBJECT_ID");
			if(subID!=null){
			subject = XnatSubjectdata.GetSubjectByProjectIdentifier(proj
					.getId(), subID, user, false);
				
				if(subject==null){
				subject = XnatSubjectdata.getXnatSubjectdatasById(subID, user,
						false);
				if (subject != null
						&& (proj != null && !subject.hasProject(proj.getId()))) {
					subject = null;
				}
				}
			}
			
			exptID= (String)request.getAttributes().get("EXPT_ID");
			if(exptID!=null){
			if (proj != null) {
				if (existing == null) {
					existing = (XnatSubjectassessordata) XnatExperimentdata
							.GetExptByProjectIdentifier(proj.getId(), exptID,
									user, false);
			}
			}

			if (existing == null) {
				existing = (XnatSubjectassessordata) XnatExperimentdata
						.getXnatExperimentdatasById(exptID, user, false);
				if (existing != null
						&& (proj != null && !existing.hasProject(proj.getId()))) {
					existing = null;
				}
			}

			this.getVariants().add(new Variant(MediaType.TEXT_HTML));
			this.getVariants().add(new Variant(MediaType.TEXT_XML));
		}else{
			response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
		}

		this.fieldMapping.put("ID", "xnat:experimentData/ID");
		this.fieldMapping.put("visit_id", "xnat:experimentData/visit_id");
		this.fieldMapping.put("date", "xnat:experimentData/date");
		this.fieldMapping.put("time", "xnat:experimentData/time");
		this.fieldMapping.put("note", "xnat:experimentData/note");
		this.fieldMapping.put("pi_firstname", "xnat:experimentData/investigator/firstname");
		this.fieldMapping.put("pi_lastname", "xnat:experimentData/investigator/lastname");
		this.fieldMapping.put("validation_method", "xnat:experimentData/validation/method");
		this.fieldMapping.put("validation_status", "xnat:experimentData/validation/status");
		this.fieldMapping.put("validation_date", "xnat:experimentData/validation/date");
		this.fieldMapping.put("validation_notes", "xnat:experimentData/validation/notes");

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
	}



	@Override
	public boolean allowPut() {
		return true;
	}

	@Override
	public void handlePut() {
	        XFTItem item = null;			

			try {
			XFTItem template=null;
			if (existing!=null){
				template=existing.getItem();
			}

			item=this.loadItem(null,true,template);
			
				if(item==null){
					String xsiType=this.getQueryVariable("xsiType");
					if(xsiType!=null){
						item=XFTItem.NewItem(xsiType, user);
					}
				}
				
				if(item==null){
				if(proj!=null){
					XnatSubjectassessordata om =(XnatSubjectassessordata)XnatSubjectassessordata.GetExptByProjectIdentifier(proj.getId(), this.exptID,user, false);
					if(om!=null){
						item=om.getItem();
					}
				}

				if(item==null){
					XnatSubjectassessordata om =(XnatSubjectassessordata)XnatSubjectassessordata.getXnatExperimentdatasById(this.exptID, null, false);
					if(om!=null){
						item=om.getItem();
					}
				}
			}

			if(item==null){
				this.getResponse().setStatus(Status.CLIENT_ERROR_EXPECTATION_FAILED, "Need PUT Contents");
					return;
				}
				
				if(item.instanceOf("xnat:subjectAssessorData")){
					expt = (XnatSubjectassessordata)BaseElement.GetGeneratedItem(item);
					
				if(filepath!=null && !filepath.equals("")){
					if(filepath.startsWith("projects/")){
						String newProjectS= filepath.substring(9);
						XnatProjectdata newProject=XnatProjectdata.getXnatProjectdatasById(newProjectS, user, false);
						String newLabel = this.getQueryVariable("label");
						if(newProject!=null){
							if(expt.getProject().equals(newProject.getId())){
								this.getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT,"Already assigned to project:"+ newProject.getId());
								return;
							}
							
							if(!user.canRead(expt)){
								this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Specified user account has insufficient priviledges for experiments in this project.");
								return;
							}

							int index=0;
							XnatExperimentdataShare matched=null;
							for(XnatExperimentdataShare pp : expt.getSharing_share()){
								if(pp.getProject().equals(newProject.getId())){
									matched=pp;
									if(newLabel!=null && !pp.getLabel().equals(newLabel)){
										pp.setLabel(newLabel);
										pp.save(user,false,false);
									}
									break;
								}
								index++;
							}

							if(this.getQueryVariable("primary")!=null && this.getQueryVariable("primary").equals("true")){
								if(newLabel==null || newLabel.equals(""))newLabel=expt.getLabel();
								if(newLabel==null || newLabel.equals(""))newLabel=expt.getId();

								
								if(!user.canDelete(expt)){
									this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Specified user account has insufficient priviledges for experiments in this project.");
									return;
								}
								
								XnatExperimentdata match=XnatExperimentdata.GetExptByProjectIdentifier(newProject.getId(), newLabel,user, false);
								if(match!=null){
									this.getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT,"Specified label is already in use.");
									return;
								}

								expt.moveToProject(newProject,newLabel,user);

								if(matched!=null){
									DBAction.RemoveItemReference(expt.getItem(), "xnat:experimentData/sharing/share", matched.getItem(), user);
									expt.removeSharing_share(index);
								}
							}else{
								if(matched==null){

									if(newLabel!=null){
										XnatExperimentdata temp=XnatExperimentdata.GetExptByProjectIdentifier(newProject.getId(), newLabel, null, false);
										if(temp!=null){
											this.getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT,"Label already in use:"+ newLabel);
											return;
										}
									}
										if(user.canCreate(expt.getXSIType()+"/project", newProject.getId())){
											XnatExperimentdataShare pp= new XnatExperimentdataShare((UserI)user);
											pp.setProject(newProject.getId());
											if(newLabel!=null)pp.setLabel(newLabel);
											pp.setProperty("sharing_share_xnat_experimentda_id", expt.getId());
											pp.save(user, false, false);
										}else{
											this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Specified user account has insufficient create priviledges for experiments in the " + newProject.getId() + " project.");
											return;
										}
								}else{
									this.getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT,"Already assigned to project:"+ newProject.getId());
									return;
								}
							}
							

							this.returnDefaultRepresentation();
						}else{
							this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Unable to identify project: " + newProjectS);
							return;
						}
					}else{
							this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
							return;
						}
				}else{
					if(expt.getLabel()==null){
						expt.setLabel(this.exptID);
					}
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
					
					//FIND PRE-EXISTING
					if(existing==null){
						if(expt.getId()!=null){
							existing=(XnatSubjectassessordata)XnatExperimentdata.getXnatExperimentdatasById(expt.getId(), null, completeDocument);
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

							if(subject==null && existing!=null){
								this.subject=existing.getSubjectData();
								expt.setSubjectId(subject.getId());
					}
					
							if(this.subject==null){
								this.subject = new XnatSubjectdata((UserI)user);
								this.subject.setProject(this.proj.getId());
								this.subject.setLabel(expt.getSubjectId());
								this.subject.setId(XnatSubjectdata.CreateNewID());
								if(!user.canCreate(this.subject)){
									this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Specified user account has insufficient create priveledges for subjects in this project.");
									return;
					}
								this.subject.save(user, false, true);
								expt.setSubjectId(this.subject.getId());
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
						if(expt.getId()==null || expt.getId().equals("")){
							expt.setId(existing.getId());
						}

						//MATCHED
						if(!existing.getProject().equals(expt.getProject())){
							this.getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT,"Project must be modified through seperate URI.");
							return;
						}

						if(!user.canEdit(expt)){
							this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Specified user account has insufficient edit priviledges for experiments in this project.");
							return;
						}

						if(this.getQueryVariable("label")!=null && !this.getQueryVariable("label").equals("") ){
							if(!expt.getLabel().equals(existing.getLabel())){
								expt.setLabel(existing.getLabel());
						}

							String label=this.getQueryVariable("label");

							if(!label.equals(existing.getLabel())){
								XnatExperimentdata match=XnatExperimentdata.GetExptByProjectIdentifier(proj.getId(), label,user, false);
								if(match!=null){
									this.getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT,"Specified label is already in use.");
									return;
								}

								Rename renamer = new Rename(proj,existing,label,user);
								try {
									renamer.call();
								} catch (ProcessingInProgress e) {
									logger.error("", e);
									this.getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT,"Specified session is being processed (" + e.getPipeline_name() +").");
									return;
								} catch (DuplicateLabelException e) {
									logger.error("", e);
									this.getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT,"Specified label is already in use.");
									return;
								} catch (LabelConflictException e) {
									logger.error("", e);
									this.getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT,"Specified label is already in use.");
									return;
								} catch (FolderConflictException e) {
									logger.error("", e);
									this.getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT,"File system destination contains pre-existing files");
									return;
								} catch (InvalidArchiveStructure e) {
									logger.error("", e);
									this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,"Non-standard archive structure in existing experiment directory.");
									return;
								} catch (URISyntaxException e) {
									logger.error("", e);
									this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,"Non-standard archive structure in existing experiment directory.");
									return;
								} catch (Exception e) {
									logger.error("", e);
									this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e.getMessage());
									return;
								}
							}
							return;
						}
					}

					if(this.subject==null){
						this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY,"Submitted experiment record must include the subject.");
						return;
					}
					
					boolean allowDataDeletion=false;
					if(this.getQueryVariable("allowDataDeletion")!=null && this.getQueryVariable("allowDataDeletion").equals("true")){
						allowDataDeletion=true;
					}
					
					if(this.getQueryVariable("fixScanTypes")!=null && this.getQueryVariable("fixScanTypes").equals("true")){
						if(expt instanceof XnatImagesessiondata){
							((XnatImagesessiondata)expt).fixScanTypes();
							((XnatImagesessiondata)expt).defaultQuality("usable");
						}
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
						user.clearLocalCache();
					MaterializedView.DeleteByUser(user);

						if(this.proj.getArcSpecification().getQuarantineCode()!=null && this.proj.getArcSpecification().getQuarantineCode().equals(1)){
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
						if(this.isQueryVariableTrue("pullDataFromHeaders") && expt instanceof XnatImagesessiondata){
							try {
								pullDataFromHeaders((XnatImagesessiondata)expt,user,this.allowDataDeletion(),this.isQueryVariableTrue("overwrite"));
							} catch (Exception e) {
								logger.error("",e);
								this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,e.getMessage());
								return;
							}
						}
						
						if(this.isQueryVariableTrue("triggerPipelines")){
							triggerPipelines(expt,true,this.isQueryVariableTrue("supressEmail"),user);
						}
					}
				}

				this.returnString(expt.getId());
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
	public boolean allowDelete() {
		return true;
	}

	@Override
	public void handleDelete(){

			if(expt==null&& exptID!=null){
				expt=(XnatSubjectassessordata)XnatExperimentdata.getXnatExperimentdatasById(exptID, user, false);
				
				if(expt==null && this.proj!=null){
				expt=(XnatSubjectassessordata)XnatExperimentdata.GetExptByProjectIdentifier(this.proj.getId(), exptID,user, false);
				}
			}
			
			if(expt==null){
			this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Unable to find the specified experiment.");
				return;
			}
			
		XnatProjectdata newProject=null;
			
		if(filepath!=null && !filepath.equals("")){
			if(filepath.startsWith("projects/")){
				String newProjectS= filepath.substring(9);
				newProject=XnatProjectdata.getXnatProjectdatasById(newProjectS, user, false);
				if(newProject==null){
					this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Unable to identify project: " + newProjectS);
					return;
				}
			}else{
					this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
					return;
	                }
		}else if(!expt.getProject().equals(proj.getId())){
			newProject=proj;
	            }
	            
		String msg=expt.delete((newProject!=null)?newProject:proj, user, this.isQueryVariableTrue("removeFiles"));
		if(msg!=null){
			this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,msg);
			return;
		}
	}

	@Override
	public Representation getRepresentation(Variant variant) {	
		MediaType mt = overrideVariant(variant);

		if(expt==null&& exptID!=null){
			expt=(XnatSubjectassessordata)XnatExperimentdata.getXnatExperimentdatasById(exptID, user, false);
			
			if(expt==null && this.proj!=null){
				expt=(XnatSubjectassessordata)XnatExperimentdata.GetExptByProjectIdentifier(this.proj.getId(), exptID,user, false);
			}
		}
		
		if(expt!=null){
			if(filepath!=null && !filepath.equals("") && filepath.equals("status")){
				try {
					if(expt.needsActivation()){
					    return new StringRepresentation("quarantine",mt);
					}else{
					    return new StringRepresentation("active",mt);
					}
				} catch (Exception e) {
					logger.error("",e);
				    return new StringRepresentation("active",mt);
			}
			}else if(filepath!=null && !filepath.equals("") && filepath.startsWith("projects")){
				XFTTable t = new XFTTable();
				ArrayList al = new ArrayList();
				al.add("label");
				al.add("ID");
				al.add("Secondary_ID");
				al.add("Name");
				t.initTable(al);

				Object[] row=new Object[4];
				row[0]=expt.getLabel();
				XnatProjectdata primary = expt.getPrimaryProject(false);
				row[1]=primary.getId();
				row[2]=primary.getSecondaryId();
				row[3]=primary.getName();
				t.rows().add(row);

				for(Map.Entry<XnatProjectdataI, String> entry: expt.getProjectDatas().entrySet()){
					row=new Object[4];
					row[0]=entry.getValue();
					row[1]=entry.getKey().getId();
					row[2]=entry.getKey().getSecondaryId();
					row[3]=entry.getKey().getName();
					t.rows().add(row);
				}

				return representTable(t, mt, new Hashtable<String,Object>());
		}else{
				return this.representItem(expt.getItem(),mt);
			}
		} else {
			this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,
					"Unable to find the specified experiment.");
			return null;
		}

	}

}
