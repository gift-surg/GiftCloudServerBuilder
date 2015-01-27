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
import org.nrg.xdat.om.XnatSubjectdata;

/**
 * Interface for commonly used functionality such as querying subjects.
 * 
 * @author Dzhoshkun Shakir (d.shakir@ucl.ac.uk)
 *
 */
public interface ResourceUtilI {
	/**
	 * Fetches subject with specified label.
	 * 
	 * @param descriptor
	 * @return null if no subject with specified label
	 * @throws IllegalAccessException
	 * @throws Exception
	 */
	public Optional<XnatSubjectdata> getSubjectByLabelOrId(String descriptor) throws IllegalAccessException, Exception;
	
	/**
	 * Fetches subject associated to provided pseudo ID.
	 * 
	 * @param pseudoId
	 * @return null if {@code pseudoId} does not exist
	 * @throws IllegalAccessException
	 * @throws Exception
	 */
	public Optional<XnatSubjectdata> getMatchingSubject(String pseudoId) throws IllegalAccessException, Exception;
	
	/**
	 * Fetches subject associated to provided pseudonym.
	 * 
	 * @param pseudonym
	 * @return null if no subject matches
	 * @throws IllegalAccessException
	 * @throws Exception
	 * 
	 * @see #getMatchingSubject(String)
	 */
	public XnatSubjectdata getMatchingSubject(ExtSubjectpseudonym pseudonym) throws IllegalAccessException, Exception;
	
	/**
	 * Fetches pseudonym as object.
	 * 
	 * @param pseudoId
	 * @return null if provided parameter not existing
	 * @throws IllegalAccessException
	 * @throws Exception
	 */
	public Optional<ExtSubjectpseudonym> getPseudonym(String pseudoId) throws IllegalAccessException, Exception;
	
	/**
	 * Adds provided pseudo ID to provided subject.
	 * 
	 * @param subject
	 * @param pseudoId
	 * @throws IllegalAccessException
	 * @throws Exception
	 */
	public void addPseudoId(XnatSubjectdata subject, String pseudoId) throws IllegalAccessException, Exception;
}
