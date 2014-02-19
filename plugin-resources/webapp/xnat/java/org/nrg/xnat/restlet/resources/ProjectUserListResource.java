/*
 * org.nrg.xnat.restlet.resources.ProjectUserListResource
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.restlet.resources;

import org.apache.commons.lang.StringUtils;
import org.nrg.config.services.ConfigService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.DBPoolException;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.List;

public class ProjectUserListResource extends SecureResource {
    XFTTable table = null;
    XnatProjectdata proj = null;
    boolean displayHiddenUsers = false;

    public ProjectUserListResource(Context context, Request request, Response response) {
        super(context, request, response);

        this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
        this.getVariants().add(new Variant(MediaType.TEXT_HTML));
        this.getVariants().add(new Variant(MediaType.TEXT_XML));

        String pID = (String) getParameter(request, "PROJECT_ID");
        if (pID != null) {
            proj = XnatProjectdata.getProjectByIDorAlias(pID, user, false);
        }
        final Object projectData = proj.getItem().getProps().get("projectdata_info");
        if (!(projectData instanceof Integer)) {
            String message = "Can't parse the project data info identifier property for project " + proj.getDisplayName() + ". Object is " + (projectData == null ? "null" : projectData.getClass().getName()) + ". Must be an Integer object.";
            logger.error(message);
            this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, message);
        } else {
            int projectId = (Integer) projectData;
            if (!(user.isSiteAdmin() || user.isOwner(proj.getId()) || isWhitelisted(projectId))) {
            logger.error("Unauthorized Access to project-level user resources. User: " + userName);
            this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "Access Denied: Only project owners and site managers can access user resources.");
        }
        displayHiddenUsers = Boolean.parseBoolean((String)getParameter(request, "DISPLAY_HIDDEN_USERS"));
    }
    }

    @Override
    public boolean allowGet() {
        return true;
    }

    @Override
    public Representation represent(Variant variant) {

        if (proj != null) {
            try {
                StringBuffer query = new StringBuffer("SELECT g.id AS \"GROUP_ID\", displayname,login,firstname,lastname,email FROM xdat_userGroup g RIGHT JOIN xdat_user_Groupid map ON g.id=map.groupid RIGHT JOIN xdat_user u ON map.groups_groupid_xdat_user_xdat_user_id=u.xdat_user_id WHERE tag='").append(proj.getId()).append("' ");
                if(!displayHiddenUsers){
                    query.append(" and enabled = 1 ");
                }
                query.append(" ORDER BY g.id DESC;");
                table = XFTTable.Execute(query.toString(), user.getDBName(), user.getLogin());
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (DBPoolException e) {
                e.printStackTrace();
            }
        }

        Hashtable<String, Object> params = new Hashtable<String, Object>();
        params.put("title", "Projects");

        MediaType mt = overrideVariant(variant);

        if(table!=null)params.put("totalRecords", table.size());
        return this.representTable(table, mt, params);
    }

    public boolean isWhitelisted() {
        final Object projectData = proj.getItem().getProps().get("projectdata_info");
        if (!(projectData instanceof Integer)) {
            throw new RuntimeException("Can't parse the project data info identifier property for project " + proj.getDisplayName() + ". Object is " + (projectData == null ? "null" : projectData.getClass().getName()) + ". Must be an Integer object.");
        }
        ConfigService configService = XDAT.getConfigService();
        String config = configService.getConfigContents("user-resource-whitelist", "whitelist.json", Long.valueOf((Integer) projectData));
        if (!StringUtils.isBlank(config)) {
            try {
                List<String> projectUserResourceWhitelist = OBJECT_MAPPER.readValue(config, TYPE_REFERENCE_LIST_STRING);
                if (projectUserResourceWhitelist != null) {
                    return projectUserResourceWhitelist.contains(user.getUsername());
                }
            } catch (IOException e) {
                logger.error("", e);
            }
        }

        return false;
    }
}