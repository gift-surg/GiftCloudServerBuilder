/*
 * org.nrg.xnat.restlet.resources.ProjectListResource
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 1/22/14 10:12 AM
 */
package org.nrg.xnat.restlet.resources;

import org.nrg.action.ActionException;
import org.nrg.xdat.om.XdatStoredSearch;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.base.BaseXnatProjectdata;
import org.nrg.xdat.search.DisplayCriteria;
import org.nrg.xdat.search.DisplaySearch;
import org.nrg.xdat.security.SecurityManager;
import org.nrg.xft.XFT;
import org.nrg.xft.XFTItem;
import org.nrg.xft.XFTTable;
import org.nrg.xft.db.ViewManager;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.search.QueryOrganizer;
import org.nrg.xft.utils.DateUtils;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.nrg.xnat.helpers.xmlpath.XMLPathShortcuts;
import org.nrg.xnat.restlet.representations.ItemXMLRepresentation;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

public class ProjectListResource extends QueryOrganizerResource {
	private static final String ACCESSIBLE = "accessible";
	XFTTable table = null;

	public ProjectListResource(Context context, Request request, Response response) {
		super(context, request, response);
		
			this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
			this.getVariants().add(new Variant(MediaType.TEXT_HTML));
			this.getVariants().add(new Variant(MediaType.TEXT_XML));

			this.fieldMapping.putAll(XMLPathShortcuts.getInstance().getShortcuts(XMLPathShortcuts.PROJECT_DATA,true));
	
	}
	
	

	@Override
	public boolean allowPost() {
		return true;
	}



	@Override
	public void handlePost() {
		XFTItem item;
		try {
			item=this.loadItem("xnat:projectData",true);

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

			boolean allowDataDeletion =false;
			if(this.getQueryVariable("allowDataDeletion")!=null && this.getQueryVariable("allowDataDeletion").equalsIgnoreCase("true")){
				allowDataDeletion =true;
			}

			if(item.instanceOf("xnat:projectData")){
				XnatProjectdata project = new XnatProjectdata(item);

				if(StringUtils.IsEmpty(project.getId())){
					this.getResponse().setStatus(Status.CLIENT_ERROR_EXPECTATION_FAILED,"Requires XNAT ProjectData ID");
					return;
				}

				if(!StringUtils.IsAlphaNumericUnderscore(project.getId())){
					this.getResponse().setStatus(Status.CLIENT_ERROR_EXPECTATION_FAILED,"Invalid character in project ID.");
					return;
				}

				if(item.getCurrentDBVersion()==null){
					if(XFT.getBooleanProperty("UI.allow-non-admin-project-creation", true) || user.isSiteAdmin()){
						this.returnSuccessfulCreateFromList(BaseXnatProjectdata.createProject(project, user, allowDataDeletion,false,newEventInstance(EventUtils.CATEGORY.PROJECT_ADMIN),getQueryVariable("accessibility")));
					}else{
						this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"User account doesn't have permission to edit this project.");
                    }
				}else{
					this.getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT,"Project already exists.");
				}
			}
		} catch (ActionException e) {
			this.getResponse().setStatus(e.getStatus(),e.getMessage());
			return;
		} catch (Exception e) {
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			e.printStackTrace();
		}
	}


	@Override
	public ArrayList<String> getDefaultFields(GenericWrapperElement e) {
		ArrayList<String> al=new ArrayList<String>();
		al.add("ID");
		al.add("secondary_ID");
		al.add("name");
		al.add("description");
		al.add("pi_firstname");
		al.add("pi_lastname");
		
		return al;
	}

	public String getDefaultElementName(){
		return "xnat:projectData";
	}


	@Override
	public boolean allowGet() {
		return true;
	}

	@Override
	public Representation getRepresentation(Variant variant) {
		DisplaySearch ds;
		Representation rep1 = super.getRepresentation(variant);
		if (rep1 != null)
			return rep1;

		if (containsQueryVariable(ACCESSIBLE)
				|| containsQueryVariable("prearc_code")
				|| containsQueryVariable("owner")
				|| containsQueryVariable("member")
				|| containsQueryVariable("collaborator")
				|| containsQueryVariable("activeSince")
				|| containsQueryVariable("recent")
				|| containsQueryVariable("favorite")
				|| containsQueryVariable("admin")
				|| (this.requested_format != null && requested_format.equals("search_xml"))) {

			try {
				ds = new DisplaySearch();
				ds.setUser(user);
				ds.setRootElement("xnat:projectData");
				ds.addDisplayField("xnat:projectData", "ID");
				ds.addDisplayField("xnat:projectData", "NAME");
				ds.addDisplayField("xnat:projectData", "DESCRIPTION");
				ds.addDisplayField("xnat:projectData", "SECONDARY_ID");
				ds.addDisplayField("xnat:projectData", "PI");
				ds.addDisplayField("xnat:projectData", "PROJECT_INVS");
				ds.addDisplayField("xnat:projectData", "PROJECT_ACCESS");
				ds.addDisplayField("xnat:projectData", "PROJECT_ACCESS_IMG");
				ds.addDisplayField("xnat:projectData", "INSERT_DATE");
				ds.addDisplayField("xnat:projectData", "INSERT_USER");
				ds.addDisplayField("xnat:projectData", "USER_ROLE", "Role", user.getXdatUserId());
				ds.addDisplayField("xnat:projectData", "LAST_ACCESSED", "Last Accessed", user.getXdatUserId());

				if (this.isQueryVariableTrue("prearc_code")) {
					ds.addDisplayField("xnat:projectData", "PROJ_QUARANTINE");
					ds.addDisplayField("xnat:projectData", "PROJ_PREARCHIVE_CODE");
				}

				CriteriaCollection allCC = new CriteriaCollection("AND");
				CriteriaCollection orCC = new CriteriaCollection("OR");

				String access = this.getQueryVariable(ACCESSIBLE);
				if (access != null) {
					if (access.equalsIgnoreCase("true")) {
						if (user.getGroup("ALL_DATA_ACCESS") == null && user.getGroup("ALL_DATA_ADMIN") == null) {
							CriteriaCollection cc = new CriteriaCollection("OR");
							DisplayCriteria dc = new DisplayCriteria();
							dc.setSearchFieldByDisplayField("xnat:projectData", "PROJECT_USERS");
							dc.setComparisonType(" LIKE ");
							dc.setValue("% " + user.getLogin() + " %", false);
							cc.add(dc);

							dc = new DisplayCriteria();
							dc.setSearchFieldByDisplayField("xnat:projectData", "PROJECT_ACCESS");
							dc.setValue("public", false);
							cc.add(dc);

							allCC.addCriteria(cc);
						}
					} else {
						CriteriaCollection cc = new CriteriaCollection("OR");
						DisplayCriteria dc = new DisplayCriteria();
						dc.setSearchFieldByDisplayField("xnat:projectData", "PROJECT_USERS");
						dc.setComparisonType(" NOT LIKE ");
						dc.setValue("% " + user.getLogin() + " %", false);
						cc.add(dc);

						dc = new DisplayCriteria();
						dc.setSearchFieldByDisplayField("xnat:projectData", "PROJECT_USERS");
						dc.setComparisonType(" IS ");
						dc.setValue(" NULL ", false);
						dc.setOverrideDataFormatting(true);
						cc.add(dc);

						allCC.addCriteria(cc);
					}
				}

				String owner = this.getQueryVariable("owner");
				if (owner != null) {
					if (owner.equalsIgnoreCase("true")) {
						CriteriaCollection cc = new CriteriaCollection("OR");
						DisplayCriteria dc = new DisplayCriteria();
						dc.setSearchFieldByDisplayField("xnat:projectData", "PROJECT_OWNERS");
						dc.setComparisonType(" LIKE ");
						dc.setValue("% " + user.getLogin() + " %", false);
						cc.add(dc);

						orCC.addCriteria(cc);
					} else {
						CriteriaCollection cc = new CriteriaCollection("OR");
						DisplayCriteria dc = new DisplayCriteria();
						dc.setSearchFieldByDisplayField("xnat:projectData", "PROJECT_USERS");
						dc.setComparisonType(" NOT LIKE ");
						dc.setValue("% " + user.getLogin() + " %", false);
						cc.add(dc);

						orCC.addCriteria(cc);
					}
				}
				if (getQueryVariable("admin") != null) {
					if (isQueryVariableTrue("admin")) {
						if (user.checkRole(PrearcUtils.ROLE_SITE_ADMIN)) {
							CriteriaCollection cc = new CriteriaCollection("OR");
							DisplayCriteria dc = new DisplayCriteria();
							dc.setSearchFieldByDisplayField("xnat:projectData", "ID");
							dc.setComparisonType(" IS NOT ");
							dc.setValue(" NULL ", false);
							dc.setOverrideDataFormatting(true);
							cc.add(dc);
							orCC.addCriteria(cc);
						}
					}
				}

				String member = this.getQueryVariable("member");
				if (member != null) {
					if (member.equalsIgnoreCase("true")) {
						CriteriaCollection cc = new CriteriaCollection("OR");
						DisplayCriteria dc = new DisplayCriteria();
						dc.setSearchFieldByDisplayField("xnat:projectData", "PROJECT_MEMBERS");
						dc.setComparisonType(" LIKE ");
						dc.setValue("% " + user.getLogin() + " %", false);
						cc.add(dc);

						orCC.addCriteria(cc);
					} else {
						CriteriaCollection cc = new CriteriaCollection("OR");
						DisplayCriteria dc = new DisplayCriteria();
						dc.setSearchFieldByDisplayField("xnat:projectData", "PROJECT_MEMBERS");
						dc.setComparisonType(" NOT LIKE ");
						dc.setValue("% " + user.getLogin() + " %", false);
						cc.add(dc);

						orCC.addCriteria(cc);
					}
				}

				String collaborator = this.getQueryVariable("collaborator");
				if (collaborator != null) {
					if (collaborator.equalsIgnoreCase("true")) {
						CriteriaCollection cc = new CriteriaCollection("OR");
						DisplayCriteria dc = new DisplayCriteria();
						dc.setSearchFieldByDisplayField("xnat:projectData", "PROJECT_COLLABS");
						dc.setComparisonType(" LIKE ");
						dc.setValue("% " + user.getLogin() + " %", false);
						cc.add(dc);

						orCC.addCriteria(cc);
					} else {
						CriteriaCollection cc = new CriteriaCollection("OR");
						DisplayCriteria dc = new DisplayCriteria();
						dc.setSearchFieldByDisplayField("xnat:projectData", "PROJECT_COLLABS");
						dc.setComparisonType(" NOT LIKE ");
						dc.setValue("% " + user.getLogin() + " %", false);
						cc.add(dc);

						orCC.addCriteria(cc);
					}
				}

				String activeSince = this.getQueryVariable("activeSince");
				if (activeSince != null) {
					try {
						Date d = DateUtils.parseDateTime(activeSince);

						DisplayCriteria dc = new DisplayCriteria();
						dc.setSearchFieldByDisplayField("xnat:projectData", "PROJECT_LAST_WORKFLOW");
						dc.setComparisonType(">");
						dc.setValue(d, false);
						orCC.add(dc);
					} catch (RuntimeException e) {
						this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY);
					}
				}

				String recent = this.getQueryVariable("recent");
				if (recent != null) {
					try {
						DisplayCriteria dc = new DisplayCriteria();
						dc.setSearchFieldByDisplayField("xnat:projectData", "PROJECT_LAST_ACCESS");
						dc.setComparisonType(" LIKE ");
						dc.setValue("% " + user.getLogin() + " %", false);
						orCC.addCriteria(dc);
					} catch (RuntimeException e) {
						this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY);
					}
				}

				String favorite = this.getQueryVariable("favorite");
				if (favorite != null) {
					try {
						DisplayCriteria dc = new DisplayCriteria();
						dc.setSearchFieldByDisplayField("xnat:projectData", "PROJECT_FAV");
						dc.setComparisonType(" LIKE ");
						dc.setValue("% " + user.getLogin() + " %", false);
						orCC.addCriteria(dc);
					} catch (RuntimeException e) {
						this.getResponse().setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY);
					}
				}

				if (orCC.size() > 0)
					allCC.addCriteria(orCC);

				if (allCC.size() > 0)
					ds.addCriteria(allCC);

				ds.setSortBy("SECONDARY_ID");

				if (this.requested_format == null || !requested_format.equals("search_xml")) {
					table = (XFTTable) ds.execute(user.getLogin());
				}
			} catch (IllegalAccessException e) {
				logger.error("", e);
				getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
				return null;
			} catch (Exception e) {
				logger.error("", e);
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
				return null;
			}

			Hashtable<String, Object> params = new Hashtable<String, Object>();
			params.put("title", "Projects");
			params.put("xdat_user_id", user.getXdatUserId());

			MediaType mt = overrideVariant(variant);

			if (this.requested_format != null && this.requested_format.equals("search_xml")) {

				XdatStoredSearch xss = ds.convertToStoredSearch("");

				if (xss != null) {
					ItemXMLRepresentation rep = new ItemXMLRepresentation(xss.getItem(), MediaType.TEXT_XML);
					rep.setAllowDBAccess(false);
					return rep;
				} else {
					this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
					return new StringRepresentation("", MediaType.TEXT_XML);
				}
			} else {
				if (table != null)
					params.put("totalRecords", table.size());
				return this.representTable(table, mt, params);
			}
		} else {
			XFTTable table;
			try {
				final String re = this.getRootElementName();

				final QueryOrganizer qo = new QueryOrganizer(re, user,
						ViewManager.ALL);

				this.populateQuery(qo);

				if (this.containsQueryVariable("restrict") && user.getGroup("ALL_DATA_ADMIN") == null) {
					final String restriction = this.getQueryVariable("restrict");
					if (restriction.equals(SecurityManager.EDIT) || restriction.equals(SecurityManager.DELETE)) {
						final List<Object> ps = user.getAllowedValues("xnat:projectData", "xnat:projectData/ID", restriction);
						final CriteriaCollection cc = new CriteriaCollection("OR");
						for (Object p : ps) {
							cc.addClause("xnat:projectData/ID", p);
						}
						qo.setWhere(cc);
					}
				}

				final String query = qo.buildQuery();

				table = XFTTable.Execute(query, user.getDBName(), userName);

				table = formatHeaders(table, qo, re + "/ID", "/data/projects/");
			} catch (IllegalAccessException e) {
				logger.error("", e);
				getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
				return null;
			} catch (Exception e) {
				logger.error("", e);
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
				return null;
			}

			final MediaType mt = overrideVariant(variant);
			final Hashtable<String, Object> params = new Hashtable<String, Object>();
			if (table != null)
				params.put("totalRecords", table.size());
			return this.representTable(table, mt, params);
		}
	}

}
