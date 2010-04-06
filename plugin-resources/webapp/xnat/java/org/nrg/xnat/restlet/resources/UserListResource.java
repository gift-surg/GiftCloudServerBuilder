// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources;

import java.sql.SQLException;
import java.util.Hashtable;

import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.DBPoolException;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

public class UserListResource extends SecureResource {
	XFTTable table = null;

	public UserListResource(Context context, Request request, Response response) {
		super(context, request, response);
		
				this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
				this.getVariants().add(new Variant(MediaType.TEXT_HTML));
				this.getVariants().add(new Variant(MediaType.TEXT_XML));
			
			try {
				String query = "SELECT xdat_user_id,login,firstname,lastname,email FROM xdat_user WHERE enabled=1 ORDER BY lastname;";
				table = XFTTable.Execute(query, user.getDBName(), user.getLogin());
			} catch (SQLException e) {
			logger.error("", e);
			} catch (DBPoolException e) {
			logger.error("", e);
		}
	}

	@Override
	public boolean allowGet() {
		return true;
	}

	@Override
	public Representation getRepresentation(Variant variant) {	
		Hashtable<String,Object> params=new Hashtable<String,Object>();
		params.put("title", "Projects");

		MediaType mt = overrideVariant(variant);

			try {
				String query = "SELECT xdat_user_id,login,firstname,lastname,email FROM xdat_user WHERE enabled=1 ORDER BY lastname;";
				table = XFTTable.Execute(query, user.getDBName(), user.getLogin());
			} catch (SQLException e) {
			logger.error("", e);
			} catch (DBPoolException e) {
			logger.error("", e);
		}
		
		if(table!=null)params.put("totalRecords", table.size());
		return this.representTable(table, mt, params);
	}
}
