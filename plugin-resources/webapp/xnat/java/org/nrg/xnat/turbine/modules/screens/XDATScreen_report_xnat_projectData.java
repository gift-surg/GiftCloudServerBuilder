// Copyright 2010 Washington University School of Medicine All Rights Reserved
/*
 * GENERATED FILE
 * Created on Mon Apr 30 09:31:43 CDT 2007
 *
 */
package org.nrg.xnat.turbine.modules.screens;
import java.sql.SQLException;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.exceptions.IllegalAccessException;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.screens.SecureReport;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.db.ItemAccessHistory;
import org.nrg.xft.db.PoolDBUtils;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xnat.turbine.utils.ProjectAccessRequest;

/**
 * @author XDAT
 *
 */
public class XDATScreen_report_xnat_projectData extends SecureReport {
	public static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(XDATScreen_report_xnat_projectData.class);
	/* (non-Javadoc)
	 * @see org.nrg.xdat.turbine.modules.screens.SecureReport#finalProcessing(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
	 */
	public void finalProcessing(RunData data, Context context) {
        XnatProjectdata project = (XnatProjectdata)om;
        
        
        try {
//        	org.nrg.xft.search.CriteriaCollection cc = new CriteriaCollection("AND");
//            cc.addClause("wrk:workflowData.ID",project.getId());
//            org.nrg.xft.collections.ItemCollection items = org.nrg.xft.search.ItemSearch.GetItems(cc,TurbineUtils.getUser(data),false);
//            //Sort by Launch Time
//            ArrayList workitems = items.getItems("wrk:workflowData.launch_time","DESC");
//            Iterator iter = workitems.iterator();
//            ArrayList workflows = new ArrayList();
//            while (iter.hasNext())
//            {
//                WrkWorkflowdata vrc = new WrkWorkflowdata((XFTItem)iter.next());
//                workflows.add(vrc);
//            }
//            
//            
//            context.put("workflows",workflows);
    
        	XDATUser user=TurbineUtils.getUser(data);
        	
        	if(user.canRead("xnat:subjectData/project",project.getId()))
        		ItemAccessHistory.LogAccess(user, item, "report");
        	

           if(ProjectAccessRequest.CREATED_PAR_TABLE){
               Integer parcount=(Integer)PoolDBUtils.ReturnStatisticQuery("SELECT COUNT(par_id)::int4 AS count FROM xs_par_table WHERE proj_id='"+ project.getId() + "'", "count", user.getDBName(), user.getLogin());
               context.put("par_count", parcount);
           }
            
           if(data.getParameters().get("topTab")!=null){
        	   context.put("topTab", data.getParameters().get("topTab"));
           }
           
           if(data.getParameters().get("bottomTab")!=null){
        	   context.put("bottomTab", data.getParameters().get("bottomTab"));
           }
        } catch (XFTInitException e) {
            logger.error("",e);
        } catch (ElementNotFoundException e) {
            logger.error("",e);
        } catch (DBPoolException e) {
            logger.error("",e);
        } catch (SQLException e) {
            logger.error("",e);
        } catch (IllegalAccessException e) {
            logger.error("",e);
        } catch (Exception e) {
            logger.error("",e);
        }
	}
	
}
