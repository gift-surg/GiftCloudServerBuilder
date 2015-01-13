/**
 * 
 */
package org.nrg.xnat.restlet.resources;

import java.util.ArrayList;

import org.nrg.xft.XFTItem;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;


/**
 * Does the pseudonym-to-subject matching.
 * 
 * @author Dzhoshkun Shakir
 *
 */
public class PseudonymSubjectMatcher extends SubjectPseudonymResource {
	public PseudonymSubjectMatcher(Context context, Request request,
			Response response) {
		super(context, request, response);
		populate(null, 
				(String) getParameter(request, "PPID"));
		if (pseudonym != null) {
			getVariants().add(new Variant(MediaType.APPLICATION_JSON));
			getVariants().add(new Variant(MediaType.TEXT_HTML));
			getVariants().add(new Variant(MediaType.TEXT_XML));
		}
	}
	
	@Override
	public Representation getRepresentation(Variant variant) {
		Representation representation = representItem(subject.getItem(), variant.getMediaType());
		return representation;
	}
	
	@Override
	public Representation represent(Variant variant) throws ResourceException {
		return getRepresentation(variant);
	}
	
	@Override
	public Representation representItem(XFTItem item, MediaType mt) {
		// TODO strip subject of children, and emphasise label, at the very least.
		return super.representItem(item, mt);
	}

	@Override
	public ArrayList<String> getDefaultFields(GenericWrapperElement e) {
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
		return "ext:pseudonymizedSubjectData";
	}
}
