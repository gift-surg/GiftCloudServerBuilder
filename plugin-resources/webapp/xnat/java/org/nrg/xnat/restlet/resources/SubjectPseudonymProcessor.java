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
		rid = new String((String) getParameter(request, "SUBJECT_ID"));
		ppid = new String((String) getParameter(request, "PPID"));
	}
	
	@Override
	public boolean allowPut() {
		return true;
	}
	
	@Override
	public void handlePut() {
		try {
			// population & sanity checks
			if (Strings.isNullOrEmpty(ppid)) {
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Empty PPID provided");
				return;
			}
			
			Optional<ExtSubjectpseudonym> pseudonym = resourceUtil.getPseudonym(ppid);
			if (pseudonym.isPresent()) {
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Provided PPID exists");
				return;
			}
			
			Optional<XnatSubjectdata> subject = resourceUtil.getSubjectByLabelOrId(rid);
			if (!subject.isPresent()) {
				getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Subject not found");
				return;
			}
			
			// add PPID
			resourceUtil.addPseudoId(subject.get(), ppid);
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
