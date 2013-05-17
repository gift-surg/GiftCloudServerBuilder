// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.Callable;

import org.codehaus.jackson.map.ObjectMapper;
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

public class ProjectUserListResource extends SecureResource  {
	XFTTable table = null;
	XnatProjectdata proj=null;
	
	public ProjectUserListResource(Context context, Request request, Response response) {
		super(context, request, response);

			this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
			this.getVariants().add(new Variant(MediaType.TEXT_HTML));
			this.getVariants().add(new Variant(MediaType.TEXT_XML));

        String pID= (String)getParameter(request,"PROJECT_ID");
			if(pID!=null){
				proj = XnatProjectdata.getProjectByIDorAlias(pID, user, false);
            }
            if (!(user.isSiteAdmin() || user.isOwner(proj.getName()) || isWhitelisted())) {
                logger.error("Unauthorized Access to project-level user resources. User: " + userName);
                this.getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN,"Access Denied: Only project owners and site managers can access user resources.");
        }
	}

	@Override
	public boolean allowGet() {
		return true;
	}

	@Override
	public Representation getRepresentation(Variant variant) {	

		if(proj!=null){
			try {
				String query = "SELECT g.id AS \"GROUP_ID\", displayname,login,firstname,lastname,email FROM xdat_userGroup g RIGHT JOIN xdat_user_Groupid map ON g.id=map.groupid RIGHT JOIN xdat_user u ON map.groups_groupid_xdat_user_xdat_user_id=u.xdat_user_id  WHERE tag='" + proj.getId() + "' ORDER BY g.id DESC;";
				table = XFTTable.Execute(query, user.getDBName(), user.getLogin());
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (DBPoolException e) {
				e.printStackTrace();
			}
		}
		
		Hashtable<String,Object> params=new Hashtable<String,Object>();
		params.put("title", "Projects");

		MediaType mt = overrideVariant(variant);
		
		return this.representTable(table, mt, params);
	}

    public boolean isWhitelisted() {
        ConfigService configService = XDAT.getConfigService();
        String config = configService.getConfigContents("user-resource-whitelist", "whitelist.json", getProjectId);
        ObjectMapper mapper = new ObjectMapper();
        ArrayList<String> projectUserResourceWhitelist = new ArrayList();

        try {
            projectUserResourceWhitelist = mapper.readValue(config, ArrayList.class);
        } catch (IOException e) {
            logger.error("", e);
        }

        if (projectUserResourceWhitelist.contains(user.getUsername())) {
            return true;
        } else {
            return false;
        }
    }

    Callable<Long> getProjectId = new Callable<Long>() {
        public Long call() {
            return new Long((Integer)proj.getItem().getProps().get("projectdata_info"));
        }
    };
}