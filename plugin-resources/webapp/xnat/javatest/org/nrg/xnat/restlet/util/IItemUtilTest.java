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

import org.mockito.Mockito;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * This class tests only the functionality pertaining to setters and
 * getters. The actual functionality related to XNAT is assumed to
 * work.
 * 
 * @author Dzhoshkun Shakir (d.shakir@ucl.ac.uk)
 *
 */
public class IItemUtilTest {
	IItemUtil itemUtil;
	
	@BeforeMethod
	public void populate() {
		itemUtil = SecureUtilFactory.getItemUtilInstance();
	}
	
	@Test
	public void setGetUser() throws IllegalStateException {
		XDATUser user = new XDATUser();
		itemUtil.setUser(user);
		assert itemUtil.getUser() == user;
	}
	
	@Test( expectedExceptions = { IllegalStateException.class } )
	public void getUserAtIllegalState() throws Exception {
		itemUtil.getUser();
	}
	
	@Test
	public void setGetResource() throws IllegalStateException {
		SecureResource resource = Mockito.mock(SecureResource.class);
		itemUtil.setResource(resource);
		assert itemUtil.getResource() == resource;
	}
	
	@Test( expectedExceptions = { IllegalStateException.class } )
	public void getResourceAtIllegalState() throws Exception {
		itemUtil.getResource();
	}
}
