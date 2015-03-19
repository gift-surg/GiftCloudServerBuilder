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

import org.nrg.xdat.om.ExtSubjectpseudonym;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

import com.google.common.base.Strings;

/**
 * Does the relevant processing when entering a new pseudonym.
 * 
 * @author Dzhoshkun Shakir (d.shakir@ucl.ac.uk)
 *
 */
public class SubjectPseudonymProcessor extends SubjectPseudonymResource {
	String rid, ppid;
	
	/**
	 * 
	 * @param context
	 * @param request
	 * @param response
	 */
	public SubjectPseudonymProcessor(Context context, Request request,
			Response response) {
		super(context, request, response);
		rid = (String) getParameter(request, "SUBJECT_ID");
		ppid = (String) getParameter(request, "PPID");
	}
	
	@Override
	public boolean allowPost() {
		return true;
	}
	
	@Override
	public void handlePost() {
		try {
			// population & sanity checks
			if (Strings.isNullOrEmpty(ppid)) {
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Empty PPID provided");
				return;
			}
			
			Optional<ExtSubjectpseudonym> pseudonym = secureItemUtil.getPseudonym(ppid);
			if (pseudonym.isPresent()) {
				getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "Provided PPID exists");
				return;
			}
			
			Optional<XnatSubjectdata> subject = secureItemUtil.getSubjectByLabelOrId(rid);
			if (!subject.isPresent()) {
				getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Subject not found");
				return;
			}
			
			// add PPID
			Optional<ExtSubjectpseudonym> newPseudonym = secureItemUtil.addPseudoId(subject.get(), ppid);
			if (newPseudonym.isPresent()) {
				returnXML(newPseudonym.get().getItem()); // TODO what is this for ? resource.returnDefaultRepresentation();
				getResponse().setStatus(Status.SUCCESS_CREATED);
			}
			else
				getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, "Unknown error occured when adding new pseudonym to subject " + subject.get().getLabel());
		} catch (Throwable t) {
			handle(t);
		}
	}
	
	@Override
	public boolean allowDelete() {
		return false;
	}

	@Override
	public ArrayList<String> getDefaultFields(GenericWrapperElement e) {
		// TODO - are these fields supposed to match the DB fields ?
		ArrayList<String> fields = new ArrayList<String>();
		fields.add("id");
		fields.add("subject_id");
		return fields;
	}

	@Override
	public String getDefaultElementName() {
		return "ext:subjectPseudonym";
	}
}
