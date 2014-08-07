/*
 * org.nrg.xnat.restlet.resources.PARResource
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 10/29/13 3:29 PM
 */
package org.nrg.xnat.restlet.resources;

import org.apache.commons.lang.StringUtils;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xft.XFTTable;
import org.nrg.xnat.turbine.utils.ProjectAccessRequest;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * @author timo
 */
public class PARResource extends SecureResource {
    private static final Logger _log = LoggerFactory.getLogger(PARResource.class);
	ProjectAccessRequest par=null;

    public PARResource(Context context, Request request, Response response) throws Exception {
		super(context, request, response);
		String par_id = (String) getParameter(request,"PAR_ID");
		par = ProjectAccessRequest.RequestPARByGUID(par_id, user);
        if (par == null) {
            par = ProjectAccessRequest.RequestPARById(Integer.parseInt(par_id), user);
        }
		if (par != null) {
            getVariants().addAll(STANDARD_VARIANTS);
            final String projectId = par.getProjectId();
            if (StringUtils.isBlank(projectId)) {
                if (!user.isSiteAdmin()) {
                    response.setStatus(Status.CLIENT_ERROR_FORBIDDEN, "Only site admins can view this type of PAR.");
                    if (_log.isWarnEnabled()) {
                        _log.warn("Attempt by user " + user.getLogin() + " to access PAR " + par.getRequestId());
                    }
                }
		} else {
                XnatProjectdata project = XnatProjectdata.getXnatProjectdatasById(projectId, null, false);
                if (project == null) {
                    response.setStatus(Status.CLIENT_ERROR_NOT_FOUND, "The project associated with the project access request appears to be gone.");
                    _log.error("Found the PAR " + par.getRequestId() + " which is missing associated project " + par.getProjectId());
                } else {
                    if (!user.isSiteAdmin() && !project.canEdit(user)) {
                        response.setStatus(Status.CLIENT_ERROR_FORBIDDEN, "You don't have the appropriate permissions to view this PAR (must be admin or have edit permissions on the associated project).");
                        if (_log.isWarnEnabled()) {
                            _log.warn("Attempt by user " + user.getLogin() + " to access PAR " + par.getRequestId() + " associated with project " + par.getProjectId());
                        }
                    }
                }
            }
        } else {
            getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
		}
	}
	
	public boolean allowPut(){
		return true;
	}

	@Override
	public void handlePut() {
		if(par!=null){
			if (par.getApproved() != null || par.getApprovalDate() != null) {
                getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT, "This project invitation has already been accepted.");
				return;
			}else{
				try {
                    if (getQueryVariable("accept") != null) {
						par.process(user,true, getEventType(), getReason(), getComment());
                    } else if (getQueryVariable("decline") != null) {
						par.process(user,false, getEventType(), getReason(), getComment());
					}
				} catch (Exception e) {
                    _log.error("Error trying to process PAR " + par.getRequestId(), e);
				}
			}
            returnDefaultRepresentation();
		}else{
            getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
		}
		
	}
	

	@Override
	public Representation represent(Variant variant) {
		XFTTable table = new XFTTable();
		table.initTable(new String[]{"id","proj_id","create_date","level"});
		Hashtable<String,Object> params=new Hashtable<String,Object>();
		try {
            ArrayList<ProjectAccessRequest> pars = ProjectAccessRequest.RequestPARsByUserEmail(user.getEmail(), user);
			for (ProjectAccessRequest par : pars) {
				Object[] row = new Object[4];
				row[0] = par.getRequestId();
				row[1] = par.getProjectId();
				row[2] = par.getCreateDate();
				row[3] = par.getLevel();
				table.rows().add(row);
			}

		} catch (Exception e) {
            _log.error("Error retrieving PAR " + par.getRequestId(), e);
		}
		
        return representTable(table, overrideVariant(variant), params);
	}
}
