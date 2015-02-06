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
	public static ISecurityUtil getSecurityUtilInstance() {
		return new DefaultSecurityUtil();
	}

	/**
	 * 
	 * @param securityUtil
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static ISecureItemUtil getSecureItemUtilInstance(
			ISecurityUtil securityUtil) throws IllegalArgumentException {
		if (securityUtil == null)
			throw new IllegalArgumentException("No secure item util without security util!");
		else if (securityUtil.getUser() == null || securityUtil.getResource() == null)
			throw new IllegalArgumentException("Security util not initialised properly");
		else {
			ISecureItemUtil secureItemUtil = new SecureItemUtil();
			secureItemUtil.setSecurityUtil(securityUtil);
			return secureItemUtil;
		}
	}
}
