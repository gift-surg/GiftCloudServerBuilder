package org.nrg.xnat.restlet.resources;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.Logger;
import org.nrg.dcm.CopyOp;
import org.nrg.transaction.OperationI;
import org.nrg.transaction.TransactionException;
import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.model.XnatExperimentdataShareI;
import org.nrg.xdat.model.XnatProjectdataI;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatExperimentdataShare;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatPvisitdata;
import org.nrg.xdat.om.XnatSubjectassessordata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTable;
import org.nrg.xft.db.DBAction;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.exception.InvalidValueException;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xft.utils.ValidationUtils.ValidationResults;
import org.nrg.xnat.archive.Rename;
import org.nrg.xnat.archive.ValidationException;
import org.nrg.xnat.archive.Rename.DuplicateLabelException;
import org.nrg.xnat.archive.Rename.FolderConflictException;
import org.nrg.xnat.archive.Rename.LabelConflictException;
import org.nrg.xnat.archive.Rename.ProcessingInProgress;
import org.nrg.xnat.exceptions.InvalidArchiveStructure;
import org.nrg.xnat.helpers.merge.ProjectAnonymizer;
import org.nrg.xnat.helpers.xmlpath.XMLPathShortcuts;
import org.nrg.xnat.restlet.actions.FixScanTypes;
import org.nrg.xnat.restlet.actions.PullSessionDataFromHeaders;
import org.nrg.xnat.restlet.actions.TriggerPipelines;
import org.nrg.xnat.restlet.util.XNATRestConstants;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.xml.sax.SAXException;

public class SubjVisitResource extends QueryOrganizerResource {

	static Logger logger = Logger.getLogger(SubjVisitResource.class);

	XnatProjectdata proj=null;
	XnatSubjectdata subject=null;
//	XnatPvisitdata visit = null;
	String visitID=null;
	XnatPvisitdata existing;



	public String getDefaultElementName(){
		return "xnat:pVisitData";
	}

	@Override
	public ArrayList<String> getDefaultFields(GenericWrapperElement e) {
		ArrayList<String> al=new ArrayList<String>();

		al.add("ID");
		al.add("project");
		al.add("date");
		al.add("xsiType");
		al.add("label");
		al.add("insert_date");
		al.add("subject_ID");
		al.add("visit_type");
		al.add("visit_name");
		al.add("closed");

		return al;
	}	

	public SubjVisitResource(Context context, Request request, Response response) {
		super(context, request, response);

		String pID= (String)request.getAttributes().get("PROJECT_ID");
		if(pID!=null){
			proj = XnatProjectdata.getProjectByIDorAlias(pID, user, false);
		}

		if(proj==null){
			response.setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Project " + pID + " not found.");
			return;
		}

		String subID= (String)request.getAttributes().get("SUBJECT_ID");
		if(subID!=null){
			subject = XnatSubjectdata.GetSubjectByProjectIdentifier(proj.getId(), subID, user, false);

			if(subject==null){
				subject = XnatSubjectdata.getXnatSubjectdatasById(subID, user, false);
				if (subject != null && (proj != null && !subject.hasProject(proj.getId()))) {
					subject = null;
				}
			}
		}
		if(subject == null){
			response.setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Subject " + subID + " not found.");
		}
		
		visitID= (String)request.getAttributes().get("VISIT_ID");
		if(visitID!=null){
			existing = (XnatPvisitdata) XnatPvisitdata.GetVisitByProjectIdentifier(proj.getId(), visitID, user, false);
			if (existing == null) {
				existing = (XnatPvisitdata) XnatPvisitdata.getXnatPvisitdatasById(visitID, user, false);
				if (existing != null && !existing.hasProject(proj.getId())) {
					existing = null;
				}
			}		
			
			
			this.getVariants().add(new Variant(MediaType.TEXT_HTML));
			this.getVariants().add(new Variant(MediaType.TEXT_XML));
		}else{
			response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Visit ID is required for this resource.");
		}
		this.fieldMapping.putAll(XMLPathShortcuts.getInstance().getShortcuts(XMLPathShortcuts.VISIT_DATA,false));
	}
	@Override
	public boolean allowPut() {
		return true;
	}

	private XnatSubjectdata getExistingSubject(XnatProjectdata proj, String subjectId){
		// First check if the subject is associated with the project,
		// if that fails check the global pool.
		XnatSubjectdata s = XnatSubjectdata.GetSubjectByProjectIdentifier(proj.getId(), subjectId, user, false);
		if(s==null){
			s = XnatSubjectdata.getXnatSubjectdatasById(subjectId, user,false);
		}
		return s;
	}

	private XnatSubjectdata createSubject(XnatProjectdata proj, String subjectId, XDATUser user)  throws ResourceException, Exception {
		XnatSubjectdata s = new XnatSubjectdata((UserI)user);
		s.setProject(this.proj.getId());
		s.setLabel(subjectId);
		s.setId(XnatSubjectdata.CreateNewID());
		if(!user.canCreate(s)){
			throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN);
		}
		return s;
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
				XnatPvisitdata om =(XnatPvisitdata)XnatPvisitdata.GetVisitByProjectIdentifier(proj.getId(), this.visitID,user, false);
				if(om!=null){
					item=om.getItem();
				}

				if(item==null){
					om =(XnatPvisitdata)XnatPvisitdata.getXnatPvisitdatasById(this.visitID, null, false);
					if(om!=null){
						item=om.getItem();
					}
				}
			}

			if(item==null){
				this.getResponse().setStatus(Status.CLIENT_ERROR_EXPECTATION_FAILED, "Need PUT Contents");
				return;
			}

			if(!item.instanceOf("xnat:pVisitData")){
				this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY,"Only xnat:Pvisitdata documents can be PUT to this address.");
			}
			XnatPvisitdata visit = (XnatPvisitdata)BaseElement.GetGeneratedItem(item);

			if(visit.getLabel()==null){
				visit.setLabel(this.visitID);
			}
			//MATCH PROJECT

			if(visit.getProject()==null || visit.getProject().equals("")){
				visit.setProject(this.proj.getId());
			}else if(visit.getProject().equals(this.proj.getId())){
			}else{
				boolean matched=false;
				for(XnatExperimentdataShareI pp : visit.getSharing_share()){
					if(pp.getProject().equals(this.proj.getId())){
						matched=true;
						break;
					}
				}

				if(!matched){
					XnatExperimentdataShare pp= new XnatExperimentdataShare((UserI)user);
					pp.setProject(this.proj.getId());
					visit.setSharing_share(pp);
				}
			}

			//FIND PRE-EXISTING
			if(existing==null){
				if(visit.getId()!=null){
					existing=(XnatPvisitdata)XnatPvisitdata.getXnatPvisitdatasById(visit.getId(), null, completeDocument);
				}

				if(existing==null && visit.getProject()!=null && visit.getLabel()!=null){
					existing=(XnatPvisitdata)XnatPvisitdata.GetVisitByProjectIdentifier(visit.getProject(), visit.getLabel(),user, completeDocument);
				}

				if(existing==null){
					for(XnatExperimentdataShareI pp : visit.getSharing_share()){
						existing=(XnatPvisitdata)XnatPvisitdata.GetVisitByProjectIdentifier(pp.getProject(), pp.getLabel(),user, completeDocument);
						if(existing!=null){
							break;
						}
					}
				}
			}

			//MATCH SUBJECT
			if(this.subject!=null){
				visit.setSubjectId(this.subject.getId());
			}else{
				if(StringUtils.IsEmpty(visit.getSubjectId()) && org.apache.commons.lang.StringUtils.isNotEmpty(subject.getId())){
					visit.setSubjectId(this.subject.getId());
				}

				if(visit.getSubjectId()!=null && !visit.getSubjectId().equals("")){
					this.subject=XnatSubjectdata.getXnatSubjectdatasById(visit.getSubjectId(), user, false);

					if(this.subject==null && visit.getProject()!=null && visit.getLabel()!=null){
						this.subject=XnatSubjectdata.GetSubjectByProjectIdentifier(visit.getProject(), visit.getSubjectId(),user, false);
					}

					if(this.subject==null){
						for(XnatExperimentdataShareI pp : visit.getSharing_share()){
							this.subject=XnatSubjectdata.GetSubjectByProjectIdentifier(pp.getProject(), visit.getSubjectId(),user, false);
							if(this.subject!=null){
								break;
							}
						}
					}

					if(subject==null && existing!=null){
						this.subject=existing.getSubjectData();
						visit.setSubjectId(subject.getId());
					}

					if(this.subject==null){
						this.subject = new XnatSubjectdata((UserI)user);
						this.subject.setProject(this.proj.getId());
						this.subject.setLabel(visit.getSubjectId());
						this.subject.setId(XnatSubjectdata.CreateNewID());
						if(!user.canCreate(this.subject)){
							this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Specified user account has insufficient create priveledges for subjects in this project.");
							return;
						}
						this.subject.save(user, false, true);
						visit.setSubjectId(this.subject.getId());
					}
				}
			}


			if(existing==null){
				if(!user.canCreate(visit)){
					this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Specified user account has insufficient create priviledges for experiments in this project.");
					return;
				}
				//IS NEW
				if(visit.getId()==null || visit.getId().equals("")){
					visit.setId(XnatPvisitdata.CreateNewID());
				}
			}else{
				if(visit.getId()==null || visit.getId().equals("")){
					visit.setId(existing.getId());
				}

				//MATCHED
				if(!existing.getProject().equals(visit.getProject())){
					this.getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT,"Project must be modified through seperate URI.");
					return;
				}

				if(!user.canEdit(visit)){
					this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Specified user account has insufficient edit priviledges for visits in this project.");
					return;
				}

				if(this.getQueryVariable("subject_ID")!=null && !this.getQueryVariable("subject_ID").equals("") ){
					if (!visit.getSubjectId().equals(this.getQueryVariable("subject_ID"))) {
						XnatSubjectdata s = this.getExistingSubject(proj,
								this.getQueryVariable("subject_ID"));
						if (s != null) {
							// \"subject_ID\" can be overloaded on both the subject's label
							// and XNAT unique subject identifier
							if (!visit.getSubjectId().equals(s.getId())) {
								// only accept subjects that are associated with this project
								if (s.hasProject(proj.getId())){
									visit.setSubjectId(s.getId());
								}
							}
						}
						else {
							try {
								XnatSubjectdata new_s = this.createSubject(proj, this.getQueryVariable("subject_ID"), user);
								new_s.save(user, false, true);
								visit.setSubjectId(new_s.getId());
							} 
							catch (ResourceException e) {
								this.getResponse().setStatus(e.getStatus(), "Specified user account has insufficient create privileges for subjects in this project.");
							} 	
						}
					}						
				}

				if(this.getQueryVariable("label")!=null && !this.getQueryVariable("label").equals("") ){
					if(!visit.getLabel().equals(existing.getLabel())){
						visit.setLabel(existing.getLabel());
					}
					String label=this.getQueryVariable("label");

					if(!label.equals(existing.getLabel())){
						XnatPvisitdata match=XnatPvisitdata.GetVisitByProjectIdentifier(proj.getId(), label,user, false);
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

			boolean allowDataDeletion=false;
			if(this.getQueryVariable("allowDataDeletion")!=null && this.getQueryVariable("allowDataDeletion").equals("true")){
				allowDataDeletion=true;
			}

			if(!StringUtils.IsEmpty(visit.getLabel()) && !StringUtils.IsAlphaNumericUnderscore(visit.getId())){
				this.getResponse().setStatus(Status.CLIENT_ERROR_EXPECTATION_FAILED,"Invalid character in visit label.");
				return;
			}

			final ValidationResults vr = visit.validate();

			if (vr != null && !vr.isValid())
			{
				this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,vr.toFullString());
				return;
			}

			if(visit.save(user,false,allowDataDeletion)){
				user.clearLocalCache();
				MaterializedView.DeleteByUser(user);

				if(this.proj.getArcSpecification().getQuarantineCode()!=null && this.proj.getArcSpecification().getQuarantineCode().equals(1)){
					visit.quarantine(user);
				}
			}

			if(this.getQueryVariable("activate")!=null && this.getQueryVariable("activate").equals("true")){
				if(user.canActivate(visit.getItem()))visit.activate(user);
				else this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Specified user account has insufficient activation priviledges for experiments in this project.");
			}

			if(this.getQueryVariable("quarantine")!=null && this.getQueryVariable("quarantine").equals("true")){
				if(user.canActivate(visit.getItem()))visit.quarantine(user);
				else this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Specified user account has insufficient activation priviledges for experiments in this project.");
			}

			this.returnString(visit.getId(),(existing==null)?Status.SUCCESS_CREATED:Status.SUCCESS_OK);
			
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
		if(existing==null){
			this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Unable to find the specified visit.");
			return;
		}

		String msg=existing.delete(proj, user,false);
		if(msg!=null){
			this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,msg);
			return;
		}
	}

	@Override
	public Representation getRepresentation(Variant variant) {	
		MediaType mt = overrideVariant(variant);
		if(existing!=null){
			return this.representItem(existing.getItem(),mt);
		} else {
			this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Unable to find the specified visit.");
			return null;
		}
	}



}
