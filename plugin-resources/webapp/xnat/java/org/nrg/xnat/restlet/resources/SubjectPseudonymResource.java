/**
 * 
 */
package org.nrg.xnat.restlet.resources;

import org.nrg.xdat.exceptions.IllegalAccessException;
import org.nrg.xnat.restlet.util.DefaultResourceUtil;
import org.nrg.xnat.restlet.util.ResourceUtilI;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

/**
 * Base class for all subject-pseudonym processing.
 * 
 * @author Dzhoshkun Shakir
 *
 */
public abstract class SubjectPseudonymResource extends QueryOrganizerResource {
	protected ResourceUtilI resourceUtil;
	
	/**
	 * 
	 * @param context
	 * @param request
	 * @param response
	 */
	protected SubjectPseudonymResource(Context context, Request request,
			Response response) {
		super(context, request, response);
		init();
	}
	
	/**
	 * 
	 */
	protected void init() {
		resourceUtil = new DefaultResourceUtil(user);
	}
	
	/**
	 * Handle how standard exceptions are propagated to user.
	 * 
	 * @param t
	 */
	protected void handle(Throwable t) {
		if (t instanceof IllegalAccessException)
			getResponse().setStatus(Status.CLIENT_ERROR_UNAUTHORIZED, "Insufficient access permissions");
		else
			getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, t.getMessage());
	}
}
