/**
 * Copyright 2008 Washington University - All rights reserved
 *
 * Author: Timothy Olsen
 */
package org.nrg.xnat.restlet.resources;

import java.util.ArrayList;
import java.util.Hashtable;

import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xft.XFTTable;
import org.nrg.xnat.turbine.utils.ProjectAccessRequest;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

/**
 * @author timo
 *
 */
public class ProjectPARListResource extends SecureResource {
	XnatProjectdata proj=null;

	public ProjectPARListResource(Context context, Request request,
			Response response) {
		super(context, request, response);
		String pID = (String) request.getAttributes().get("PROJECT_ID");
		if (pID != null) {
			proj = XnatProjectdata.getProjectByIDorAlias(pID, user, false);
		}

		if (proj != null) {
			this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
			this.getVariants().add(new Variant(MediaType.TEXT_HTML));
			this.getVariants().add(new Variant(MediaType.TEXT_XML));
		} else {
			this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,
					"Unknown project: " + pID);
		}
	}


	@Override
	public Representation getRepresentation(Variant variant) {	
		XFTTable table = new XFTTable();
		Hashtable<String,Object> params=new Hashtable<String,Object>();
		if (ProjectAccessRequest.CREATED_PAR_TABLE) {
			try {
				table = XFTTable
						.Execute(
								"SELECT par.par_id,par.proj_id,par.level,par.create_date,par.email,u.login,p.secondary_id,par.approved, par.approval_date FROM xs_par_table par LEFT JOIN xnat_projectData p ON par.proj_id=p.id LEFT JOIN xdat_user u ON par.approver_id=u.xdat_user_id WHERE par.proj_id='"
										+ proj.getId() + "'", user.getDBName(),
								user.getLogin());

			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			table = new XFTTable();
			String[] headers = { "par_id", "proj_id", "level", "create_date",
					"email", "login", "secondary_id", "approved",
					"approval_date" };
			table.initTable(headers);
		}

		MediaType mt = overrideVariant(variant);

		return this.representTable(table, mt, params);
	}
}
