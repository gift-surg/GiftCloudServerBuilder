// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;

import org.nrg.xdat.schema.SchemaElement;
import org.nrg.xdat.security.ElementSecurity;
import org.nrg.xdat.security.SecurityValues;
import org.nrg.xft.XFTTable;
import org.nrg.xft.db.ViewManager;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xft.schema.design.SchemaElementI;
import org.nrg.xft.search.QueryOrganizer;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xnat.helpers.xmlpath.XMLPathShortcuts;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

public class ExperimentListResource  extends QueryOrganizerResource {
	
	public ExperimentListResource(Context context, Request request, Response response) {
		super(context, request, response);
			this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
			this.getVariants().add(new Variant(MediaType.TEXT_HTML));
			this.getVariants().add(new Variant(MediaType.TEXT_XML));
		
			this.fieldMapping.putAll(XMLPathShortcuts.getInstance().getShortcuts(XMLPathShortcuts.EXPERIMENT_DATA,true));
		
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
		if(e.instanceOf("xnat:subjectAssessorData")){
			al.add("subject_ID");
			al.add("subject_label");
		}

		if(e.instanceOf("xnat:imageAssessorData")){
			al.add("session_ID");
			al.add("session_label");
		}
		
		return al;
	}

	public String getDefaultElementName(){
		return "xnat:experimentData";
	}

	@Override
	public Representation getRepresentation(Variant variant) {	
		Representation rep=super.getRepresentation(variant);
		if(rep!=null)return rep;
		
		XFTTable table = null;
		Hashtable<String,Object> params=new Hashtable<String,Object>();
			try {
			if(this.getQueryVariable("recent")!=null){
				params.put("title", "Recent Experiments");
				//this uses an ugly hack to try to enforces security via the SQL statement.  It generates the statement for the subject data type.  It then uses the same permissions on the experimentData type.  This assumes that experiments and subjects have the same permissions defined (which is currently always the case).  But, this could be an issue in the future.
				org.nrg.xft.search.QueryOrganizer qo = new org.nrg.xft.search.QueryOrganizer("xnat:subjectData",user,ViewManager.ALL);
				qo.addField("xnat:subjectData/ID");			
				
				try {
				String query= qo.buildQuery();
				
				String idField=qo.translateXMLPath("xnat:subjectData/ID");
	
					boolean limit=false;
				int days = 0;
				if(this.getQueryVariable("recent")!=null){
					String daysS=this.getQueryVariable("recent");
					try {
						days= Integer.parseInt(daysS);
					} catch (NumberFormatException e) {
							limit=true;
						days=60;
					}
				}
				
				//experiments
				query=StringUtils.ReplaceStr(query, idField, "id");
				query=StringUtils.ReplaceStr(query, "xnat_subjectData", "xnat_experimentData");
				query=StringUtils.ReplaceStr(query, "xnat_projectParticipant", "xnat_experimentData_share");
				query=StringUtils.ReplaceStr(query, "subject_id", "sharing_share_xnat_experimentda_id");
				
				query="SELECT * FROM (SELECT DISTINCT ON (expt.id) expt.id,label,project,date,status, xme.element_name, COALESCE(es.code,es.singular,es.element_name) AS TYPE_DESC,insert_date,activation_date,last_modified,workflow_date,pipeline_name, CASE WHEN (CASE WHEN last_modified>insert_date THEN last_modified ELSE insert_date END)>(CASE WHEN workflow_date>activation_date THEN workflow_date ELSE activation_date END) THEN (CASE WHEN last_modified>insert_date THEN last_modified ELSE insert_date END) ELSE (CASE WHEN workflow_date>activation_date THEN workflow_date ELSE activation_date END) END  AS action_date FROM xnat_experimentData expt LEFT JOIN xdat_meta_element xme ON expt.extension=xme.xdat_meta_element_id LEFT JOIN xdat_element_security es ON xme.element_name=es.element_name LEFT JOIN xnat_experimentData_meta_data emd ON expt.experimentData_info=emd.meta_data_id LEFT JOIN (   SELECT DISTINCT ON (id) id,current_step_launch_time AS workflow_date,pipeline_name FROM wrk_workflowdata WHERE status='Complete' ORDER BY id,current_step_launch_time ) wrkflw ON expt.id=wrkflw.id RIGHT JOIN (" +
						query +
					") perm ON expt.id=perm.id ";

						query +=" RIGHT JOIN xnat_imageSessionData isd ON perm.id=isd.id ";
					query +=" WHERE (insert_date > (NOW() - interval '" + days +" day') OR activation_date > (NOW() - interval '" + days +" day') OR last_modified > (NOW() - interval '" + days +" day') OR workflow_date > (NOW() - interval '" + days +" day')) ";
					
				
				query +=" )SEARCH ORDER BY action_date DESC";
					if(limit)query+=" LIMIT 60";

				table=XFTTable.Execute(query, user.getDBName(), userName);
				} catch (IllegalAccessException e) {
					logger.error("",e);
					e.printStackTrace();
					table=new XFTTable();
					String[] headers = {"id","label","project","date","status","element_name","type_desc","insert_date","activation_date","last_modified","workflow_date","pipeline_name","action_date"};
					table.initTable(headers);
				}
			}else if(this.getQueryVariable("needQC")!=null){
				params.put("title", "Recent Experiments");
				//this uses an ugly hack to try to enforces security via the SQL statement.  It generates the statement for the subject data type.  It then uses the same permissions on the experimentData type.  This assumes that experiments and subjects have the same permissions defined (which is currently always the case).  But, this could be an issue in the future.
				org.nrg.xft.search.QueryOrganizer qo = new org.nrg.xft.search.QueryOrganizer("xnat:subjectData",user,ViewManager.ALL);
				qo.addField("xnat:subjectData/ID");			
				
				try {
				String query= qo.buildQuery();
				
				String idField=qo.translateXMLPath("xnat:subjectData/ID");
	
					boolean limit=false;
				int days = 0;
				if(this.getQueryVariable("recent")!=null){
					String daysS=this.getQueryVariable("recent");
					try {
						days= Integer.parseInt(daysS);
					} catch (NumberFormatException e) {
							limit=true;
						days=60;
					}
				}
				
				//experiments
				query=StringUtils.ReplaceStr(query, idField, "id");
				query=StringUtils.ReplaceStr(query, "xnat_subjectData", "xnat_experimentData");
				query=StringUtils.ReplaceStr(query, "xnat_projectParticipant", "xnat_experimentData_share");
				query=StringUtils.ReplaceStr(query, "subject_id", "sharing_share_xnat_experimentda_id");
				
				query="SELECT * FROM (" +
						"SELECT DISTINCT ON (expt.id) expt.id, expt.label,expt.project,date,status, xme.element_name, COALESCE(es.code,es.singular,es.element_name) AS TYPE_DESC,insert_date,activation_date,last_modified,workflow_date,pipeline_name, CASE WHEN (CASE WHEN last_modified>insert_date THEN last_modified ELSE insert_date END)>(CASE WHEN workflow_date>activation_date THEN workflow_date ELSE activation_date END) THEN (CASE WHEN last_modified>insert_date THEN last_modified ELSE insert_date END) ELSE (CASE WHEN workflow_date>activation_date THEN workflow_date ELSE activation_date END) END  AS action_date, first_scan_id,u.login AS insert_user, sub.label AS subject_label, sub.id AS subject_id " +
						"FROM xnat_experimentData expt " +
						"LEFT JOIN xdat_meta_element xme ON expt.extension=xme.xdat_meta_element_id " +
						"LEFT JOIN xdat_element_security es ON xme.element_name=es.element_name " +
						"LEFT JOIN xnat_experimentData_meta_data emd ON expt.experimentData_info=emd.meta_data_id " +
						"LEFT JOIN xdat_user u ON emd.insert_user_xdat_user_id=u.xdat_user_id " +
						"LEFT JOIN (   " +
							"SELECT DISTINCT ON (id) id,current_step_launch_time AS workflow_date,pipeline_name " +
							"FROM wrk_workflowdata " +
							"WHERE status='Complete' " +
							"ORDER BY id,current_step_launch_time " +
						") wrkflw ON expt.id=wrkflw.id " +
						"RIGHT JOIN (" +
							query +
						") perm ON expt.id=perm.id " +
						"RIGHT JOIN xnat_imageSessionData isd ON perm.id=isd.id " +
						"LEFT JOIN xnat_subjectAssessorData sad ON perm.id=sad.id " +
						"LEFT JOIN xnat_subjectData sub ON sad.subject_id=sub.id " +
						"LEFT JOIN (" +
							"SELECT DISTINCT ON (isd.id) isd.ID AS SESSION_ID, scan.ID AS FIRST_SCAN_ID, MANUAL_QC.ID AS MANUAL_QC_ID FROM xnat_imagesessiondata isd LEFT JOIN xnat_imagescanData scan ON isd.id=scan.image_session_id LEFT JOIN ( 	SELECT DISTINCT ON (iad.imagesession_id) iad.id, iad.imagesession_id FROM xnat_imageAssessorData iad LEFT JOIN xnat_experimentdata expt ON iad.id=expt.id 	LEFT JOIN xdat_meta_element xme ON expt.extension=xme.xdat_meta_element_id WHERE xme.element_name='xnat:qcManualAssessorData') MANUAL_QC ON isd.id=MANUAL_QC.imagesession_id" +
						") FIRST_SCAN ON isd.id=FIRST_SCAN.SESSION_ID " +
						"WHERE (MANUAL_QC_ID IS NULL) " +
					")SEARCH WHERE id IS NOT NULL ORDER BY action_date DESC";
					
				
					if(limit)query+=" LIMIT 60";

				table=XFTTable.Execute(query, user.getDBName(), userName);
				} catch (IllegalAccessException e) {
					logger.error("",e);
					e.printStackTrace();
					table=new XFTTable();
					String[] headers = {"id","label","project","date","status","element_name","type_desc","insert_date","activation_date","last_modified","workflow_date","pipeline_name","action_date"};
					table.initTable(headers);
				}
			}else{
				params.put("title", "Matching experiments");
				String rootElementName=this.getRootElementName();
				QueryOrganizer qo = new QueryOrganizer(rootElementName,user,ViewManager.ALL);

				this.populateQuery(qo);

				if(!ElementSecurity.IsSecureElement(rootElementName)){
					qo.addField("xnat:experimentData/extension_item/element_name");
					qo.addField("xnat:experimentData/project");
				}
				
				String query=qo.buildQuery();
				
				try {
					table=XFTTable.Execute(query, user.getDBName(), userName);
					
					if(!ElementSecurity.IsSecureElement(rootElementName)){
						ArrayList remove=new ArrayList();
						Hashtable<String, Boolean> checked = new Hashtable<String,Boolean>();
						
						String enS=qo.getFieldAlias("xnat:experimentData/extension_item/element_name");
						if(enS==null){
							this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
							return null;
						}
						Integer en=table.getColumnIndex(enS.toLowerCase());
						Integer p=table.getColumnIndex(qo.getFieldAlias("xnat:experimentData/project").toLowerCase());
						
						for(int i=0;i<table.rows().size();i++){
							Object[] row = (Object[])table.rows().get(i);
							String element_name=(String)row[en];
							String project=(String)row[p];
							try{
								if(project==null || element_name==null){
									remove.add(row);
								}else{
									
									if(!checked.containsKey(element_name+project)){
										SchemaElementI secureElement = SchemaElement.GetElement(element_name);

										SecurityValues values = new SecurityValues();
										values.put(element_name + "/project",project);

										if (user.canReadByXMLPath(secureElement,values))
										{
											checked.put(element_name+project, Boolean.TRUE);
										}else{
											checked.put(element_name+project, Boolean.FALSE);
										}
									}
									
									if(!checked.get(element_name+project).booleanValue()){
										remove.add(row);
									}
								}
							}catch (Throwable e) {
								e.printStackTrace();
								remove.add(row);
							}
						}
						
						table.rows().removeAll(remove);
					}
				
					if(table.size()>0){
						table=formatHeaders(table,qo,rootElementName+"/ID","/data/experiments/");
					}
			} catch (SQLException e) {
					logger.error("",e);
				e.printStackTrace();
					this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
					return null;
			} catch (DBPoolException e) {
					logger.error("",e);
				e.printStackTrace();
					this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
					return null;
			} catch (Exception e) {
					logger.error("",e);
				e.printStackTrace();
					this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
					return null;
			}
				
			}
		} catch (SQLException e) {
			logger.error("",e);
			e.printStackTrace();
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			return null;
		} catch (DBPoolException e) {
			logger.error("",e);
			e.printStackTrace();
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			return null;
		} catch (Exception e) {
			logger.error("",e);
			e.printStackTrace();
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
			return null;
		}
		

		MediaType mt = overrideVariant(variant);
		if(table!=null)params.put("totalRecords", table.size());
		return this.representTable(table, mt, params);
	}
}
