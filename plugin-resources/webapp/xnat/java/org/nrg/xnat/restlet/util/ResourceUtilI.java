/**
 * 
 */
package org.nrg.xnat.restlet.util;

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
	 * @param label
	 * @return null if no subject with specified label
	 * @throws IllegalAccessException
	 * @throws Exception
	 */
	public XnatSubjectdata getSubject(String label) throws IllegalAccessException, Exception;
	
	/**
	 * Fetches subject associated to provided pseudo ID.
	 * 
	 * @param pseudoId
	 * @return null if no subject matches
	 * @throws IllegalAccessException
	 * @throws Exception
	 */
	public XnatSubjectdata getMatchingSubject(String pseudoId) throws IllegalAccessException, Exception;
	
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
	public ExtSubjectpseudonym getPseudonym(String pseudoId) throws IllegalAccessException, Exception;
	
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
