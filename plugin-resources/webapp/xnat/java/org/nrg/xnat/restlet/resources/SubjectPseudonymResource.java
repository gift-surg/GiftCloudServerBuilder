/*=============================================================================

  GIFT-Cloud: A data storage and collaboration platform

  Copyright (c) University College London (UCL). All rights reserved.

  Parts of this software are derived from XNAT
    http://www.xnat.org
    Copyright (c) 2014, Washington University School of Medicine
    All Rights Reserved
    Released under the Simplified BSD.

  This software is distributed WITHOUT ANY WARRANTY; without even
  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
  PURPOSE.

  See LICENSE.txt in the top level directory for details.

=============================================================================*/
package org.nrg.xnat.restlet.resources;

import org.nrg.xdat.exceptions.IllegalAccessException;
import org.nrg.xnat.restlet.util.ISecureItemUtil;
import org.nrg.xnat.restlet.util.SecureUtilFactory;
import org.nrg.xnat.security.ISecurityUtil;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

/**
 * Base class for all subject-pseudonym processing.
 * 
 * @author Dzhoshkun Shakir (d.shakir@ucl.ac.uk)
 *
 */
public abstract class SubjectPseudonymResource extends QueryOrganizerResource {
	protected ISecureItemUtil secureItemUtil = null;
	
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
		ISecurityUtil securityUtil = SecureUtilFactory.getSecurityUtilInstance();
		securityUtil.setUser(user);
		securityUtil.setResource(this);
		// IllegalArgumentException not caught here, based on advice in http://stackoverflow.com/questions/15208544/when-should-an-illegalargumentexception-be-thrown
		secureItemUtil = SecureUtilFactory.getSecureItemUtilInstance(securityUtil);
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
