/**
 * 
 */
package org.nrg.xnat.restlet.util;

import java.util.ArrayList;
import java.util.Optional;

import javax.jms.IllegalStateException;

import org.nrg.xdat.exceptions.IllegalAccessException;
import org.nrg.xdat.om.ExtSubjectpseudonym;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xnat.restlet.resources.SecureResource;

/**
 * @author Dzhoshkun Shakir (d.shakir@ucl.ac.uk)
 *
 */
public class DefaultResourceUtil implements ResourceUtilI {
	XDATUser user;
	SecureResource resource;
	
	/**
	 * 
	 * @param user
	 * @param resource
	 */
	public DefaultResourceUtil(XDATUser user, SecureResource resource) {
		this.user = user;
		this.resource = resource;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.ResourceUtilI#getSubject(java.lang.String)
	 */
	@Override
	public Optional<XnatSubjectdata> getSubjectByLabelOrId(String descriptor) throws IllegalAccessException, Exception {
		CriteriaCollection criteria = new CriteriaCollection("OR");
		criteria.addClause("xnat:subjectData/label", descriptor);
		criteria.addClause("xnat:subjectData/id", descriptor);
		ArrayList<XnatSubjectdata> subjects = XnatSubjectdata.getXnatSubjectdatasByField(criteria, user, false);
		if (subjects.isEmpty())
			return Optional.empty();
		else if (subjects.size() > 1)
			throw new IllegalStateException("More than one subject with same label");
		else
			return Optional.of(subjects.get(0));
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.ResourceUtilI#getMatchingSubject(java.lang.String)
	 */
	@Override
	public XnatSubjectdata getMatchingSubject(String pseudoId) throws IllegalAccessException, Exception {
		Optional<ExtSubjectpseudonym> pseudonym = getPseudonym(pseudoId);
		if (!pseudonym.isPresent())
			return null;
		else
			return getMatchingSubject(pseudonym.get());
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.ResourceUtilI#getMatchingSubject(org.nrg.xdat.om.ExtSubjectpseudonym)
	 */
	@Override
	public XnatSubjectdata getMatchingSubject(ExtSubjectpseudonym pseudonym)
			throws IllegalAccessException, Exception {
		if (pseudonym == null)
			return null;
		else
			return getSubjectByLabelOrId(pseudonym.getSubject()).get();
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.ResourceUtilI#getPseudonym(java.lang.String)
	 */
	@Override
	public Optional<ExtSubjectpseudonym> getPseudonym(String pseudoId) throws IllegalAccessException, Exception {
		ExtSubjectpseudonym pseudonym = ExtSubjectpseudonym.getExtSubjectpseudonymsById(pseudoId, user, false); // TODO where is the user checking ?
		if (pseudonym == null)
			return Optional.empty();
		else
			return Optional.of(pseudonym);
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.ResourceUtilI#addPseudonym(org.nrg.xdat.om.XnatSubjectdata, org.nrg.xdat.om.ExtSubjectpseudonym)
	 */
	@Override
	public void addPseudoId(XnatSubjectdata subject,
			String pseudoId) throws IllegalAccessException, Exception {
		// check user's access rights
		if (!user.canEdit(subject)) {
			throw new IllegalAccessException("User "+user.getUsername()+" has insufficient access rights");
		}
		
		// put the new pseudonym
		ExtSubjectpseudonym newPseudonym = new ExtSubjectpseudonym();
		newPseudonym.setId(pseudoId);
		newPseudonym.setSubject(subject.getId());
		
		PersistentWorkflowI wrk = PersistentWorkflowUtils
				.getOrCreateWorkflowData(null, user, newPseudonym.getItem(), 
						EventUtils.newEventInstance(
								EventUtils.CATEGORY.DATA, 
								resource.getEventType(), 
								resource.getAction(), 
								pseudoId, 
								"Inserted new pseudonym for a subject."));
		try {
			if (SaveItemHelper.authorizedSave(newPseudonym.getItem(), user, false,
					true, wrk.buildEvent())) {
				PersistentWorkflowUtils.complete(wrk, wrk.buildEvent());
				MaterializedView.DeleteByUser(user);
			}
		} catch (Exception e) {
			PersistentWorkflowUtils.fail(wrk, wrk.buildEvent());
			throw e;
		}

		resource.returnXML(newPseudonym.getItem()); // TODO what is this for ?
	}
}
