// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources;

import java.util.ArrayList;
import java.util.Hashtable;

import org.nrg.xft.XFTTable;
import org.nrg.xft.db.ViewManager;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xft.search.QueryOrganizer;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

public class SubjectListResource extends QueryOrganizerResource {
	public SubjectListResource(Context context, Request request, Response response) {
		super(context, request, response);
		
		this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
		this.getVariants().add(new Variant(MediaType.TEXT_HTML));
		this.getVariants().add(new Variant(MediaType.TEXT_XML));
		
		this.fieldMapping.put("project", "xnat:subjectData/project");
		this.fieldMapping.put("label", "xnat:subjectData/label");
		this.fieldMapping.put("ID", "xnat:subjectData/ID");

		this.fieldMapping.put("group", "xnat:subjectData/group");
		this.fieldMapping.put("src", "xnat:subjectData/src");
		this.fieldMapping.put("pi_firstname", "xnat:subjectData/investigator/firstname");
		this.fieldMapping.put("pi_lastname", "xnat:subjectData/investigator/lastname");
		this.fieldMapping.put("dob", "xnat:subjectData/demographics[@xsi:type=xnat:demographicData]/dob");
		this.fieldMapping.put("yob", "xnat:subjectData/demographics[@xsi:type=xnat:demographicData]/yob");
		this.fieldMapping.put("age", "xnat:subjectData/demographics[@xsi:type=xnat:demographicData]/age");
		this.fieldMapping.put("gender", "xnat:subjectData/demographics[@xsi:type=xnat:demographicData]/gender");
		this.fieldMapping.put("handedness", "xnat:subjectData/demographics[@xsi:type=xnat:demographicData]/handedness");
		this.fieldMapping.put("ses", "xnat:subjectData/demographics[@xsi:type=xnat:demographicData]/ses");
		this.fieldMapping.put("education", "xnat:subjectData/demographics[@xsi:type=xnat:demographicData]/education");
		this.fieldMapping.put("educationDesc", "xnat:subjectData/demographics[@xsi:type=xnat:demographicData]/educationDesc");
		this.fieldMapping.put("race", "xnat:subjectData/demographics[@xsi:type=xnat:demographicData]/race");
		this.fieldMapping.put("ethnicity", "xnat:subjectData/demographics[@xsi:type=xnat:demographicData]/ethnicity");
		this.fieldMapping.put("weight", "xnat:subjectData/demographics[@xsi:type=xnat:demographicData]/weight");
		this.fieldMapping.put("height", "xnat:subjectData/demographics[@xsi:type=xnat:demographicData]/height");
		this.fieldMapping.put("gestational_age", "xnat:subjectData/demographics[@xsi:type=xnat:demographicData]/gestational_age");
		this.fieldMapping.put("post_menstrual_age", "xnat:subjectData/demographics[@xsi:type=xnat:demographicData]/post_menstrual_age");
		this.fieldMapping.put("birth_weight", "xnat:subjectData/demographics[@xsi:type=xnat:demographicData]/birth_weight");
		
		this.fieldMapping.put("insert_date", "xnat:subjectData/meta/insert_date");
		this.fieldMapping.put("insert_user", "xnat:subjectData/meta/insert_user/login");
		this.fieldMapping.put("last_modified", "xnat:subjectData/meta/last_modified");
		
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
		Representation rep=super.getRepresentation(variant);
		if(rep!=null)return rep;
			
		XFTTable table;
		try {
			QueryOrganizer qo = new QueryOrganizer(this.getRootElementName(), user,
					ViewManager.ALL);

			this.populateQuery(qo);

			String query = qo.buildQuery();

			table = XFTTable.Execute(query, user.getDBName(), userName);

			table = formatHeaders(table, qo, "xnat:subjectData/ID",
					"/REST/subjects/");
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
