//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Jul 2, 2007
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import java.util.ArrayList;
import java.util.List;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.XdatUser;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.base.BaseXnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xnat.utils.WorkflowUtils;

public class ManageProjectAccess extends SecureAction {
	public static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ManageProjectAccess.class);
	   
    @Override
    public void doPerform(RunData data, Context context) throws Exception {
        //String accessibility = data.getParameters().getString("accessibility");
        String p = data.getParameters().getString("project");
        XnatProjectdata project =(XnatProjectdata) XnatProjectdata.getXnatProjectdatasById(p, null, false);
        
        PersistentWorkflowI wrk = WorkflowUtils.buildOpenWorkflow(TurbineUtils.getUser(data), "xnat:projectData", project.getId(), project.getId(), newEventInstance(data,EventUtils.CATEGORY.PROJECT_ACCESS));
        EventMetaI c=wrk.buildEvent();        

        String accessibility = data.getParameters().getString("accessibility");
        project.initAccessibility(accessibility, false,c);
        
        boolean sendmail=false;
        if (null!=data.getParameters().getString("sendmail") && data.getParameters().getString("sendmail").equals("email"))
        	sendmail=true;
        
        String collaborators = data.getParameters().getString("collaborators");
        String members = data.getParameters().getString("members");
        String owners = data.getParameters().getString("owners");

        List<String> ownersL= StringUtils.CommaDelimitedStringToArrayList(owners);
        List<String> membersL= StringUtils.CommaDelimitedStringToArrayList(members);
        List<String> collaboratorsL= StringUtils.CommaDelimitedStringToArrayList(collaborators);

        
        if (owners!=null){
            ArrayList<String> currentOwners = project.getGroupMembers(BaseXnatProjectdata.OWNER_GROUP);
            
            for (String newOwner: ownersL){
                if (!currentOwners.contains(newOwner))
                {
                    
                    //ADD MEMBER
                    ArrayList<XdatUser> al = XdatUser.getXdatUsersByField("xdat:user.email", newOwner, null, true);
                    for (XdatUser newU : al){
                        XDATUser newUOM = new XDATUser(newU);
                        final PersistentWorkflowI wrk2=PersistentWorkflowUtils.getOrCreateWorkflowData(null, TurbineUtils.getUser(data), project.SCHEMA_ELEMENT_NAME,project.getId(),project.getId(),newEventInstance(data,EventUtils.CATEGORY.PROJECT_ACCESS, EventUtils.ADD_USER_TO_PROJECT + "(" + newUOM.getLogin() + ")"));
        				
                        project.addGroupMember(project.getId() + "_" + BaseXnatProjectdata.OWNER_GROUP, newUOM, TurbineUtils.getUser(data),c);
    		            PersistentWorkflowUtils.save(wrk2,c);
    		        }
                    if (sendmail){
                    	context.put("user",TurbineUtils.getUser(data));
                        context.put("server",TurbineUtils.GetFullServerPath());
                        context.put("process","Transfer to the archive.");
                        context.put("system",TurbineUtils.GetSystemName());
                        context.put("access_level","owner");
                        context.put("admin_email",AdminUtils.getAdminEmailId());
                        context.put("projectOM",project);
                    	org.nrg.xnat.turbine.modules.actions.ProcessAccessRequest.SendAccessApprovalEmail(context, newOwner, TurbineUtils.getUser(data), TurbineUtils.GetSystemName() + " Access Granted for " + project.getName());
                    }
                }
            }
            
            for (String newOwner: currentOwners){
                if (!ownersL.contains(newOwner))
                {
                    //REMOVE MEMBER//ADD MEMBER
                    ArrayList<XdatUser> al = XdatUser.getXdatUsersByField("xdat:user.email", newOwner, null, true);
                    for (XdatUser newU : al){
                        XDATUser newUOM = new XDATUser(newU);
                        project.removeGroupMember(project.getId() + "_" + BaseXnatProjectdata.OWNER_GROUP, newUOM, TurbineUtils.getUser(data),newEventInstance(data,EventUtils.CATEGORY.PROJECT_ACCESS,EventUtils.REMOVE_USER_TO_PROJECT + " ("+ newU.getLogin() + ")"));
                    }
                }
            }
        }else{
        }
        
        if (members!=null){
            ArrayList<String> currentMembers = project.getGroupMembers(BaseXnatProjectdata.MEMBER_GROUP);
            
            for (String newMember: membersL){
                if (!currentMembers.contains(newMember))
                {
                    //ADD MEMBER
                    ArrayList<XdatUser> al = XdatUser.getXdatUsersByField("xdat:user.email", newMember, null, true);
                    for (XdatUser newU : al){
                        XDATUser newUOM = new XDATUser(newU);
						final PersistentWorkflowI wrk2=PersistentWorkflowUtils.getOrCreateWorkflowData(null, TurbineUtils.getUser(data), project.SCHEMA_ELEMENT_NAME,project.getId(),project.getId(),newEventInstance(data,EventUtils.CATEGORY.PROJECT_ACCESS, EventUtils.ADD_USER_TO_PROJECT + "(" + newUOM.getLogin() + ")"));
                        project.addGroupMember(project.getId() + "_" + BaseXnatProjectdata.MEMBER_GROUP, newUOM, TurbineUtils.getUser(data),c);
    		            PersistentWorkflowUtils.complete(wrk2,c);
                    }
                    if (sendmail){
                    	context.put("user",TurbineUtils.getUser(data));
                        context.put("server",TurbineUtils.GetFullServerPath());
                        context.put("process","Transfer to the archive.");
                        context.put("system",TurbineUtils.GetSystemName());
                        context.put("access_level","member");
                        context.put("admin_email",AdminUtils.getAdminEmailId());
                        context.put("projectOM",project);
                    	org.nrg.xnat.turbine.modules.actions.ProcessAccessRequest.SendAccessApprovalEmail(context, newMember, TurbineUtils.getUser(data), TurbineUtils.GetSystemName() + " Access Granted for " + project.getName());
                    }
                }
            }
            
            for (String newMember: currentMembers){
                if (!membersL.contains(newMember))
                {
                    //REMOVE MEMBER//ADD MEMBER
                    ArrayList<XdatUser> al = XdatUser.getXdatUsersByField("xdat:user.email", newMember, null, true);
                    for (XdatUser newU : al){
                        XDATUser newUOM = new XDATUser(newU);
                        project.removeGroupMember(project.getId() + "_" + BaseXnatProjectdata.MEMBER_GROUP, newUOM, TurbineUtils.getUser(data),newEventInstance(data,EventUtils.CATEGORY.PROJECT_ACCESS,EventUtils.REMOVE_USER_TO_PROJECT + " ("+ newU.getLogin() + ")"));
                    }
                }
            }
        }else{
        }
        
        if (collaborators!=null){
            ArrayList<String> currentMembers = project.getGroupMembers(BaseXnatProjectdata.COLLABORATOR_GROUP);
            
            for (String newMember: collaboratorsL){
                if (!currentMembers.contains(newMember))
                {
                    //ADD MEMBER
                    ArrayList<XdatUser> al = XdatUser.getXdatUsersByField("xdat:user.email", newMember, null, true);
                    for (XdatUser newU : al){
                        XDATUser newUOM = new XDATUser(newU);
                        final PersistentWorkflowI wrk2=PersistentWorkflowUtils.getOrCreateWorkflowData(null, TurbineUtils.getUser(data), project.SCHEMA_ELEMENT_NAME,project.getId(),project.getId(),newEventInstance(data,EventUtils.CATEGORY.PROJECT_ACCESS, EventUtils.ADD_USER_TO_PROJECT + "(" + newUOM.getLogin() + ")"));
                        project.addGroupMember(project.getId() + "_" + BaseXnatProjectdata.COLLABORATOR_GROUP, newUOM, TurbineUtils.getUser(data),c);
                        PersistentWorkflowUtils.complete(wrk2, c);
                    }
                    if (sendmail){
                    	context.put("user",TurbineUtils.getUser(data));
                        context.put("server",TurbineUtils.GetFullServerPath());
                        context.put("process","Transfer to the archive.");
                        context.put("system",TurbineUtils.GetSystemName());
                        context.put("access_level","collaborator");
                        context.put("admin_email",AdminUtils.getAdminEmailId());
                        context.put("projectOM",project);
                    	org.nrg.xnat.turbine.modules.actions.ProcessAccessRequest.SendAccessApprovalEmail(context, newMember, TurbineUtils.getUser(data), TurbineUtils.GetSystemName() + " Access Granted for " + project.getName());
                    }
                }
            }
            
            for (String newMember: currentMembers){
                if (!collaboratorsL.contains(newMember))
                {
                    //REMOVE MEMBER
                    ArrayList<XdatUser> al = XdatUser.getXdatUsersByField("xdat:user.email", newMember, null, true);
                    for (XdatUser newU : al){
                        XDATUser newUOM = new XDATUser(newU);
                        project.removeGroupMember(project.getId() + "_" + BaseXnatProjectdata.COLLABORATOR_GROUP, newUOM, TurbineUtils.getUser(data),newEventInstance(data,EventUtils.CATEGORY.PROJECT_ACCESS,EventUtils.REMOVE_USER_TO_PROJECT + " ("+ newU.getLogin() + ")"));
                    }
                }
            }
        }else{
        }

        PersistentWorkflowUtils.complete(wrk,wrk.buildEvent());
        
        //UserGroupManager.Refresh();
        TurbineUtils.getUser(data).init();
        
        this.redirectToReportScreen("XDATScreen_report_xnat_projectData.vm", project, data);
    }
    
   
}
