/**
 * 
 */
package org.nrg.xnat.restlet.resources;

import java.util.ArrayList;

import org.nrg.xdat.om.ExtSubjectpseudonym;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xft.XFTItem;
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
 * @author Dzhoshkun Shakir
 *
 */
public class PseudonymSubjectMatcher extends SubjectPseudonymResource {
	String ppid;
	
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
		ppid = new String((String) getParameter(request, "PPID"));
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.nrg.xnat.restlet.resources.QueryOrganizerResource#getRepresentation(org.restlet.resource.Variant)
	 */
	@Override
	public Representation getRepresentation(Variant variant) {
		// sanity check
		if (Strings.isNullOrEmpty(ppid)) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "Empty PPID provided");
			return null;
		}
		
		// get subject
		XnatSubjectdata subject = null;
		try {
			subject = resourceUtil.getMatchingSubject(ppid);
		} catch (Throwable t) {
			handle(t);
			return null;
		}
		
		// represent subject after sanity check
		if (subject == null) {
			getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, "PPID not found");
			return null;
		}
		else {
			return representItem(subject.getItem(), variant.getMediaType());
		}
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
	 * @see org.nrg.xnat.restlet.resources.SecureResource#representItem(org.nrg.xft.XFTItem, org.restlet.data.MediaType)
	 */
	@Override
	public Representation representItem(XFTItem item, MediaType mt) {
		// TODO strip subject of children, and emphasise label, at the very least.
		return super.representItem(item, mt);
	}

	/*
	 * (non-Javadoc)
	 * @see org.nrg.xnat.restlet.resources.QueryOrganizerResource#getDefaultFields(org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement)
	 */
	@Override
	public ArrayList<String> getDefaultFields(GenericWrapperElement e) {
		// TODO
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
