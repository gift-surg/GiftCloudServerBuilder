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
package org.nrg.xnat.security;

import org.nrg.xdat.om.base.BaseXdatUser;
import org.nrg.xdat.om.base.auto.AutoXdatUser;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.XFTItem;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.nrg.xnat.restlet.util.SecureUtilFactory;

/**
 * Performs security checks. Always use the {@link SecureUtilFactory#getSecurityUtilInstance()} 
 * method to get an instance of this interface.
 * 
 * @author Dzhoshkun Shakir (d.shakir@ucl.ac.uk)
 *
 */
public interface ISecurityUtil {
	/**
	 * 
	 * @param item
	 * @return
	 */
	public boolean canRead(final XFTItem item);
	
	/**
	 * 
	 * @param item
	 * @return
	 */
	public boolean canEdit(final XFTItem item);
	
	/**
	 * This setter method must be called right after creating an object, in conjunction with {@link #setResource(SecureResource)}.
	 * 
	 * @param user should ideally be a higher-level class, such as {@link BaseXdatUser} or {@link AutoXdatUser}, but provided for compatibility with current XNAT version (1.6).
	 * @throws IllegalArgumentException when parameter null
	 */
	public void setUser(XDATUser user) throws IllegalArgumentException;
	
	/**
	 * This setter method must be called right after creating an object, in conjunction with {@link #setUser(XDATUser)}.
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
}
