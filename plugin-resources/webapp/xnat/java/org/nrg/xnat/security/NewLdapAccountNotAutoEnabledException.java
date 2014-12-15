/*
 * org.nrg.xnat.security.NewLdapAccountNotAutoEnabledException
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.security;

import org.nrg.xdat.entities.XDATUserDetails;
import org.springframework.security.core.AuthenticationException;

public class NewLdapAccountNotAutoEnabledException extends
		AuthenticationException {
	private static final long serialVersionUID = 1L;

	private XDATUserDetails userDetails;

	public NewLdapAccountNotAutoEnabledException(String msg) {
		super(msg);
	}

	public NewLdapAccountNotAutoEnabledException(String msg,
			XDATUserDetails userDetails) {
		super(msg);
		setUserDetails(userDetails);
	}

	public XDATUserDetails getUserDetails() {
		return userDetails;
	}

	public void setUserDetails(XDATUserDetails userDetails) {
		this.userDetails = userDetails;
	}
}
