// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.turbine.modules.actions;


import org.apache.turbine.modules.ScreenLoader;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.ArcProject;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.base.BaseXnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.modules.screens.EditScreenA;
import org.nrg.xdat.turbine.utils.PopulateItem;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTItem;
import org.nrg.xft.event.Event;
import org.nrg.xft.event.EventManager;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.event.persist.PersistentWorkflowUtils.JustificationAbsent;
import org.nrg.xft.utils.ValidationUtils.ValidationResults;
import org.nrg.xnat.utils.WorkflowUtils;

public class AddProject extends SecureAction {
	static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(AddProject.class);
	
	@Override
	public void doPerform(RunData data, Context context) throws Exception {
		XDATUser user = TurbineUtils.getUser(data);
        XFTItem found = null;

        if (TurbineUtils.HasPassedParameter("tag", data)){
            context.put("tag", TurbineUtils.GetPassedParameter("tag", data));
        }
        try {
            EditScreenA screen = (EditScreenA) ScreenLoader.getInstance().getInstance("XDATScreen_add_xnat_projectData");
            
            XFTItem newItem = (XFTItem)screen.getEmptyItem(data);
            
            PopulateItem populater = PopulateItem.Populate(data,"xnat:projectData",true,newItem);
                        
            found = populater.getItem();
            XnatProjectdata  project = new XnatProjectdata(found);

            final PersistentWorkflowI wrk=PersistentWorkflowUtils.getOrCreateWorkflowData(null, user, XnatProjectdata.SCHEMA_ELEMENT_NAME,project.getId(),project.getId(),newEventInstance(data,EventUtils.CATEGORY.PROJECT_ADMIN,EventUtils.getAddModifyAction("xnat:projectData", true)));
	    	EventMetaI c=wrk.buildEvent();
	    	
            try {
				project.initNewProject(user,false,false,c);
			} catch (Exception e2) {
				TurbineUtils.SetEditItem(found,data);
                data.addMessage(e2.getMessage());
                if (data.getParameters().getString("edit_screen") !=null)
                {
                    data.setScreenTemplate(data.getParameters().getString("edit_screen"));
                }
                return;
			}
			
            ValidationResults vr = null;
            
            ValidationResults temp = project.getItem().validate();
            if (! project.getItem().isValid())
            {
               vr = temp;
            }
            
            if (vr != null)
            {
                TurbineUtils.SetEditItem(project.getItem(),data);
                context.put("vr",vr);
                if (data.getParameters().getString("edit_screen") !=null)
                {
                    data.setScreenTemplate(data.getParameters().getString("edit_screen"));
                }
            }else{
            	try {
            		project.save(TurbineUtils.getUser(data),false,false,c);
            		ItemI temp1 =project.getItem().getCurrentDBVersion(false);
            		if (temp1 != null)
            		{
                        found = (XFTItem)temp1;
            		}
            	} catch (Exception e) {
            		logger.error("Error Storing " + found.getXSIType(),e);
            		
            		data.setMessage("Error Saving item.");
                    TurbineUtils.SetEditItem(found,data);
                    if (data.getParameters().getString("edit_screen") !=null)
                    {
                        data.setScreenTemplate(data.getParameters().getString("edit_screen"));
                    }
                    return;
            	}
                
                XnatProjectdata postSave;
				try {
					postSave = new XnatProjectdata(found);
					postSave.getItem().setUser(user);

					postSave.initGroups(c);
					
					//postSave.initBundles(user);
					
					String accessibility=data.getParameters().getString("accessibility");
					if (accessibility==null){
					    accessibility="protected";
					}
					
					if (!accessibility.equals("private"))
					    project.initAccessibility(accessibility, true,c);
					
					user.refreshGroup(postSave.getId() + "_" + BaseXnatProjectdata.OWNER_GROUP);
					populater = PopulateItem.Populate(data,"arc:project",true);

					XFTItem item = populater.getItem();
					ArcProject arcP = new ArcProject(item);
					postSave.initArcProject(arcP, user,c);

					WorkflowUtils.complete(wrk, c);
					
					user.clearLocalCache();
					EventManager.Trigger(XnatProjectdata.SCHEMA_ELEMENT_NAME, postSave.getId(), Event.UPDATE);
				} catch (Exception e) {
					WorkflowUtils.fail(wrk, c);
					throw e;
				}
                
            	data = TurbineUtils.setDataItem(data,found);
            	data = TurbineUtils.SetSearchProperties(data,found);

                
                if (TurbineUtils.HasPassedParameter("destination", data)){
                    this.redirectToReportScreen((String)TurbineUtils.GetPassedParameter("destination", data,"AddStep2.vm"), postSave, data);
                }else{
                    this.redirectToReportScreen("XDATScreen_report_xnat_projectData.vm",(ItemI) postSave, data);
                }
                
            }
        } catch (Exception e) {
            handleException(data, found, e, TurbineUtils.EDIT_ITEM);
        }
	}
}
