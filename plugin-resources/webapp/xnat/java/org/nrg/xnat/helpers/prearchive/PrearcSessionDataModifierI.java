/*
 * org.nrg.xnat.helpers.prearchive.PrearcSessionDataModifierI
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.helpers.prearchive;

public interface PrearcSessionDataModifierI {

	public abstract void setStatus(SessionData sd,
			final PrearcUtils.PrearcStatus status);

}