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
import org.nrg.xdat.exceptions.IllegalAccessException;
import org.nrg.xnat.security.ISecurityUtil;

/**
 * Interface for commonly used, security-involving functionality pertaining to DB items. Always
 * use the {@link SecureUtilFactory#getSecureItemUtilInstance(IItemUtil, ISecurityUtil)} method
 * to get an instance of this interface.
 * 
 * @author Dzhoshkun Shakir (d.shakir@ucl.ac.uk)
 *
 */
public interface ISecureItemUtil {
	/**
	 * This method must be called right after an object is created,
	 * in conjunction with {@link #setSecurityUtil(ISecurityUtil)}.
	 * 
	 * @param itemUtil
	 */
	public void setItemUtil(IItemUtil itemUtil);
	
	/**
	 * This method must be called right after an object is created,
	 * in conjunction with {@link #setItemUtil(IItemUtil)}.
	 * 
	 * @param securityUtil
	 */
	public void setSecurityUtil(ISecurityUtil securityUtil);
	
	/**
	 * Fetches project with specified label.
	 * 
	 * @param descriptor
	 * @return null if no project with specified label
	 * @throws IllegalAccessException
	 */
	public Optional<XnatProjectdata> getProjectByLabelOrId(String descriptor) throws IllegalAccessException;
	
	/**
	 * Fetches subject with specified label within specified project.
	 * 
	 * @param projectId
	 * @param descriptor
	 * @return null if no subject with specified label
	 * @throws IllegalAccessException
	 */
	public Optional<XnatSubjectdata> getSubjectByLabelOrId(String projectId, String descriptor) throws IllegalAccessException;
	
	/**
	 * Fetches subject associated to provided pseudo ID.
	 * 
	 * @param projectId
	 * @param pseudoId
	 * @return null if {@code pseudoId} does not exist
	 * @throws IllegalAccessException
	 */
	public Optional<XnatSubjectdata> getMatchingSubject(String projectId, String pseudoId) throws IllegalAccessException;
	
	/**
	 * Fetches pseudonym as object.
	 * 
	 * @param projectId
	 * @param pseudoId
	 * @return null if provided parameter not existing
	 * @throws IllegalAccessException
	 */
	public Optional<ExtSubjectpseudonym> getPseudonym(String projectId, String pseudoId) throws IllegalAccessException;
	
	/**
	 * Adds provided pseudo ID to provided subject.
	 * 
	 * @param project
	 * @param subject
	 * @param pseudoId
	 * @return the newly created pseudonym
	 * @throws IllegalAccessException
	 * @throws IllegalStateException if pseudonym already exists
	 */
	public Optional<ExtSubjectpseudonym> addPseudoId(XnatProjectdata project, XnatSubjectdata subject, String pseudoId) throws IllegalAccessException, IllegalStateException;
	
	/**
	 * Fetches matching experiment data as object.
	 * 
	 * @param projectId
	 * @param subjectId
	 * @param uid
	 * @return null if provided parameter not existing
	 * @throws IllegalAccessException
	 */
	public Optional<XnatImagesessiondata> getMatchingExperiment(String projectId, String subjectId, String uid) throws IllegalAccessException;
}
