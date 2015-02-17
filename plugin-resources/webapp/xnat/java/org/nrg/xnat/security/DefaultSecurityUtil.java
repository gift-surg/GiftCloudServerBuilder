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

import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.XFTItem;
import org.nrg.xnat.restlet.resources.SecureResource;

/**
 * 
 * 
 * @author Dzhoshkun Shakir (d.shakir@ucl.ac.uk)
 *
 */
public class DefaultSecurityUtil implements ISecurityUtil {
	XDATUser user = null;
	SecureResource resource = null;

	/* (non-Javadoc)
	 * @see org.nrg.xnat.security.ISecurityUtil#canRead(org.nrg.xft.XFTItem)
	 */
	@Override
	public boolean canRead(XFTItem item) {
		try {
			return user.canRead(item);
		} catch (Exception e) {
			// TODO - this is only a hack to circumvent an unspecific exception type
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.security.ISecurityUtil#canEdit(org.nrg.xft.XFTItem)
	 */
	@Override
	public boolean canEdit(XFTItem item) {
		try {
			return user.canEdit(item);
		} catch (Exception e) {
			// TODO see canRead() above
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.security.ISecurityUtil#setUser(org.nrg.xdat.security.XDATUser)
	 */
	@Override
	public void setUser(XDATUser user) throws IllegalArgumentException {
		if (user==null)
			throw new IllegalArgumentException("Provided user is null");
		else
			this.user = user;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.security.ISecurityUtil#setResource(org.nrg.xnat.restlet.resources.SecureResource)
	 */
	@Override
	public void setResource(SecureResource resource) throws IllegalArgumentException {
		if (resource==null)
			throw new IllegalArgumentException("Provided resource is null");
		else
			this.resource = resource;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.security.ISecurityUtil#getUser()
	 */
	@Override
	public XDATUser getUser() throws IllegalStateException {
		if (user==null)
			throw new IllegalStateException("Getter called before setter");
		else
			return user;
	}

	/* (non-Javadoc)
	 * @see org.nrg.xnat.security.ISecurityUtil#getResource()
	 */
	@Override
	public SecureResource getResource() throws IllegalStateException {
		if (resource==null)
			throw new IllegalStateException("Getter called before setter");
		else
			return resource;
	}

}
