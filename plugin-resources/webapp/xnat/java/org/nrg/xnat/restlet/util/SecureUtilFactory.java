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

import org.nrg.xnat.security.DefaultSecurityUtil;
import org.nrg.xnat.security.ISecurityUtil;
import org.nrg.xnat.restlet.util.IItemUtil;

/**
 * 
 * 
 * @author Dzhoshkun Shakir (d.shakir@ucl.ac.uk)
 *
 */
public final class SecureUtilFactory {
	/**
	 * 
	 * @return
	 */
	public static IItemUtil getItemUtilInstance() {
		return new DefaultItemUtil();
	}
	
	/**
	 * 
	 * @return
	 */
	public static ISecurityUtil getSecurityUtilInstance() {
		return new DefaultSecurityUtil();
	}

	/**
	 * 
	 * @param itemUtil
	 * @param securityUtil
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static ISecureItemUtil getSecureItemUtilInstance(
			IItemUtil itemUtil, ISecurityUtil securityUtil) throws IllegalArgumentException {
		if (itemUtil == null)
			throw new IllegalArgumentException("No secure item util without security util!");
		else if (securityUtil == null)
			throw new IllegalArgumentException("No secure item util without security util!");
		else {
			try {
				itemUtil.getUser();
				itemUtil.getResource();
				securityUtil.getUser();
				securityUtil.getResource();
				if (itemUtil.getUser() != securityUtil.getUser() || itemUtil.getResource() != securityUtil.getResource())
					throw new IllegalArgumentException("Item util not matching security util");
			}
			catch (IllegalStateException e) { // something not set properly, i.e. getter called before setter
				throw new IllegalArgumentException(e);
			}
			ISecureItemUtil secureItemUtil = new SecureItemUtil();
			secureItemUtil.setItemUtil(itemUtil);
			secureItemUtil.setSecurityUtil(securityUtil);
			return secureItemUtil;
		}
	}
}
