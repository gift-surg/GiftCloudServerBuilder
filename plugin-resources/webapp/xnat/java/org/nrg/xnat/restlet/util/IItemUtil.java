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

import org.nrg.xdat.om.ExtSubjectpseudonym;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.om.base.BaseXdatUser;
import org.nrg.xdat.om.base.auto.AutoXdatUser;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.restlet.resources.SecureResource;

/**
 * Provides backend for some of the functionality declared in {@link ISecureItemUtil},
 * mostly due to the heavy use of static methods in XNAT. While being very important to
 * the testability (mockability) of those static methods, this interface is intended to
 * be used <em>only</em> behind the {@link ISecureItemUtil} interface, and <em>never</em>
 * publicly.
 * 
 * @author Dzhoshkun Shakir (d.shakir@ucl.ac.uk)
 * 
 * @see ISecureItemUtilTest (where this interface comes into play within testing)
 *
 */
public interface IItemUtil {
	/**
	 * This setter method must be called right after creating an object,
	 * in conjunction with {@link #setResource(SecureResource)}.
	 * 
	 * @param user should ideally be a higher-level class, such as 
	 * {@link BaseXdatUser} or {@link AutoXdatUser}, but provided for 
	 * compatibility with current XNAT version (1.6).
	 * @throws IllegalArgumentException when parameter null
	 */
	public void setUser(XDATUser user) throws IllegalArgumentException;
	
	/**
	 * This setter method must be called right after creating an object, 
	 * in conjunction with {@link #setUser(XDATUser)}.
	 * 
	 * @param resource
	 * @throws IllegalArgumentException when parameter null
	 */
	public void setResource(SecureResource resource) throws IllegalArgumentException;
	
	/**
	 * 
	 * @return
	 * @throws IllegalStateException if called before setter.
	 * 
	 * @see #setUser(XDATUser)
	 */
	public XDATUser getUser() throws IllegalStateException;
	
	/**
	 * 
	 * @return
	 * @throws IllegalStateException if called before setter.
	 * 
	 * @see #setResource(SecureResource)
	 */
	public SecureResource getResource() throws IllegalStateException;

	/**
	 * 
	 * @param descriptor
	 * @return
	 * 
	 * @see ISecureItemUtil#getProjectByLabelOrId(String)
	 */
	public Optional<XnatProjectdata> getProjectByLabelOrIdImpl(String descriptor);
	
	/**
	 * 
	 * @param projectId
	 * @param descriptor
	 * @return
	 * 
	 * @see ISecureItemUtil#getSubjectByLabelOrId(String, String)
	 */
	public Optional<XnatSubjectdata> getSubjectByLabelOrIdImpl(String projectId, String descriptor);

	/**
	 * 
	 * @param projectId
	 * @param pseudoId
	 * @return
	 * 
	 * @see ISecureItemUtil#getMatchingSubject(String)
	 */
	public Optional<XnatSubjectdata> getMatchingSubjectImpl(String projectId, String pseudoId);

	/**
	 * 
	 * @param projectId
	 * @param pseudoId
	 * @return
	 * 
	 * @see ISecureItemUtil#getPseudonym(String, String)
	 */
	public Optional<ExtSubjectpseudonym> getPseudonymImpl(String projectId, String pseudoId);

	/**
	 * 
	 * @param project
	 * @param subject
	 * @param pseudoId
	 * @return null if pseudonym could not be added
	 * @throws IllegalStateException if pseudonym already exists
	 * 
	 * @see ISecureItemUtil#addPseudoId(XnatProjectdata, XnatSubjectdata, String)
	 */
	public Optional<ExtSubjectpseudonym> addPseudoIdImpl(XnatProjectdata project, XnatSubjectdata subject, String pseudoId) throws IllegalStateException;
	
	/**
	 * 
	 * @param projectId
	 * @param subjectId
	 * @param uid
	 * @return
	 * 
	 * @see ISecureItemUtil#getMatchingExperiment(String, String, String)
	 */
	public Optional<XnatImagesessiondata> getMatchingExperimentImpl(String projectId, String subjectId, String uid);
}
