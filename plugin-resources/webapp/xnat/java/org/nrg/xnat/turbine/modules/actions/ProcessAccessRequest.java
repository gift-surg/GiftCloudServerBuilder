//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on May 21, 2007
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import java.io.StringWriter;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.jcs.access.exception.InvalidArgumentException;
import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XdatUser;
import org.nrg.xdat.om.XdatUserGroupid;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.base.BaseXnatProjectdata;
import org.nrg.xdat.security.UserGroup;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.exception.InvalidPermissionException;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xnat.turbine.utils.ProjectAccessRequest;
import org.nrg.xnat.utils.WorkflowUtils;

public class ProcessAccessRequest extends SecureAction {
    static Logger logger = Logger.getLogger(ProcessAccessRequest.class);

    public void doDenial(RunData data, Context context) throws Exception {
        Integer id = ((Integer)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedInteger("id",data));
        XdatUser other =(XdatUser) XdatUser.getXdatUsersByXdatUserId(id,TurbineUtils.getUser(data), false);

        String p = ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("project",data));
        
        if(p==null || p.contains("'")){
        	error(new InvalidArgumentException(p),data);
        }
        
        
        XDATUser user = TurbineUtils.getUser(data);
        XnatProjectdata project = (XnatProjectdata)XnatProjectdata.getXnatProjectdatasById(p, null, false);
        
        final PersistentWorkflowI wrk=PersistentWorkflowUtils.getOrCreateWorkflowData(null, user, project.SCHEMA_ELEMENT_NAME,project.getId(),project.getId(),newEventInstance(data, EventUtils.CATEGORY.PROJECT_ACCESS, EventUtils.REJECT_PROJECT_REQUEST));
    	EventMetaI c=wrk.buildEvent();
    	WorkflowUtils.save(wrk, c);
        
		if (other!=null && project !=null){
        	if(!user.canDelete(project)){
        		error(new InvalidPermissionException("Invalid permissions"),data);
        		return;
        	}
        	
		    XDATUser otherU = new XDATUser(other);
	        try {
			    
			    for (Map.Entry<String, UserGroup> entry:otherU.getGroups().entrySet()){
			        if (entry.getValue().getTag().equals(project.getId())){
			            for(XdatUserGroupid map:otherU.getGroups_groupid()){
			                if (map.getGroupid().equals(entry.getValue().getId())){  
                        	SaveItemHelper.authorizedDelete(map.getItem(), user,c);
			                }
			            }
			        }
			    }
			    
			    ProjectAccessRequest par = ProjectAccessRequest.RequestPARByUserProject(otherU.getXdatUserId(),project.getId(), user);
			    par.setApproved(false);
			    par.save(user);
				
		        WorkflowUtils.complete(wrk, c);
			} catch (Exception e) {
				WorkflowUtils.fail(wrk, c);
			}
			    
		    context.put("user",user);
		    context.put("server",TurbineUtils.GetFullServerPath());
		    context.put("system",TurbineUtils.GetSystemName());
		    context.put("admin_email",AdminUtils.getAdminEmailId());
		    context.put("projectOM",project);
		    StringWriter sw = new StringWriter();
		    Template template =Velocity.getTemplate("/screens/RequestProjectAccessDenialEmail.vm");
		    template.merge(context,sw);
		    String message= sw.toString();

		    String from = AdminUtils.getAdminEmailId();
		    String subject = TurbineUtils.GetSystemName() + " Access Request for " + project.getName() + " Denied";

		    try {
            	XDAT.getMailService().sendHtmlMessage(from, otherU.getEmail(), user.getEmail(), AdminUtils.getAdminEmailId(), subject, message);
		    } catch (Exception e) {
		        logger.error("Unable to send mail",e);
		        throw e;
		    }
		}

        //data.setScreenTemplate("XDATScreen_manage_xnat_projectData.vm");
        //data.setScreenTemplate("/xnat_projectData/xnat_projectData_summary_management.vm");        
        TurbineUtils.SetSearchProperties(data, project);
        data.getParameters().setString("topTab", "Access");
        this.redirectToReportScreen("XDATScreen_report_xnat_projectData.vm", project, data);
    }
    
    public void doApprove(RunData data, Context context) throws Exception {
        Integer id = ((Integer)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedInteger("id",data));
        XDATUser user = TurbineUtils.getUser(data);
        XdatUser other =(XdatUser) XdatUser.getXdatUsersByXdatUserId(id,TurbineUtils.getUser(data), false);

        String p = ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("project",data));
        String access_level = ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("access_level",data));
        if (StringUtils.isEmpty(access_level)){
        	access_level="member";
        }else{
        	if(!(access_level.equalsIgnoreCase(BaseXnatProjectdata.MEMBER_GROUP) 
        			|| access_level.equalsIgnoreCase(BaseXnatProjectdata.OWNER_GROUP)
        			|| access_level.equalsIgnoreCase(BaseXnatProjectdata.COLLABORATOR_GROUP))){
        		error(new Exception("Unknown Access level:"+access_level), data);
        		return;
        	}
        }
        
        if(p==null || p.contains("'")){
        	error(new InvalidArgumentException(p),data);
        	return;
        }
        
        XnatProjectdata project = (XnatProjectdata)XnatProjectdata.getXnatProjectdatasById(p, null, false);
                


        
        final PersistentWorkflowI wrk=PersistentWorkflowUtils.getOrCreateWorkflowData(null, user, project.SCHEMA_ELEMENT_NAME,project.getId(),project.getId(),newEventInstance(data, EventUtils.CATEGORY.PROJECT_ACCESS, EventUtils.APPROVE_PROJECT_REQUEST));
    	EventMetaI c=wrk.buildEvent();
    	WorkflowUtils.save(wrk, c);
        
        if (other!=null && project !=null){        
        	if(!user.canDelete(project)){
        		error(new InvalidPermissionException("Invalid permissions"),data);
        		return;
        	}

            XDATUser otherU;
			try {
				otherU = new XDATUser(other);
				
				for (Map.Entry<String, UserGroup> entry:otherU.getGroups().entrySet()){
				    if (entry.getValue().getTag().equals(project.getId())){
				        for(XdatUserGroupid map:otherU.getGroups_groupid()){
				            if (map.getGroupid().equals(entry.getValue().getId())){   
                            SaveItemHelper.authorizedDelete(map.getItem(), user,c);
				            }
				        }
				    }
				}
				
				project.addGroupMember(project.getId() + "_" + access_level.toLowerCase(), otherU, user,c,true);
				
				ProjectAccessRequest par = ProjectAccessRequest.RequestPARByUserProject(otherU.getXdatUserId(),project.getId(), user);
				par.setApproved(true);
				par.save(user);
				WorkflowUtils.complete(wrk, c);
			} catch (Exception e) {
				WorkflowUtils.fail(wrk, c);
				throw e;
			}
                        
            context.put("user",user);
            context.put("server",TurbineUtils.GetFullServerPath());
            context.put("process","Transfer to the archive.");
            context.put("system",TurbineUtils.GetSystemName());
            context.put("access_level",access_level);
            context.put("admin_email",AdminUtils.getAdminEmailId());
            context.put("projectOM",project);
            String[] projectOwnerEmails = project.getOwnerEmails().toArray(new String[] {});
            SendAccessApprovalEmail(context,AdminUtils.getAdminEmailId(),new String[]{otherU.getEmail()},projectOwnerEmails,new String[]{AdminUtils.getAdminEmailId()},TurbineUtils.GetSystemName() + " Access Request for " + project.getName() + " Approved");
        }      
        //data.setScreenTemplate("XDATScreen_manage_xnat_projectData.vm");
        //data.setScreenTemplate("/xnat_projectData/xnat_projectData_summary_management.vm");
        TurbineUtils.SetSearchProperties(data, project);
        data.getParameters().setString("topTab", "Access");
        this.redirectToReportScreen("XDATScreen_report_xnat_projectData.vm", project, data);
    }
    
    public static void SendAccessApprovalEmail(Context context,String otherUemail,XDATUser user,String subject) throws Exception{
	String admin = AdminUtils.getAdminEmailId();
   	SendAccessApprovalEmail(context, admin, new String[]{otherUemail}, new String[]{user.getEmail()}, new String[]{admin}, subject);
    }

    public static void SendAccessApprovalEmail(Context context, String from, String[] to, String[] cc, String[] bcc, String subject) throws Exception{
        StringWriter sw = new StringWriter();
        Template template =Velocity.getTemplate("/screens/RequestProjectAccessApprovalEmail.vm");
        template.merge(context,sw);
        String message= sw.toString();

        try {
        	XDAT.getMailService().sendHtmlMessage(from, to, cc, bcc, subject, message);
        } catch (Exception e) {
            logger.error("Unable to send mail",e);
            throw e;
        }
    }

    /* (non-Javadoc)
     * @see org.apache.turbine.modules.actions.VelocitySecureAction#doPerform(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    @Override
    public void doPerform(RunData data, Context context) throws Exception {

    }

    
}
