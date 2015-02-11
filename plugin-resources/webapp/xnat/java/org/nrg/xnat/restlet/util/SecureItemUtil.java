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
package org.nrg.xnat.restlet.util;

import java.util.ArrayList;
import java.util.Optional;

import org.nrg.xdat.exceptions.IllegalAccessException;
import org.nrg.xdat.om.ExtSubjectpseudonym;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.om.base.auto.AutoExtSubjectpseudonym;
import org.nrg.xft.XFTItem;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.event.persist.PersistentWorkflowUtils.ActionNameAbsent;
import org.nrg.xft.event.persist.PersistentWorkflowUtils.IDAbsent;
import org.nrg.xft.event.persist.PersistentWorkflowUtils.JustificationAbsent;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xnat.security.ISecurityUtil;

/**
 * @author Dzhoshkun Shakir (d.shakir@ucl.ac.uk)
 *
 */
public final class SecureItemUtil implements ISecureItemUtil {
	private ISecurityUtil securityUtil;
	
	/**
	 * 
	 * @param item
	 * @throws IllegalAccessException
	 */
	private void checkCanReadAndThrow(XFTItem item) throws IllegalAccessException {
		if (!securityUtil.canRead(item))
			throw new IllegalAccessException("User " + securityUtil.getUser().getUsername() + " has insufficient read access");
	}
	
	/**
	 * 
	 * @param item
	 * @throws IllegalAccessException
	 */
	private void checkCanEditAndThrow(XFTItem item) throws IllegalAccessException {
		if (!securityUtil.canEdit(item))
			throw new IllegalAccessException("User " + securityUtil.getUser().getUsername() + " has insufficient write access");
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.ISecureItemUtil#setSecurityUtil(org.nrg.xnat.security.ISecurityUtil)
	 */
	@Override
	public void setSecurityUtil(ISecurityUtil securityUtil) {
		this.securityUtil = securityUtil;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.ResourceUtilI#getSubject(java.lang.String)
	 */
	@Override
	public Optional<XnatSubjectdata> getSubjectByLabelOrId(String descriptor) throws IllegalAccessException {
		CriteriaCollection criteria = new CriteriaCollection("OR");
		criteria.addClause("xnat:subjectData/label", descriptor);
		criteria.addClause("xnat:subjectData/id", descriptor);
		ArrayList<XnatSubjectdata> subjects = XnatSubjectdata.getXnatSubjectdatasByField(criteria, securityUtil.getUser(), false);
		Optional<XnatSubjectdata> subject;
		if (subjects.isEmpty())
			subject = Optional.empty();
		else if (subjects.size() > 1)
			// TODO throw new IllegalStateException("More than one subject with same label");
			subject = Optional.empty();
		else {
			subject = Optional.of(subjects.get(0));
			checkCanReadAndThrow(subject.get().getItem());
		}
		return subject;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.ResourceUtilI#getMatchingSubject(java.lang.String)
	 */
	@Override
	public Optional<XnatSubjectdata> getMatchingSubject(String pseudoId) throws IllegalAccessException {
		Optional<ExtSubjectpseudonym> pseudonym = getPseudonym(pseudoId);
		if (!pseudonym.isPresent())
			return Optional.empty();
		else
			return getSubjectByLabelOrId(pseudonym.get().getSubject());
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.ResourceUtilI#getPseudonym(java.lang.String)
	 */
	@Override
	public Optional<ExtSubjectpseudonym> getPseudonym(String pseudoId) throws IllegalAccessException {
		ExtSubjectpseudonym tmp = AutoExtSubjectpseudonym.getExtSubjectpseudonymsById(pseudoId, securityUtil.getUser(), false);
		Optional<ExtSubjectpseudonym> pseudonym;
		if (tmp == null)
			pseudonym = Optional.empty();
		else {
			pseudonym = Optional.of(tmp);
			checkCanReadAndThrow(pseudonym.get().getItem());
		}
		return pseudonym;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.ResourceUtilI#addPseudonym(org.nrg.xdat.om.XnatSubjectdata, org.nrg.xdat.om.ExtSubjectpseudonym)
	 */
	@Override
	public Optional<ExtSubjectpseudonym> addPseudoId(XnatSubjectdata subject,
			String pseudoId) throws IllegalAccessException {
		checkCanEditAndThrow(subject.getItem());
		
		// put the new pseudonym
		ExtSubjectpseudonym newPseudonym = new ExtSubjectpseudonym();
		newPseudonym.setId(pseudoId);
		newPseudonym.setSubject(subject.getId());
		
		PersistentWorkflowI wrk;
		try {
			wrk = PersistentWorkflowUtils
					.getOrCreateWorkflowData(null, securityUtil.getUser(), newPseudonym.getItem(), 
							EventUtils.newEventInstance(
									EventUtils.CATEGORY.DATA, 
									securityUtil.getResource().getEventType(), 
									securityUtil.getResource().getAction(), 
									pseudoId, 
									"Inserted new pseudonym for a subject."));
			
		} catch (JustificationAbsent | ActionNameAbsent | IDAbsent e1) { // from PersistentWorkflowUtils.getOrCreateWorkflowData
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return Optional.empty();
		}
		
		try {
			if (SaveItemHelper.authorizedSave(newPseudonym.getItem(), securityUtil.getUser(), false,
					true, wrk.buildEvent())) {
				PersistentWorkflowUtils.complete(wrk, wrk.buildEvent());
				MaterializedView.DeleteByUser(securityUtil.getUser());
			}
		} catch (Exception e) { // from SaveItemHelper.authorizedSave, PersistentWorkflowUtils.complete, or MaterializedView.DeleteByUser
			try {
				PersistentWorkflowUtils.fail(wrk, wrk.buildEvent());
			} catch (Exception e1) {
				// TODO this is becoming uglier, but we have to stop unspecific exceptions somewhere right ?
				e1.printStackTrace();
			}
//			throw e; // TODO originally part of code
			return Optional.empty();
		}
		
		return Optional.of(newPseudonym);
	}
}
