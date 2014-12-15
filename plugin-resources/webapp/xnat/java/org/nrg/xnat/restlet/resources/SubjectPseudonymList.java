/**
 * 
 */
package org.nrg.xnat.restlet.resources;

import java.util.ArrayList;

import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

/**
 * @author dzhoshkun
 *
 */
public class SubjectPseudonymList extends QueryOrganizerResource {

	public SubjectPseudonymList(Context context, Request request,
			Response response) {
		super(context, request, response);
		// TODO ___ 3)
		// org.nrg.xnat.restlet.resources.SubjectPseudoymList.SubjectPseudoymList(Context,
		// Request, Response)
	}

	@Override
	public ArrayList<String> getDefaultFields(GenericWrapperElement e) {
		// TODO ___ 3.1)
		// org.nrg.xnat.restlet.resources.SubjectPseudoymList.getDefaultFields(GenericWrapperElement)
		return null;
	}

	@Override
	public String getDefaultElementName() {
		// TODO ___ 3.2)
		// org.nrg.xnat.restlet.resources.SubjectPseudoymList.getDefaultElementName()
		return null;
	}

}
