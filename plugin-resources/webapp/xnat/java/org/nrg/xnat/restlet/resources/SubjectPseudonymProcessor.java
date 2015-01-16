/**
 * 
 */
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
		return true;
	}
	
	@Override
	public void handleDelete() {
		// TODO
	}

	@Override
	public ArrayList<String> getDefaultFields(GenericWrapperElement e) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDefaultElementName() {
		// TODO Auto-generated method stub
		return null;
	}
}
