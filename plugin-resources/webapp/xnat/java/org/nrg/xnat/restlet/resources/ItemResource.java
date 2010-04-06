// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.resources;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

public abstract class ItemResource extends SecureResource {

	public ItemResource(Context context, Request request, Response response) {
		super(context, request, response);
		
	}

}
