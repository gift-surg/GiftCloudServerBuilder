// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
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

public class ProjectUserListResource extends SecureResource {
    XFTTable table = null;
    XnatProjectdata proj = null;

    public ProjectUserListResource(Context context, Request request, Response response) {
        super(context, request, response);

        this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
        this.getVariants().add(new Variant(MediaType.TEXT_HTML));
        this.getVariants().add(new Variant(MediaType.TEXT_XML));

        String pID = (String) getParameter(request, "PROJECT_ID");
        if (pID != null) {
            proj = XnatProjectdata.getProjectByIDorAlias(pID, user, false);
        }
        if (!(user.isSiteAdmin() || user.isOwner(proj.getName()) || isWhitelisted())) {
            logger.error("Unauthorized Access to project-level user resources. User: " + userName);
            this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "Access Denied: Only project owners and site managers can access user resources.");
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
                String query = "SELECT g.id AS \"GROUP_ID\", displayname,login,firstname,lastname,email FROM xdat_userGroup g RIGHT JOIN xdat_user_Groupid map ON g.id=map.groupid RIGHT JOIN xdat_user u ON map.groups_groupid_xdat_user_xdat_user_id=u.xdat_user_id  WHERE tag='" + proj.getId() + "' ORDER BY g.id DESC;";
                table = XFTTable.Execute(query, user.getDBName(), user.getLogin());
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (DBPoolException e) {
                e.printStackTrace();
            }
        }

        Hashtable<String, Object> params = new Hashtable<String, Object>();
        params.put("title", "Projects");

        MediaType mt = overrideVariant(variant);

        return this.representTable(table, mt, params);
    }

    public boolean isWhitelisted() {
        final Object projectdata_info = proj.getItem().getProps().get("projectdata_info");
        if (!(projectdata_info instanceof Integer)) {
            throw new RuntimeException("Can't parse the project data info identifier property for project " + proj.getDisplayName() + ". Object is " + (projectdata_info == null ? "null" : projectdata_info.getClass().getName()) + ". Must be an Integer object.");
        }
        ConfigService configService = XDAT.getConfigService();
        String config = configService.getConfigContents("user-resource-whitelist", "whitelist.json", Long.valueOf((Integer) projectdata_info));
        if (!StringUtils.isBlank(config)) {
            ObjectMapper mapper = new ObjectMapper();

            try {
                List<String> projectUserResourceWhitelist = mapper.readValue(config, new TypeReference<ArrayList<String>>() {});
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