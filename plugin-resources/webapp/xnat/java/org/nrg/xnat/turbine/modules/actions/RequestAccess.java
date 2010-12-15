//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on May 21, 2007
 *
 */
package org.nrg.xnat.turbine.modules.actions;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;

import javax.mail.internet.InternetAddress;

import org.apache.log4j.Logger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.email.EmailUtils;
import org.nrg.xft.email.EmailerI;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.nrg.xnat.turbine.utils.ProjectAccessRequest;

public class RequestAccess extends SecureAction {
    static Logger logger = Logger.getLogger(RequestAccess.class);

    @Override
    public void doPerform(RunData data, Context context) throws Exception {
        String p = data.getParameters().getString("project");
        XnatProjectdata project =(XnatProjectdata) XnatProjectdata.getXnatProjectdatasById(p, null, false);

        String access_level = data.getParameters().getString("access_level");
        String comments = data.getParameters().getString("comments");

        XDATUser user = TurbineUtils.getUser(data);

        ProjectAccessRequest.CreatePAR(project.getId(), access_level, user);
                
        context.put("user",user);
        context.put("server",TurbineUtils.GetFullServerPath());
        context.put("process","Transfer to the archive.");
        context.put("system",TurbineUtils.GetSystemName());
        context.put("admin_email",AdminUtils.getAdminEmailId());
        context.put("projectOM",project);
        context.put("access_level",access_level);
        context.put("comments",comments);
        StringWriter sw = new StringWriter();
        Template template =Velocity.getTemplate("/screens/RequestProjectAccessEmail.vm");
        template.merge(context,sw);
        String message= sw.toString();

        ArrayList<String> ownerEmails = project.getOwnerEmails();

        ArrayList<InternetAddress> to = new ArrayList();
        Iterator iter = ownerEmails.iterator();
        while (iter.hasNext())
        {
            String s = (String)iter.next();
            InternetAddress ia = new InternetAddress();
            ia.setAddress(s);
            to.add(ia);
        }

        ArrayList<InternetAddress> bcc = new ArrayList();
        if(ArcSpecManager.GetInstance().getEmailspecifications_projectAccess()){
            InternetAddress ia = new InternetAddress();
            ia.setAddress(AdminUtils.getAdminEmailId());
            bcc.add(ia);
        }
        
        String from = AdminUtils.getAdminEmailId();
        String subject = TurbineUtils.GetSystemName() + " Access Request for " + project.getName();

        try {
            EmailerI sm = EmailUtils.getEmailer();
            sm.setFrom(from);
            sm.setTo(to);
            sm.setBcc(bcc);
            sm.setSubject(subject);
            sm.setMsg(message);
            
            sm.send();
        } catch (Exception e) {
            logger.error("Unable to send mail",e);
            System.out.println("Error sending Email");
            throw e;
        }
        
        data.setMessage("Access request sent.");
        data.setScreenTemplate("Index.vm");
    }

}
