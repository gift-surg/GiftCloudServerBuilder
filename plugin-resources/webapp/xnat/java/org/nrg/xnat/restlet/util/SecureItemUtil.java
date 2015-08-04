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

import java.util.Optional;

import org.nrg.xdat.exceptions.IllegalAccessException;
import org.nrg.xdat.om.ExtSubjectpseudonym;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xft.XFTItem;
import org.nrg.xnat.security.ISecurityUtil;

/**
 * @author Dzhoshkun Shakir (d.shakir@ucl.ac.uk)
 *
 */
public final class SecureItemUtil implements ISecureItemUtil {
	private ISecurityUtil securityUtil;
	private IItemUtil itemUtil;
	
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
	 * @see org.nrg.xnat.restlet.util.ISecureItemUtil#setItemUtil(org.nrg.xnat.restlet.util.IItemUtil)
	 */
	@Override
	public void setItemUtil(IItemUtil itemUtil) {
		this.itemUtil = itemUtil;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.ISecureItemUtil#setSecurityUtil(org.nrg.xnat.security.ISecurityUtil)
	 */
	@Override
	public void setSecurityUtil(ISecurityUtil securityUtil) {
		this.securityUtil = securityUtil;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.ISecureItemUtil#getProjectByLabelOrId(java.lang.String)
	 */
	@Override
	public Optional<XnatProjectdata> getProjectByLabelOrId(String descriptor)
			throws IllegalAccessException {
		Optional<XnatProjectdata> project = itemUtil.getProjectByLabelOrIdImpl(descriptor);
		if (project.isPresent())
			checkCanReadAndThrow(project.get().getItem());
		return project;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.ResourceUtilI#getSubject(java.lang.String)
	 */
	@Override
	public Optional<XnatSubjectdata> getSubjectByLabelOrId(String projectId, String descriptor) throws IllegalAccessException {
		Optional<XnatSubjectdata> subject = itemUtil.getSubjectByLabelOrIdImpl(projectId, descriptor);
		if (subject.isPresent())
			checkCanReadAndThrow(subject.get().getItem());
		return subject;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.ResourceUtilI#getMatchingSubject(java.lang.String)
	 */
	@Override
	public Optional<XnatSubjectdata> getMatchingSubject(String projectId, String pseudoId) throws IllegalAccessException {
		Optional<ExtSubjectpseudonym> pseudonym = itemUtil.getPseudonymImpl(projectId, pseudoId);
		if (!pseudonym.isPresent())
			return Optional.empty();
		else {
			checkCanReadAndThrow(pseudonym.get().getItem());
			return itemUtil.getSubjectByLabelOrIdImpl(projectId, pseudonym.get().getSubject());
		}
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.ResourceUtilI#getPseudonym(java.lang.String)
	 */
	@Override
	public Optional<ExtSubjectpseudonym> getPseudonym(String projectId, String pseudoId) throws IllegalAccessException {
		Optional<ExtSubjectpseudonym> pseudonym = itemUtil.getPseudonymImpl(projectId, pseudoId);
		if (pseudonym.isPresent())
			checkCanReadAndThrow(pseudonym.get().getItem());
		return pseudonym;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.ResourceUtilI#addPseudonym(org.nrg.xdat.om.XnatSubjectdata, org.nrg.xdat.om.ExtSubjectpseudonym)
	 */
	@Override
	public Optional<ExtSubjectpseudonym> addPseudoId(XnatProjectdata project, XnatSubjectdata subject,
			String pseudoId) throws IllegalAccessException, IllegalStateException {
		checkCanReadAndThrow(project.getItem());
		checkCanReadAndThrow(subject.getItem());
		return itemUtil.addPseudoIdImpl(project, subject, pseudoId);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.ISecureItemUtil#getMatchingExperiment(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public Optional<XnatImagesessiondata> getMatchingExperiment(
			String projectId, String subjectId, String uid)
			throws IllegalAccessException {
		Optional<XnatImagesessiondata> experiment = itemUtil.getMatchingExperimentImpl(projectId, subjectId, uid);
		if (experiment.isPresent())
			checkCanReadAndThrow(experiment.get().getItem());
		return experiment;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.ISecureItemUtil#getMatchingScan(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public Optional<XnatImagescandata> getMatchingScan(String projectId,
			String subjectId, String exptId, String uid)
			throws IllegalAccessException {
		Optional<XnatImagescandata> scan = itemUtil.getMatchingScanImpl(projectId, subjectId, exptId, uid);
		if (scan.isPresent())
			checkCanReadAndThrow(scan.get().getItem());
		return scan;
	}
}
