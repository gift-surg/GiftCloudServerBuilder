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

import org.nrg.xdat.om.ExtSubjectpseudonym;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.om.base.auto.AutoExtSubjectpseudonym;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.XFTItem;
import org.nrg.xft.db.MaterializedView;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.event.persist.PersistentWorkflowUtils.ActionNameAbsent;
import org.nrg.xft.event.persist.PersistentWorkflowUtils.IDAbsent;
import org.nrg.xft.event.persist.PersistentWorkflowUtils.JustificationAbsent;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.identifier.IDGeneratorFactory;
import org.nrg.xft.identifier.IDGeneratorI;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xnat.restlet.resources.SecureResource;

/**
 * 
 * 
 * @author Dzhoshkun Shakir (d.shakir@ucl.ac.uk)
 *
 */
public final class DefaultItemUtil implements IItemUtil {
	XDATUser user;
	SecureResource resource;

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.IItemUtil#setUser(org.nrg.xdat.security.XDATUser)
	 */
	@Override
	public void setUser(XDATUser user) throws IllegalArgumentException {
		if (user == null)
			throw new IllegalArgumentException("User cannot be null");
		else
			this.user = user;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.IItemUtil#setResource(org.nrg.xnat.restlet.resources.SecureResource)
	 */
	@Override
	public void setResource(SecureResource resource) throws IllegalArgumentException {
		if (resource == null)
			throw new IllegalArgumentException("Resource cannot be null");
		else
			this.resource = resource;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.IItemUtil#getUser()
	 */
	@Override
	public XDATUser getUser() throws IllegalStateException {
		if (user == null)
			throw new IllegalStateException("Getter called before setter");
		else
			return user;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.IItemUtil#getResource()
	 */
	@Override
	public SecureResource getResource() throws IllegalStateException {
		if (resource == null)
			throw new IllegalStateException("Getter called before setter");
		else
			return resource;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.IItemUtil#getProjectByLabelOrIdImpl(java.lang.String)
	 */
	@Override
	public Optional<XnatProjectdata> getProjectByLabelOrIdImpl(String descriptor) {
		CriteriaCollection criteria = new CriteriaCollection("OR");
		criteria.addClause("xnat:projectData/name", descriptor);
		criteria.addClause("xnat:projectData/id", descriptor);
		ArrayList<XnatProjectdata> projects = XnatProjectdata.getXnatProjectdatasByField(criteria, user, false);
		Optional<XnatProjectdata> project;
		if (projects.isEmpty())
			project = Optional.empty();
		else if (projects.size() > 1)
			// TODO throw new IllegalStateException("More then one project with same label");
			project = Optional.empty();
		else
			project = Optional.of(projects.get(0));
		return project;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.IItemUtil#getSubjectByLabelOrIdImpl(java.lang.String)
	 */
	@Override
	public Optional<XnatSubjectdata> getSubjectByLabelOrIdImpl(String descriptor) {		
		CriteriaCollection criteria = new CriteriaCollection("OR");
		criteria.addClause("xnat:subjectData/label", descriptor);
		criteria.addClause("xnat:subjectData/id", descriptor);
		ArrayList<XnatSubjectdata> subjects = XnatSubjectdata.getXnatSubjectdatasByField(criteria, user, false);
		Optional<XnatSubjectdata> subject;
		if (subjects.isEmpty())
			subject = Optional.empty();
		else if (subjects.size() > 1)
			// TODO throw new IllegalStateException("More than one subject with same label");
			subject = Optional.empty();
		else
			subject = Optional.of(subjects.get(0));
		return subject;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.IItemUtil#getMatchingSubjectImpl(java.lang.String)
	 */
	@Override
	public Optional<XnatSubjectdata> getMatchingSubjectImpl(String projectId, String pseudoId) {
		Optional<ExtSubjectpseudonym> pseudonym = getPseudonymImpl(projectId, pseudoId);
		if (!pseudonym.isPresent())
			return Optional.empty();
		else {
			return getSubjectByLabelOrIdImpl(pseudonym.get().getSubject());
		}
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.IItemUtil#getPseudonymImpl(java.lang.String)
	 */
	@Override
	public Optional<ExtSubjectpseudonym> getPseudonymImpl(String projectId, String pseudoId) {
		CriteriaCollection criteria = new CriteriaCollection("AND");
		criteria.addClause("ext:subjectPseudonym/ppid", pseudoId);
		criteria.addClause("ext:subjectPseudonym/project", projectId);
		ArrayList<ExtSubjectpseudonym> pseudonyms = ExtSubjectpseudonym.getExtSubjectpseudonymsByField(criteria, user, false);
		Optional<ExtSubjectpseudonym> pseudonym;
		if (pseudonyms.isEmpty())
			pseudonym = Optional.empty();
		else if (pseudonyms.size() > 1)
			// TODO throw new IllegalStateException("More than one pseudonym with same id and project");
			pseudonym = Optional.empty();
		else
			pseudonym = Optional.of(pseudonyms.get(0));
		return pseudonym;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.IItemUtil#addPseudoIdImpl(org.nrg.xdat.om.XnatSubjectdata, java.lang.String)
	 */
	@Override
	public Optional<ExtSubjectpseudonym> addPseudoIdImpl(
			XnatProjectdata project, XnatSubjectdata subject, String pseudoId) throws IllegalStateException {
		
		if (getPseudonymImpl(project.getId(), pseudoId).isPresent())
			throw new IllegalStateException("Pseudonym "+pseudoId+" with project "+project.getId()+" already exists");
		
		XFTItem item = null;
		try {
			item = XFTItem.NewItem("ext:subjectPseudonym", user);
		} catch (XFTInitException | ElementNotFoundException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
			return Optional.empty();
		}
		
		// put the new pseudonym
		ExtSubjectpseudonym newPseudonym = new ExtSubjectpseudonym(item);
		Optional<String> id = generateId("ext_subjectPseudonym", "id");
		if (!id.isPresent())
			return Optional.empty();
		newPseudonym.setId(id.get());
		newPseudonym.setPpid(pseudoId);
		newPseudonym.setProject(project.getId());
		newPseudonym.setSubject(subject.getId());
		
		PersistentWorkflowI wrk;
		try {
			wrk = PersistentWorkflowUtils
					.getOrCreateWorkflowData(null, user, newPseudonym.getItem(), 
							EventUtils.newEventInstance(
									EventUtils.CATEGORY.DATA, 
									resource.getEventType(), 
									resource.getAction(), 
									pseudoId, 
									"Inserted new pseudonym for a subject."));
			
		} catch (JustificationAbsent | ActionNameAbsent | IDAbsent e1) { // from PersistentWorkflowUtils.getOrCreateWorkflowData
			// TODO this looks like a server-side exception, so is this the proper way of handling it?
			e1.printStackTrace();
			return Optional.empty();
		}
		
		try {
			if (SaveItemHelper.authorizedSave(newPseudonym.getItem(), user, false,
					true, wrk.buildEvent())) {
				PersistentWorkflowUtils.complete(wrk, wrk.buildEvent());
				MaterializedView.DeleteByUser(user);
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
	
	/**
	 * Generates a unique identifier, esp. for use with primary keys in tables.
	 * 
	 * @param tableName eg. "xnat_subjectData"
	 * @param tableColumnName eg. "id"
	 * @return generated identifier, or null in case of a generic exception from the XNAT base
	 */
	protected static Optional<String> generateId(String tableName, String tableColumnName) {
		try {
			IDGeneratorI generator = IDGeneratorFactory
					.GetIDGenerator("org.nrg.xnat.turbine.utils.IDGenerator");
			generator.setTable(tableName);
			generator.setDigits(5);
			generator.setColumn(tableColumnName);
			return Optional.of(generator.generateIdentifier());
		} catch (Exception e) {
			return Optional.empty();
		}
	}

}
