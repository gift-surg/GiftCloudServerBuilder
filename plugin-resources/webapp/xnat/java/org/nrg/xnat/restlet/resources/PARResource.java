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

import org.nrg.xft.XFTTable;
import org.nrg.xnat.turbine.utils.ProjectAccessRequest;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * @author timo
 *
 */
public class PARResource extends SecureResource {
	ProjectAccessRequest par=null;
	public PARResource(Context context, Request request, Response response) {
		super(context, request, response);
		String par_id = (String) getParameter(request,"PAR_ID");
		par = ProjectAccessRequest.RequestPARByGUID(par_id, user);
        if (par == null) {
            par = ProjectAccessRequest.RequestPARById(Integer.parseInt(par_id), user);
        }
		if (par != null) {
			this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
			this.getVariants().add(new Variant(MediaType.TEXT_HTML));
			this.getVariants().add(new Variant(MediaType.TEXT_XML));
		} else {
			this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
		}
	}
	
	public boolean allowPut(){
		return true;
	}

	@Override
	public void handlePut() {
		if(par!=null){
			if (par.getApproved() != null || par.getApprovalDate() != null) {
				this.getResponse().setStatus(Status.CLIENT_ERROR_CONFLICT,"This project invitation has already been accepted.");
				return;
			}else{
				try {
					if(this.getQueryVariable("accept")!=null){
						par.process(user,true, getEventType(), getReason(), getComment());
					}else if(this.getQueryVariable("decline")!=null){
						par.process(user,false, getEventType(), getReason(), getComment());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			this.returnDefaultRepresentation();
		}else{
			this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
		}
		
	}
	

	@Override
	public Representation represent(Variant variant) {
		XFTTable table = new XFTTable();
		table.initTable(new String[]{"id","proj_id","create_date","level"});
		Hashtable<String,Object> params=new Hashtable<String,Object>();
		try {
			ArrayList<ProjectAccessRequest> pars = ProjectAccessRequest
					.RequestPARsByUserEmail(user.getEmail(), user);
			for (ProjectAccessRequest par : pars) {
				Object[] row = new Object[4];
				row[0] = par.getRequestId();
				row[1] = par.getProjectId();
				row[2] = par.getCreateDate();
				row[3] = par.getLevel();
				table.rows().add(row);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		

		MediaType mt = overrideVariant(variant);

		return this.representTable(table, mt, params);
	}
}
