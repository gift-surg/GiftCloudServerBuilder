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

import org.nrg.xdat.om.XnatImagescandata;
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
 * 
 * 
 * @author Dzhoshkun Shakir (d.shakir@ucl.ac.uk)
 *
 */
public class UidScanMatcher extends SecureQueryOrganizerResource {
	String projectId;
	String rid;
	String exptId;
	String uid;

	/**
	 * 
	 * @param context
	 * @param request
	 * @param response
	 */
	public UidScanMatcher(Context context, Request request, Response response) {
		super(context, request, response);
		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
		getVariants().add(new Variant(MediaType.TEXT_HTML));
		getVariants().add(new Variant(MediaType.TEXT_XML));
		projectId = (String) getParameter(request, "PROJECT_ID");
		rid = (String) getParameter(request, "SUBJECT_ID");
		exptId = (String) getParameter(request, "ASSESSED_ID");
		uid = (String) getParameter(request, "UID");
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.resources.QueryOrganizerResource#getDefaultFields(org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement)
	 */
	@Override
	public ArrayList<String> getDefaultFields(GenericWrapperElement e) {
		ArrayList<String> al = new ArrayList<String>();

		al.add("id");
		al.add("project");
		al.add("uid");
		al.add("series_description");
		al.add("image_session_id");
		al.add("insert_date");
		al.add("insert_user");

		return al;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.resources.QueryOrganizerResource#getDefaultElementName()
	 */
	@Override
	public String getDefaultElementName() {
		return "xnat:imageScanData";
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
		if (Strings.isNullOrEmpty(rid)) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Empty SUBJECT_ID provided");
			return null;
		}
		if (Strings.isNullOrEmpty(exptId)) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Empty ASSESSED_ID (EXPT_ID) provided");
			return null;
		}
		if (Strings.isNullOrEmpty(uid)) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Empty UID provided");
			return null;
		}
		
		// get subject
		Optional<XnatImagescandata> scan;
		try {
			scan = secureItemUtil.getMatchingScan(projectId, rid, exptId, uid);
		} catch (Throwable t) {
			handle(t);
			scan = Optional.empty();
		}
		
		// represent subject after sanity check
		XnatImagescandata result = null;
		if (!scan.isPresent()) {
			getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
			return null;
		}
		else {
			result = scan.get();
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

}
