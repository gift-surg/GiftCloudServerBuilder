//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on May 21, 2007
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

import javax.mail.internet.InternetAddress;

import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.WrkWorkflowdata;
import org.nrg.xdat.om.XdatUser;
import org.nrg.xdat.om.XdatUserGroupid;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.UserGroup;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.db.DBAction;
import org.nrg.xft.email.EmailUtils;
import org.nrg.xft.email.EmailerI;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.nrg.xnat.turbine.utils.ProjectAccessRequest;

public class ProcessAccessRequest extends SecureAction {
    static Logger logger = Logger.getLogger(ProcessAccessRequest.class);

    public void doDenial(RunData data, Context context) throws Exception {
        Integer id = data.getParameters().getInteger("id");
        XdatUser other =(XdatUser) XdatUser.getXdatUsersByXdatUserId(id,TurbineUtils.getUser(data), false);

        String p = data.getParameters().getString("project");
        XDATUser user = TurbineUtils.getUser(data);
        XnatProjectdata project = (XnatProjectdata)XnatProjectdata.getXnatProjectdatasById(p, null, false);
        
        if (other!=null && project !=null){
            XDATUser otherU = new XDATUser(other);
            
            for (Map.Entry<String, UserGroup> entry:otherU.getGroups().entrySet()){
                if (entry.getValue().getTag().equals(project)){
                    for(XdatUserGroupid map:otherU.getGroups_groupid()){
                        if (map.getGroupid().equals(entry.getValue().getId())){   
                            DBAction.DeleteItem(map.getItem(), user);
                        }
                    }
                }
            }
            
            ProjectAccessRequest par = ProjectAccessRequest.RequestPARByUserProject(otherU.getXdatUserId(),project.getId(), user);
            par.setApproved(false);
            par.save(user);
            
            context.put("user",user);
            context.put("server",TurbineUtils.GetFullServerPath());
            context.put("system",TurbineUtils.GetSystemName());
            context.put("admin_email",AdminUtils.getAdminEmailId());
            context.put("projectOM",project);
            StringWriter sw = new StringWriter();
            Template template =Velocity.getTemplate("/screens/RequestProjectAccessDenialEmail.vm");
            template.merge(context,sw);
            String message= sw.toString();

            ArrayList<InternetAddress> to = new ArrayList();
            InternetAddress ia = new InternetAddress();
            ia.setAddress(otherU.getEmail());
            to.add(ia);

            ArrayList<InternetAddress> bcc = new ArrayList();
            if(ArcSpecManager.GetInstance().getEmailspecifications_projectAccess()){
                ia = new InternetAddress();
                ia.setAddress(AdminUtils.getAdminEmailId());
                bcc.add(ia);
            }
            
            ArrayList<InternetAddress> cc = new ArrayList();
            ia = new InternetAddress();
            ia.setAddress(user.getEmail());
            cc.add(ia);
            
            String from = AdminUtils.getAdminEmailId();
            String subject = TurbineUtils.GetSystemName() + " Access Request for " + project.getName() + " Denied";

            try {
                EmailerI sm = EmailUtils.getEmailer();
                sm.setFrom(from);
                sm.setTo(to);
                sm.setCc(cc);
                sm.setBcc(bcc);
                sm.setSubject(subject);
                sm.setMsg(message);
                
                sm.send();
            } catch (Exception e) {
                logger.error("Unable to send mail",e);
                System.out.println("Error sending Email");
                throw e;
            }
        }

        //data.setScreenTemplate("XDATScreen_manage_xnat_projectData.vm");
        //data.setScreenTemplate("/xnat_projectData/xnat_projectData_summary_management.vm");        
        TurbineUtils.SetSearchProperties(data, project);
       // data.getSession().setAttribute("tab","Access");
        data.getParameters().setString("params", "/topTab/Access");
        this.redirectToReportScreen("XDATScreen_report_xnat_projectData.vm", project, data);
    }
    
    public void doApprove(RunData data, Context context) throws Exception {
        Integer id = data.getParameters().getInteger("id");
        XdatUser other =(XdatUser) XdatUser.getXdatUsersByXdatUserId(id,TurbineUtils.getUser(data), false);

        String p = data.getParameters().getString("project");
        String access_level = data.getParameters().getString("access_level");
        if (access_level==null)access_level="member";
        XDATUser user = TurbineUtils.getUser(data);
        XnatProjectdata project = (XnatProjectdata)XnatProjectdata.getXnatProjectdatasById(p, null, false);
        
        if (other!=null && project !=null){
            XDATUser otherU = new XDATUser(other);
                        
            boolean deletedOldPermission = false;
            
            for (Map.Entry<String, UserGroup> entry:otherU.getGroups().entrySet()){
                if (entry.getValue().getTag().equals(project)){
                    for(XdatUserGroupid map:otherU.getGroups_groupid()){
                        if (map.getGroupid().equals(entry.getValue().getId())){   
                            DBAction.DeleteItem(map.getItem(), user);
                            deletedOldPermission=true;
                        }
                    }
                }
            }
            
            project.addGroupMember(project.getId() + "_" + access_level.toLowerCase(), otherU, user);
            
            ProjectAccessRequest par = ProjectAccessRequest.RequestPARByUserProject(otherU.getXdatUserId(),project.getId(), user);
            par.setApproved(true);
            par.save(user);
            

	        
	        try {
				WrkWorkflowdata workflow = new WrkWorkflowdata((UserI)user);
				workflow.setDataType("xnat:projectData");
				workflow.setExternalid(project.getId());
				workflow.setId(project.getId());
				workflow.setPipelineName("New " + par.getLevel() + ": " + otherU.getFirstname() + " " + otherU.getLastname());
				workflow.setStatus("Complete");
				workflow.setLaunchTime(Calendar.getInstance().getTime());
				workflow.save(user, false, false);
			} catch (Throwable e) {
				logger.error("",e);
			}
            
            context.put("user",user);
            context.put("server",TurbineUtils.GetFullServerPath());
            context.put("process","Transfer to the archive.");
            context.put("system",TurbineUtils.GetSystemName());
            context.put("access_level",access_level);
            context.put("admin_email",AdminUtils.getAdminEmailId());
            context.put("projectOM",project);
            SendAccessApprovalEmail(context,otherU.getEmail(),user,TurbineUtils.GetSystemName() + " Access Request for " + project.getName() + " Approved");
        }      
        //data.setScreenTemplate("XDATScreen_manage_xnat_projectData.vm");
        //data.setScreenTemplate("/xnat_projectData/xnat_projectData_summary_management.vm");
        TurbineUtils.SetSearchProperties(data, project);
        //data.getSession().setAttribute("tab","Access");
        data.getParameters().setString("params", "/topTab/Access");
        this.redirectToReportScreen("XDATScreen_report_xnat_projectData.vm", project, data);
    }
    
    public static void SendAccessApprovalEmail(Context context,String otherUemail,XDATUser user,String subject) throws Exception{
    	
        StringWriter sw = new StringWriter();
        Template template =Velocity.getTemplate("/screens/RequestProjectAccessApprovalEmail.vm");
        template.merge(context,sw);
        String message= sw.toString();

        ArrayList<InternetAddress> to = new ArrayList();
        InternetAddress ia = new InternetAddress();
        ia.setAddress(otherUemail);
        to.add(ia);

        ArrayList<InternetAddress> bcc = new ArrayList();
        if(ArcSpecManager.GetInstance().getEmailspecifications_projectAccess()){
	        ia = new InternetAddress();
	        ia.setAddress(AdminUtils.getAdminEmailId());
	        bcc.add(ia);
        }
        
        ArrayList<InternetAddress> cc = new ArrayList();
        ia = new InternetAddress();
        ia.setAddress(user.getEmail());
        cc.add(ia);
        
        String from = AdminUtils.getAdminEmailId();

        try {
            EmailerI sm = EmailUtils.getEmailer();
            sm.setFrom(from);
            sm.setTo(to);
            sm.setCc(cc);
            sm.setBcc(bcc);
            sm.setSubject(subject);
            sm.setMsg(message);
            
            sm.send();
        } catch (Exception e) {
            logger.error("Unable to send mail",e);
            System.out.println("Error sending Email");
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
