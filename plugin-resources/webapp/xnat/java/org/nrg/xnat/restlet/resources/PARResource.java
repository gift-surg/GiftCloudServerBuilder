/**
 * Copyright 2008 Washington University - All rights reserved
 *
 * Author: Timothy Olsen
 */
package org.nrg.xnat.restlet.resources;

import java.util.ArrayList;
import java.util.Hashtable;

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
public class PARResource extends SecureResource {
	ProjectAccessRequest par=null;
	public PARResource(Context context, Request request, Response response) {
		super(context, request, response);
		String par_id = (String) getParameter(request,"PAR_ID");
		Integer id = Integer.valueOf(par_id);
		par = ProjectAccessRequest.RequestPARById(id, user);
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
	public Representation getRepresentation(Variant variant) {	
		XFTTable table = new XFTTable();
		table.initTable(new String[]{"id","proj_id","create_date","level"});
		Hashtable<String,Object> params=new Hashtable<String,Object>();
		try {
			ArrayList<ProjectAccessRequest> pars = ProjectAccessRequest
					.RequestPARsByUserEmeail(user.getEmail(), user);
			for (ProjectAccessRequest par : pars) {
				Object[] row = new Object[4];
				row[0] = par.getPar_id();
				row[1] = par.getProjectID();
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
