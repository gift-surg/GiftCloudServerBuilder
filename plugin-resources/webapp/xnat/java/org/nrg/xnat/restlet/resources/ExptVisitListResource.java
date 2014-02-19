/*
 * org.nrg.xnat.restlet.resources.ExptVisitListResource
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.restlet.resources;

import org.apache.log4j.Logger;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatPvisitdata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.schema.SchemaElement;
import org.nrg.xdat.security.SecurityValues;
import org.nrg.xft.XFTTable;
import org.nrg.xft.db.ViewManager;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.search.QueryOrganizer;
import org.nrg.xnat.helpers.xmlpath.XMLPathShortcuts;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;

public class ExptVisitListResource  extends QueryOrganizerResource  {
	static Logger logger = Logger.getLogger(ExptVisitListResource.class);

	XnatProjectdata proj=null;
	XnatSubjectdata subject=null;
	XnatPvisitdata visit=null;

	public String getDefaultElementName(){
		return "xnat:experimentData";
	}

	@Override
	public ArrayList<String> getDefaultFields(GenericWrapperElement e) {
		ArrayList<String> al=new ArrayList<String>();

		al.add("ID");
		al.add("project");
		al.add("date");
		al.add("label");
		al.add("subject_ID");
		al.add("xnat:experimentdata/visit");

		return al;
	}
	
	
	public ExptVisitListResource(Context context, Request request, Response response) {
		super(context, request, response);

		String pID= (String)request.getAttributes().get("PROJECT_ID");
		if(pID!=null){
			proj = XnatProjectdata.getProjectByIDorAlias(pID, user, false);
		}

		if(proj==null){
			response.setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Unable to identify project " + pID);
			return;
		}

		String subID= (String)request.getAttributes().get("SUBJECT_ID");
		if(subID!=null){
			subject = XnatSubjectdata.GetSubjectByProjectIdentifier(proj
					.getId(), subID, user, false);

			if(subject==null){
				subject = XnatSubjectdata.getXnatSubjectdatasById(subID, user, false);
				if (subject != null && (proj != null && !subject.hasProject(proj.getId()))) {
					subject = null;
				}
			}
		}
		if(subID != null && subject == null){
			response.setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Unable to identify project/subject combination " + pID + "/" + subID);
			return;
		}

		String visitID= (String)request.getAttributes().get("VISIT_ID");
		if(visitID==null){
			response.setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Unable to identify visit " + visitID);
			return;
		}
		
		visit = (XnatPvisitdata) XnatPvisitdata.GetVisitByProjectIdentifier(proj.getId(), visitID, user, false);
		if (visit == null) {
			visit = (XnatPvisitdata) XnatPvisitdata.getXnatPvisitdatasById(visitID, user, false);
			if (visit != null && !visit.hasProject(proj.getId())) {
				visit = null;
			}
		}
		if(visit == null ){
			response.setStatus(Status.CLIENT_ERROR_NOT_FOUND,"Unable to identify visit " + visitID);
			return;
		}
		
		this.getVariants().add(new Variant(MediaType.TEXT_HTML));
		this.getVariants().add(new Variant(MediaType.TEXT_XML));
		
		this.fieldMapping.putAll(XMLPathShortcuts.getInstance().getShortcuts(XMLPathShortcuts.VISIT_DATA,false));
	}
	@Override

	public Representation getRepresentation(Variant variant) {
		Representation rep=super.getRepresentation(variant);
		if(rep!=null)return rep;

		XFTTable table = null;


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


		try {
			final String rootElementName=this.getRootElementName();
			final QueryOrganizer qo = new QueryOrganizer(rootElementName,user,ViewManager.ALL);

			this.populateQuery(qo);

			CriteriaCollection where=new CriteriaCollection("AND");

			CriteriaCollection cc= new CriteriaCollection("OR");
			cc.addClause(rootElementName+"/project", proj.getId());
			cc.addClause(rootElementName+"/sharing/share/project", proj.getId());
			where.addClause(cc);

			if(subject!=null){
				CriteriaCollection cc2= new CriteriaCollection("AND");
				cc2.addClause("xnat:subjectAssessorData/subject_id", subject.getId());
				where.addClause(cc2);
			}
			
			CriteriaCollection cc3= new CriteriaCollection("OR");
			cc3.addClause(rootElementName+"/visit", visit.getId());
			cc3.addClause(rootElementName+"/sharing/share/visit", visit.getId());
			where.addClause(cc3);

			

			qo.setWhere(where);

			String query=qo.buildQuery();

			table=XFTTable.Execute(query, user.getDBName(), userName);

			if(table.size()>0){
				table=formatHeaders(table,qo,rootElementName+"/ID","/data/experiments/");

				final Integer labelI=table.getColumnIndex("label");
				final Integer idI=table.getColumnIndex(rootElementName+"/ID");
				if(labelI!=null && idI!=null){
					final XFTTable t= XFTTable.Execute("SELECT sharing_share_xnat_experimentda_id as id,label FROM xnat_experimentData_share WHERE project='"+ proj.getId() + "' and visit='" + visit.getId() +"'", user.getDBName(), user.getUsername());
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
