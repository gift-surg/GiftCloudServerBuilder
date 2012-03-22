package org.nrg.xnat.restlet.resources;

import java.io.IOException;

import javax.servlet.http.Cookie;

import org.nrg.xnat.restlet.util.UpdateExpirationCookie;
import org.restlet.Context;
import org.restlet.data.CookieSetting;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

public class Touch extends SecureResource {

	public Touch(Context context, Request request, Response response) {
		super(context, request, response);
		this.getVariants().add(new Variant(MediaType.ALL));
	}
	@Override
	public boolean allowPut () {
		return false;
	}
	@Override
	public boolean allowPost () {
		return false;
	}
	@Override
	public boolean allowGet () {
		return true;
	}
	
	@Override
	public Representation represent (Variant v) {
		// Cookies inserted by the UpdateExpirationCookie filter are removed by this point so have
		// to add them again to the response.
		int idleTime = this.getHttpSession().getMaxInactiveInterval();
		Cookie cookie = UpdateExpirationCookie.makeCookie(idleTime);
		
		this.getResponse().getCookieSettings().add(new CookieSetting(cookie.getName(), cookie.getValue()));
		return new StringRepresentation(cookie.getValue());
	}
}
