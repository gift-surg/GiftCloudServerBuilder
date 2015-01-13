/**
 * 
 */
package org.nrg.xnat.restlet.resources;

import org.nrg.xdat.om.ExtPseudonymizedsubjectdata;
import org.nrg.xdat.om.ExtSubjectpseudonym;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

import com.google.common.base.Strings;

/**
 * Base class for all subject-pseudonym processing.
 * 
 * @author Dzhoshkun Shakir
 *
 */
public abstract class SubjectPseudonymResource extends QueryOrganizerResource {
	ExtPseudonymizedsubjectdata subject;
	ExtSubjectpseudonym pseudonym;
	
	public SubjectPseudonymResource(Context context, Request request,
			Response response) {
		super(context, request, response);
	}
	
	protected void populate(String rid, String ppid) {
		if (!Strings.isNullOrEmpty(rid))
			subject = ExtPseudonymizedsubjectdata.getExtPseudonymizedsubjectdatasById(pseudonym.getPseudonymizedSubjectId(), user, false);
		else
			subject = null;
		
		if (!Strings.isNullOrEmpty(ppid)) {
			pseudonym = ExtSubjectpseudonym.GetPseudonym(subject, ppid, user, false);
			if (pseudonym != null && subject == null)
				subject = ExtPseudonymizedsubjectdata.getExtPseudonymizedsubjectdatasById(pseudonym.getPseudonymizedSubjectId(), user, false);
		}
	}
}
