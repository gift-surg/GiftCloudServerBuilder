// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources;

import java.util.ArrayList;
import java.util.Hashtable;

import org.nrg.xdat.model.XnatProjectparticipantI;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatProjectparticipant;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTable;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.db.ViewManager;
import org.nrg.xft.exception.InvalidValueException;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.search.QueryOrganizer;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xft.utils.ValidationUtils.ValidationResults;
import org.nrg.xnat.helpers.xmlpath.XMLPathShortcuts;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.xml.sax.SAXParseException;

public class ProjectSubjectList extends QueryOrganizerResource {
	XnatProjectdata proj=null;
	
	public ProjectSubjectList(Context context, Request request, Response response) {
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

				if(proj!=null){
					this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
					this.getVariants().add(new Variant(MediaType.TEXT_HTML));
					this.getVariants().add(new Variant(MediaType.TEXT_XML));					
				}else{
				response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
			}
		}else{
			response.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
		}
	
		this.fieldMapping.putAll(XMLPathShortcuts.getInstance().getShortcuts(XMLPathShortcuts.SUBJECT_DATA));
	}

	@Override
	public boolean allowPost() {
		return true;
	}

	@Override
	public void handlePost() {
	        XFTItem item = null;			

			try {
			item=this.loadItem("xnat:subjectData",true);
			
				if(item==null){
					item=XFTItem.NewItem("xnat:subjectData", user);
				}
				
				if(item.instanceOf("xnat:subjectData")){
					XnatSubjectdata sub = new XnatSubjectdata(item);

					if(sub.getExperiments_experiment().size()>0){
						this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY,"Submitted subject record must not include subject assessors.");
						return;
					}
					
					if(this.proj==null && sub.getProject()!=null){
						proj = XnatProjectdata.getXnatProjectdatasById(sub.getProject(), user, false);
					}
					
					if(this.proj!=null){
						if(sub.getProject()==null || sub.getProject().equals("")){
							sub.setProject(this.proj.getId());
						}else if(sub.getProject().equals(this.proj.getId())){
						}else{
							boolean matched=false;
							for(XnatProjectparticipantI pp : sub.getSharing_share()){
								if(pp.getProject().equals(this.proj.getId())){
									matched=true;
									break;
								}
							}
							
							if(!matched){
								XnatProjectparticipantI pp= new XnatProjectparticipant((UserI)user);
								((XnatProjectparticipant)pp).setProject(this.proj.getId());
								sub.setSharing_share((XnatProjectparticipant)pp);
							}
						}
					}else{
						this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY,"Submitted subject record must include the project attribute.");
						return;
					}

					XnatSubjectdata existing=null;
					if(sub.getId()!=null){
						existing=XnatSubjectdata.getXnatSubjectdatasById(sub.getId(), user, completeDocument);
					}
					
					if(existing==null && sub.getProject()!=null && sub.getLabel()!=null){
					existing=XnatSubjectdata.GetSubjectByProjectIdentifier(sub.getProject(), sub.getLabel(),user, completeDocument);
					}
					
					if(existing==null){
						for(XnatProjectparticipantI pp : sub.getSharing_share()){
						existing=XnatSubjectdata.GetSubjectByProjectIdentifier(pp.getProject(), pp.getLabel(),user, completeDocument);
							if(existing!=null){
								break;
							}
						}
					}
					
					if(existing==null){
						if(!user.canCreate(sub)){
						this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Specified user account has insufficient create priviledges for subjects in this project.");
						return;
						}
						//IS NEW
						if(sub.getId()==null || sub.getId().equals("")){
						sub.setId(XnatSubjectdata.CreateNewID());
						}
					}else{
					this.getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT,"Subject already exists.");
					return;
						}
				

				if(!StringUtils.IsEmpty(sub.getLabel()) && !StringUtils.IsAlphaNumericUnderscore(sub.getId())){
					this.getResponse().setStatus(Status.CLIENT_ERROR_EXPECTATION_FAILED,"Invalid character in subject label.");
					return;
								}
				

				
				final ValidationResults vr = sub.validate();
	            
	            if (vr != null && !vr.isValid())
	            {
	            	this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,vr.toFullString());
					return;
							}
				
				if(sub.save(user,false,false)){
					MaterializedView.DeleteByUser(user);
						}

				if(this.getQueryVariable("activate")!=null && this.getQueryVariable("activate").equals("true")){
					if(user.canActivate(sub.getItem()))sub.activate(user);
					else this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Specified user account has insufficient activation priviledges for experiments in this project.");
					}
					
				if(this.getQueryVariable("quarantine")!=null && this.getQueryVariable("quarantine").equals("true")){
					if(user.canActivate(sub.getItem()))sub.quarantine(user);
					else this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Specified user account has insufficient activation priviledges for experiments in this project.");
				}
					
				this.returnSuccessfulCreateFromList(sub.getId());
				}else{
					this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY,"Only xnat:Subject documents can be PUT to this address.");
				}
		} catch (SAXParseException e) {
			this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY,e.getMessage());
		} catch (InvalidValueException e) {
			this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
			} catch (Exception e) {
				this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			logger.error("",e);
			}
		}

	
	
	@Override
	public ArrayList<String> getDefaultFields(GenericWrapperElement e) {
		ArrayList<String> al=new ArrayList<String>();
		
		al.add("ID");
		al.add("project");
		al.add("label");
		al.add("insert_date");
		al.add("insert_user");
		
		return al;
	}

	public String getDefaultElementName(){
		return "xnat:subjectData";
	}

	@Override
	public Representation getRepresentation(Variant variant) {	
		XFTTable table = null;
		if(proj!=null){
			final Representation rep=super.getRepresentation(variant);
			if(rep!=null)return rep;
			
			try {
				final QueryOrganizer qo = new QueryOrganizer(this.getRootElementName(), user,
						ViewManager.ALL);
	            
				this.populateQuery(qo);

				final CriteriaCollection cc= new CriteriaCollection("OR");
				cc.addClause("xnat:subjectData/project", proj.getId());
				cc.addClause("xnat:subjectData/sharing/share/project", proj.getId());
				qo.setWhere(cc);

				final String query = qo.buildQuery();

				table = XFTTable.Execute(query, user.getDBName(), userName);

				table = formatHeaders(table, qo, "xnat:subjectData/ID",
						"/REST/subjects/");
				
				final Integer labelI=table.getColumnIndex("label");
				final Integer idI=table.getColumnIndex("ID");
				if(labelI!=null && idI!=null){
					final XFTTable t= XFTTable.Execute("SELECT subject_id,label FROM xnat_projectParticipant WHERE project='"+ proj.getId() + "'", user.getDBName(), user.getUsername());
					final Hashtable lbls=t.toHashtable("subject_id", "label");
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
			} catch (Exception e) {
				e.printStackTrace();
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
				return null;
			}

			final MediaType mt = overrideVariant(variant);
			final Hashtable<String, Object> params = new Hashtable<String, Object>();
			if (table != null)
				params.put("totalRecords", table.size());
			return this.representTable(table, mt, params);
		}
		
		final Hashtable<String,Object> params=new Hashtable<String,Object>();
		params.put("title", "Project Subjects");

		final MediaType mt = overrideVariant(variant);

		if(table!=null)params.put("totalRecords", table.size());
		return this.representTable(table, mt, params);
	}
}
