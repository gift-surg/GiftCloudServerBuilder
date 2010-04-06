// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources;

import java.sql.SQLException;
import java.util.Hashtable;

import org.nrg.xft.XFTTable;
import org.nrg.xft.db.FavEntries;
import org.nrg.xft.exception.DBPoolException;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

public class UserFavoritesList extends SecureResource {
	String dataType=null;
	
	public UserFavoritesList(Context context, Request request, Response response) {
		super(context, request, response);
		
			this.getVariants().add(new Variant(MediaType.APPLICATION_JSON));
			this.getVariants().add(new Variant(MediaType.TEXT_HTML));
			this.getVariants().add(new Variant(MediaType.TEXT_XML));
			
			dataType=getQueryVariable("DATA_TYPE");
		}

	@Override
	public Representation getRepresentation(Variant variant) {	
		XFTTable table = null;
		if(dataType!=null){
			try {	            
				 table=FavEntries.GetFavoriteEntries(dataType, user);
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (DBPoolException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		Hashtable<String,Object> params=new Hashtable<String,Object>();
		params.put("title", "User Favorites");

		MediaType mt = overrideVariant(variant);

		return this.representTable(table, mt, params);
	}
}
