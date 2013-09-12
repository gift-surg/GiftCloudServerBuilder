/*
 * org.nrg.xnat.turbine.modules.actions.ModifyProject
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.turbine.modules.actions;

import org.apache.axis.utils.StringUtils;
import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.model.XnatInvestigatordataI;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.ElementSecurity;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.ModifyItem;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.PopulateItem;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTItem;
import org.nrg.xft.db.PoolDBUtils;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.exception.InvalidPermissionException;
import org.nrg.xft.exception.InvalidValueException;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xnat.utils.WorkflowUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ModifyProject extends SecureAction {
    static Logger logger = Logger.getLogger(ModifyItem.class);
    /* (non-Javadoc)
     * @see org.apache.turbine.modules.actions.VelocitySecureAction#doPerform(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    @Override
    public void doPerform(RunData data, Context context) throws Exception {

            PopulateItem populater = PopulateItem.Populate(data,"xnat:projectData",true);
            XDATUser user = TurbineUtils.getUser(data);
            
            InvalidValueException error=null;
            if (populater.hasError())
            {
                error = populater.getError();
            }

            XFTItem item = populater.getItem();
            XnatProjectdata  project = new XnatProjectdata(item);
                       

            final PersistentWorkflowI wrk=PersistentWorkflowUtils.getOrCreateWorkflowData(null, user, project.SCHEMA_ELEMENT_NAME,project.getId(),project.getId(),newEventInstance(data,EventUtils.CATEGORY.PROJECT_ADMIN));
	    	EventMetaI c=wrk.buildEvent();

        try {
            if(StringUtils.isEmpty(project.getId())){
            	data.addMessage("Missing required field (Abbreviation).");
				TurbineUtils.SetEditItem(item,data);
                if (((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("edit_screen",data)) !=null)
                {
                    data.setScreenTemplate(((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("edit_screen",data)));
                }
                return;
            }

            List<XnatProjectdata> conflicts = new ArrayList<XnatProjectdata>();
            List<XnatProjectdata> toRemove = new ArrayList<XnatProjectdata>();
            conflicts.addAll(XnatProjectdata.getXnatProjectdatasByField("xnat:projectData/name",project.getName(),user,false));

            for (XnatProjectdata potentialConflict : conflicts) {
                if (potentialConflict.getId().equals(project.getId())) toRemove.add(potentialConflict);
            }
            conflicts.removeAll(toRemove);
            if(!conflicts.isEmpty()){
                data.addMessage("A project with the title '" + project.getName() + "' already exists.");
                TurbineUtils.SetEditItem(item,data);
                if (((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("edit_screen",data)) !=null)
                {
                    data.setScreenTemplate(((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("edit_screen",data)));
                }
                return;
            }

            conflicts.addAll(XnatProjectdata.getXnatProjectdatasByField("xnat:projectData/secondary_id",project.getSecondaryId(),user,false));
            for (XnatProjectdata potentialConflict : conflicts) {
                if (potentialConflict.getId().equals(project.getId())) toRemove.add(potentialConflict);
            }
            conflicts.removeAll(toRemove);
            if(!conflicts.isEmpty()){
                data.addMessage("A project with the running title '" + project.getSecondaryId() + "' already exists.");
                TurbineUtils.SetEditItem(item,data);
                if (((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("edit_screen",data)) !=null)
                {
                    data.setScreenTemplate(((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("edit_screen",data)));
                }
                return;
            }
            
            if (error!=null)
            {
                data.addMessage(error.getMessage());
                TurbineUtils.SetEditItem(item, data);
                data.setScreenTemplate("XDATScreen_edit_projectData.vm");
                return;
            }

            if(!user.canEdit(project)){
            	error(new InvalidPermissionException("User cannot modify project " + project.getId()), data);
            	return;
            }
            
            try {
				project.initNewProject(user,false,true,c);
			} catch (Exception e2) {
				TurbineUtils.SetEditItem(item,data);
                data.addMessage(e2.getMessage());
                if (((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("edit_screen",data)) !=null)
                {
                    data.setScreenTemplate(((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("edit_screen",data)));
                }
                return;
			}
            
            this.removeExcessInvestigators(project, user);
            SaveItemHelper.authorizedSave(item,user, false, false,c);
            
            XnatProjectdata postSave = new XnatProjectdata(item);
            postSave.getItem().setUser(user);

            postSave.initGroups();
            
            user.initGroups();
            user.clearLocalCache();
            //postSave.initBundles(user);
            
            String accessibility=((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("accessibility",data));
            if (accessibility==null){
                accessibility="protected";
            }
            
            project.initAccessibility(accessibility, true,user,c);
            
           // p.initBundles((XDATUser)user);
            
            if (TurbineUtils.HasPassedParameter("destination", data)){
                this.redirectToReportScreen((String)TurbineUtils.GetPassedParameter("destination", data,"AddStep2.vm"), postSave, data);
            }else{
                this.redirectToReportScreen("XDATScreen_report_xnat_projectData.vm",(ItemI) postSave, data);
            }
                       
            WorkflowUtils.complete(wrk, c);
            user.clearLocalCache();
            ElementSecurity.refresh();
        } catch (Exception e) {
            logger.error("",e);
            WorkflowUtils.fail(wrk, c);
        }
    }

    /**
     * Inelegant solution to the need to be able to remove investigators from a project.
     * @param project
     * @param user
     * @throws Exception
     */
    private void removeExcessInvestigators(XnatProjectdata project, XDATUser user) throws Exception {
        // get a List of investigators on the project to be saved
        List<Integer> investigatorIds = new ArrayList<Integer>();
        for (XnatInvestigatordataI investigator : project.getInvestigators_investigator()) {
            if (investigator.getXnatInvestigatordataId() != null)
                investigatorIds.add(investigator.getXnatInvestigatordataId());
        }

        // if there are investigators, we don't want to delete them, so create a statement to exclude them from the delete
        String supplementaryClause = "";
        if (!investigatorIds.isEmpty()) {
            StringBuilder sb = null;
            sb = new StringBuilder();
            for (Integer investigatorId : investigatorIds) {
                sb.append(investigatorId);
                sb.append(",");
            }
            sb.deleteCharAt(sb.length()-1);  // remove final, unnecessary comma
            supplementaryClause = " AND xnat_investigatordata_xnat_investigatordata_id NOT IN (" + sb.toString() + ")";
        }
        String query = "DELETE FROM xnat_projectdata_investigator WHERE xnat_projectdata_id = '" + project.getId() + "'" +
                supplementaryClause + ";";
        PoolDBUtils.ExecuteNonSelectQuery(query,user.getDBName(), user.getLogin());

    }
}
