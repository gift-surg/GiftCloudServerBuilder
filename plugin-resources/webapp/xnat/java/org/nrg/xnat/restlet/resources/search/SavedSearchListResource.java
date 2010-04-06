// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources.search;

import java.sql.SQLException;
import java.util.Hashtable;

import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

public class SavedSearchListResource extends SecureResource {

	public SavedSearchListResource(Context context, Request request, Response response) {
		super(context, request, response);
			this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
			this.getVariants().add(new Variant(MediaType.TEXT_HTML));
			this.getVariants().add(new Variant(MediaType.TEXT_XML));
	}

	@Override
	public Representation getRepresentation(Variant variant) {	
		MediaType mt = overrideVariant(variant);
		XFTTable table=null;
		try {
			String query="SELECT DISTINCT xss.* FROM xdat_stored_search xss LEFT JOIN xdat_stored_search_allowed_user xssau ON xss.id=xssau.xdat_stored_search_id LEFT JOIN xdat_stored_search_groupid xssag ON xss.id=xssag.allowed_groups_groupid_xdat_sto_id LEFT JOIN xdat_user_groupid ON xssag.groupid=xdat_user_groupid.groupid WHERE (xss.secure=0 OR xssau.login='" + user.getLogin() +"' OR groups_groupid_xdat_user_xdat_user_id="+ user.getXdatUserId() + ")";
			String includeTagged = this.getQueryVariable("includeTag");
			if(includeTagged!=null){
				if(includeTagged.equals("true")){
					query +=" AND xss.tag IS NOT NULL";
				}else{
					query +=" AND xss.tag='" + includeTagged +"'";
				}
			}else{
				query +=" AND xss.tag IS NULL";
			}
			table = XFTTable.Execute(query, user.getDBName(), user.getLogin());
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (DBPoolException e) {
			e.printStackTrace();
		}
		
		Hashtable<String,Object> params=new Hashtable<String,Object>();
		params.put("title", "Stored Searches");
		
		return this.representTable(table, mt, params);

	}
}
