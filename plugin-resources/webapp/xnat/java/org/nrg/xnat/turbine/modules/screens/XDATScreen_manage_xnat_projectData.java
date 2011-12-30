//Copyright 2007 Washington University School of Medicine All Rights Reserved
/*
 * Created on Jul 2, 2007
 *
 */
package org.nrg.xnat.turbine.modules.screens;

import java.sql.SQLException;
import java.util.TreeMap;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.base.BaseXnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.screens.SecureReport;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.XFTTable;
import org.nrg.xft.event.EventUtils;
import org.nrg.xnat.turbine.utils.ProjectAccessRequest;

public class XDATScreen_manage_xnat_projectData  extends SecureReport {
    public static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(XDATScreen_report_xnat_projectData.class);
    /* (non-Javadoc)
     * @see org.nrg.xdat.turbine.modules.screens.SecureReport#finalProcessing(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    public void finalProcessing(RunData data, Context context) {
        XnatProjectdata project = (XnatProjectdata)om;
        XDATUser user = TurbineUtils.getUser(data);
        try {
            context.put("guest", project.getPublicAccessibility());

            project.initGroups(EventUtils.ADMIN_EVENT(user));
            
            XFTTable table = XFTTable.Execute("select TRIM(email) AS email, lastname || ', ' || firstname AS user_name FROM xdat_user WHERE email IS NOT NULL ORDER BY LOWER(lastname);", project.getDBName(), user.getLogin());
            context.put("allUsers", table.convertToMap("user_name", "email",new TreeMap<Object, Object>()));
            
            context.put("pars",ProjectAccessRequest.RequestPARs(" proj_id='" + project.getId() + "' AND approved IS NULL", user));
            
            context.put("ownerEmails", project.getGroupMembers(BaseXnatProjectdata.OWNER_GROUP));
            context.put("membersEmails", project.getGroupMembers(BaseXnatProjectdata.MEMBER_GROUP));
            context.put("collaboratorEmails", project.getGroupMembers(BaseXnatProjectdata.COLLABORATOR_GROUP));

        } catch (SQLException e) {
            logger.error("",e);
        } catch (Exception e) {
            logger.error("",e);
        }
    }
}