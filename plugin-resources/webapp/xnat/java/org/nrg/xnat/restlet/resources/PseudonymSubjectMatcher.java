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

import java.util.ArrayList;
import java.util.Optional;

import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

import com.google.common.base.Strings;


/**
 * Does the pseudonym-to-subject matching.
 * 
 * @author Dzhoshkun Shakir (d.shakir@ucl.ac.uk)
 *
 */
public class PseudonymSubjectMatcher extends SubjectPseudonymResource {
	String ppid;
	String projectId;
	
	/**
	 * 
	 * @param context
	 * @param request
	 * @param response
	 */
	public PseudonymSubjectMatcher(Context context, Request request,
			Response response) {
		super(context, request, response);
		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
		getVariants().add(new Variant(MediaType.TEXT_HTML));
		getVariants().add(new Variant(MediaType.TEXT_XML));
		projectId = (String) getParameter(request, "PROJECT_ID");
		ppid = (String) getParameter(request, "PPID");
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.nrg.xnat.restlet.resources.QueryOrganizerResource#getRepresentation(org.restlet.resource.Variant)
	 */
	@Override
	public Representation getRepresentation(Variant variant) {
		// sanity check
		if (Strings.isNullOrEmpty(projectId)) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Empty PROJECT_ID provided");
			return null;
		}
		if (Strings.isNullOrEmpty(ppid)) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Empty PPID provided");
			return null;
		}
		
		// get subject
		Optional<XnatSubjectdata> subject;
		try {
			subject = secureItemUtil.getMatchingSubject(projectId, ppid);
		} catch (Throwable t) {
			handle(t);
			subject = Optional.empty();
		}
		
		// represent subject after sanity check
		XnatSubjectdata result = null;
		if (!subject.isPresent()) {
			getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
			return null;
		}
		else {
			result = subject.get();
		}
		return representItem(result.getItem(), variant.getMediaType());
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.restlet.resource.Resource#represent(org.restlet.resource.Variant)
	 */
	@Override
	public Representation represent(Variant variant) throws ResourceException {
		return getRepresentation(variant);
	}

	/*
	 * (non-Javadoc)
	 * @see org.nrg.xnat.restlet.resources.QueryOrganizerResource#getDefaultFields(org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement)
	 */
	@Override
	public ArrayList<String> getDefaultFields(GenericWrapperElement e) {
		// TODO - this is merely copied from SubjectResource
		ArrayList<String> al = new ArrayList<String>();

		al.add("ID");
		al.add("project");
		al.add("label");
		al.add("insert_date");
		al.add("insert_user");

		return al;
	}

	@Override
	public String getDefaultElementName() {
		return "xnat:subjectData";
	}
}
