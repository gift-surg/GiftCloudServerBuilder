/*
 * org.nrg.xnat.restlet.resources.ExperimentListResource
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/11/13 12:37 PM
 */
package org.nrg.xnat.restlet.resources;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.nrg.framework.utilities.Reflection;
import org.nrg.xdat.schema.SchemaElement;
import org.nrg.xdat.security.ElementSecurity;
import org.nrg.xdat.security.SecurityValues;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFTTable;
import org.nrg.xft.db.ViewManager;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xft.schema.design.SchemaElementI;
import org.nrg.xft.search.QueryOrganizer;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xft.utils.VelocityUtils;
import org.nrg.xnat.helpers.xmlpath.XMLPathShortcuts;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

import com.google.common.collect.Lists;

public class ExperimentListResource  extends QueryOrganizerResource {
    
    public ExperimentListResource(org.restlet.Context context, Request request, Response response) {
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

    @SuppressWarnings("ConstantConditions")
    @Override
    public Representation getRepresentation(Variant variant) {  
        Representation rep=super.getRepresentation(variant);
        if(rep!=null) {
            return rep;
        }
        
        XFTTable table;
        
        FilteredExptListHandlerI handler=null;
        try {
			for(FilteredExptListHandlerI filter:getHandlers()){
				if(filter.canHandle(this)){
					handler=filter;
				}
			}
		} catch (InstantiationException e1) {
			logger.error("",e1);
		} catch (IllegalAccessException e1) {
			logger.error("",e1);
		}
		
        Hashtable<String,Object> params=new Hashtable<String,Object>();

        try {
	        if(handler!=null){
	        	table=handler.build(this,params);
	        }else{
	        	//unable to identify experiment list filter... this shouldn't happen
	        	table =null;
	        }
	            
        } catch (SQLException e) {
            logger.error("Error occurred executing database query", e);
            this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            return null;
        } catch (DBPoolException e) {
            logger.error("Error occurred connecting to database", e);
            this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            return null;
        } catch (Exception e) {
            logger.error("Unknown error occurred",e);
            this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            return null;
        }
        
        if(table==null){
        	return null;
        }

        MediaType mt = overrideVariant(variant);
        if(table!=null) {
            params.put("totalRecords", table.size());
        }
        return this.representTable(table, mt, params);
    }
    
    private static List<FilteredExptListHandlerI> handlers=null;
    
    /**
     * Get a list of the possible experiment handlers.  This allows additional handlers to be injected at a later date or via a module.
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public static List<FilteredExptListHandlerI> getHandlers() throws InstantiationException, IllegalAccessException{
    	if(handlers==null){
	    	handlers=Lists.newArrayList();
	    	
	    	//ordering here is important.  the last match wins
	    	handlers.add(new DefaultExperimentHandler());
	    	handlers.add(new RecentExperiments());
	    	List<Class<?>> classes;
	        try {
	            classes = Reflection.getClassesForPackage("org.nrg.xnat.restlet.experimentsList.extensions");
	        } catch (Exception exception) {
	            throw new RuntimeException(exception);
	        }
	
	        for (Class<?> clazz : classes) {
	            if (clazz.isInstance(FilteredExptListHandlerI.class)) {
	            	handlers.add((FilteredExptListHandlerI)clazz.newInstance());
	            }
	        }
    	}
        
        return handlers;
    }
    
    //FilteredExptListHandlerI allows additional experiment list handlers to be added via modules
    public static interface FilteredExptListHandlerI{
    	public boolean canHandle(SecureResource resource);
    	public XFTTable build(ExperimentListResource resource,Hashtable<String,Object> params) throws Exception;
    }

    //handles requests where ?recent=something
    public static class RecentExperiments implements FilteredExptListHandlerI{

		@Override
		public boolean canHandle(SecureResource resource) {
			return (resource.getQueryVariable("recent")!=null);
		}

		@Override
		public XFTTable build(ExperimentListResource resource, Hashtable<String, Object> params) throws Exception {
			XFTTable table;
            params.put("title", "Recent Experiments");
            //this uses an ugly hack to try to enforces security via the SQL statement.  It generates the statement for the subject data type.  It then uses the same permissions on the experimentData type.  This assumes that experiments and subjects have the same permissions defined (which is currently always the case).  But, this could be an issue in the future.

            boolean limit=false;
            int days = 0;
            if(resource.getQueryVariable("recent")!=null){
                String daysS=resource.getQueryVariable("recent");
                try {
                    days= Integer.parseInt(daysS);
                } catch (NumberFormatException e) {
                    limit=true;
                    days=60;
                }
            }

            //experiments
            VelocityContext context= new VelocityContext();
            context.put("time", Calendar.getInstance().getTime());
   
            StringBuilder builder=new StringBuilder();
            builder.append("SELECT * FROM (SELECT DISTINCT ON (expt.id) expt.id,label,project,date,status, workflow_status, xme.element_name, COALESCE(es.code,es.singular,es.element_name) AS TYPE_DESC,insert_date,activation_date,last_modified,workflow_date,pipeline_name, COALESCE(workflow_date,last_modified,insert_date)  AS action_date FROM xnat_experimentData expt LEFT JOIN xdat_meta_element xme ON expt.extension=xme.xdat_meta_element_id LEFT JOIN xnat_experimentData_meta_data emd ON expt.experimentData_info=emd.meta_data_id LEFT JOIN xdat_element_security es ON xme.element_name=es.element_name LEFT JOIN (   SELECT DISTINCT ON (id) id,COALESCE(current_step_launch_time,launch_time) AS workflow_date,CASE pipeline_name WHEN 'Transferred'::text THEN 'Archived'::text ELSE CASE xs_lastposition('/'::text, pipeline_name::text) WHEN 0 THEN pipeline_name ELSE substring(substring(pipeline_name::text, xs_lastposition('/'::text, pipeline_name::text) + 1), 1, xs_lastposition('.'::text, substring(pipeline_name::text, xs_lastposition('/'::text, pipeline_name::text) + 1)) - 1) END END AS pipeline_name,status AS workflow_status FROM wrk_workflowdata WHERE category!='SIDE_ADMIN' AND category IS NOT NULL AND launch_time > (NOW() - interval '" + days +" day') AND status!='Failed (Dismissed)' AND pipeline_name NOT LIKE 'xnat_tools%AutoRun.xml' ORDER BY id,COALESCE(current_step_launch_time,launch_time) DESC ) wrkflw ON expt.id=wrkflw.id RIGHT JOIN (");
            if(resource.user.getGroup("ALL_DATA_ACCESS")!=null || resource.user.getGroup("ALL_DATA_ADMIN")!=null){
            	 builder.append("SELECT DISTINCT ID FROM xnat_imageSessionData");
            }else{
            	 builder.append(String.format("SELECT DISTINCT ID FROM (SELECT map.groups_groupid_xdat_user_xdat_user_id,xea.element_name, xfm.field, xfm.field_value FROM xdat_user u JOIN xdat_user_groupID map ON u.xdat_user_id=map.groups_groupid_xdat_user_xdat_user_id JOIN xdat_userGroup gp ON map.groupid=gp.id JOIN xdat_element_access xea ON gp.xdat_usergroup_id=xea.xdat_usergroup_xdat_usergroup_id JOIN xdat_field_mapping_set xfms ON xea.xdat_element_access_id=xfms.permissions_allow_set_xdat_elem_xdat_element_access_id JOIN xdat_field_mapping xfm ON xfms.xdat_field_mapping_set_id=xfm.xdat_field_mapping_set_xdat_field_mapping_set_id AND read_element=1 AND field_value!=''and field !='' WHERE u.login='guest' UNION SELECT map.groups_groupid_xdat_user_xdat_user_id,xea.element_name, xfm.field, xfm.field_value FROM xdat_user_groupID map JOIN xdat_userGroup gp ON map.groupid=gp.id JOIN xdat_element_access xea ON gp.xdat_usergroup_id=xea.xdat_usergroup_xdat_usergroup_id JOIN xdat_field_mapping_set xfms ON xea.xdat_element_access_id=xfms.permissions_allow_set_xdat_elem_xdat_element_access_id JOIN xdat_field_mapping xfm ON xfms.xdat_field_mapping_set_id=xfm.xdat_field_mapping_set_xdat_field_mapping_set_id AND read_element=1 AND field_value!=''and field !='' WHERE map.groups_groupid_xdat_user_xdat_user_id=%s) perms INNER JOIN (SELECT isd.id, element_name || '/project' as field, expt.project FROM xnat_imageSessionData isd LEFT JOIN xnat_experimentData expt ON isd.id=expt.id LEFT JOIN xdat_meta_element xme ON expt.extension=xme.xdat_meta_element_id UNION SELECT expt.id,xme.element_name || '/sharing/share/project', shr.project  FROM xnat_experimentData_share shr LEFT JOIN xnat_experimentData expt ON expt.id=shr.sharing_share_xnat_experimentda_id LEFT JOIN xdat_meta_element xme ON expt.extension=xme.xdat_meta_element_id) expts ON perms.field=expts.field AND perms.field_value=expts.project",resource.user.getXdatUserId()));
            }
            builder.append(") perm ON expt.id=perm.id ");

            builder.append(" RIGHT JOIN xnat_imageSessionData isd ON perm.id=isd.id  ");
            builder.append(" WHERE (insert_date > (NOW() - interval '" + days +" day') OR activation_date > (NOW() - interval '" + days +" day') OR last_modified > (NOW() - interval '" + days +" day') OR workflow_date > (NOW() - interval '" + days +" day')) ");
            builder.append(" )SEARCH ORDER BY action_date DESC");
            if(limit) {
            	 builder.append(" LIMIT 60");
            }

            table=XFTTable.Execute(builder.toString(), resource.user.getDBName(), resource.userName);

            return table;
		}
    	
    }
    //handles everything else
    public static class DefaultExperimentHandler implements FilteredExptListHandlerI{

		@Override
		public boolean canHandle(SecureResource resource) {
			return true;
		}

		@Override
		public XFTTable build(ExperimentListResource resource,Hashtable<String, Object> params) throws Exception {
			XFTTable table=null;
			params.put("title", "Matching experiments");
            String rootElementName=resource.getRootElementName();
            QueryOrganizer qo = new QueryOrganizer(rootElementName,resource.user,ViewManager.ALL);

            resource.populateQuery(qo);

            if(!ElementSecurity.IsSecureElement(rootElementName)){
                qo.addField("xnat:experimentData/extension_item/element_name");
                qo.addField("xnat:experimentData/project");
            }

            String query=qo.buildQuery();

            table=XFTTable.Execute(query, resource.user.getDBName(), resource.userName);

            if(!ElementSecurity.IsSecureElement(rootElementName)){
                List<Object[]> remove=new ArrayList<Object[]>();
                Hashtable<String, Boolean> checked = new Hashtable<String,Boolean>();

                String enS=qo.getFieldAlias("xnat:experimentData/extension_item/element_name");
                if(enS==null) {
                    logger.warn("Couldn't find property xnat:experimentData/extension_item/element_name for search");
                    resource.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
                    return null;
                }

                Integer en=table.getColumnIndex(enS.toLowerCase());
                Integer p=table.getColumnIndex(qo.getFieldAlias("xnat:experimentData/project").toLowerCase());

                for(Object[] row : table.rows()) {
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

                                if (resource.user.canReadByXMLPath(secureElement,values)) {
                                    checked.put(element_name+project, Boolean.TRUE);
                                }else{
                                    checked.put(element_name+project, Boolean.FALSE);
                                }
                            }

                            if(!checked.get(element_name + project)){
                                remove.add(row);
                            }
                        }
                    } catch (Throwable e) {
                        logger.debug("Problem occurred iterating secure elements", e);
                        remove.add(row);
                    }
                }

                table.rows().removeAll(remove);
            }

            if(table.size()>0){
                table=resource.formatHeaders(table,qo,rootElementName+"/ID","/data/experiments/");
            }
        	return table;
        }
		
    }
}