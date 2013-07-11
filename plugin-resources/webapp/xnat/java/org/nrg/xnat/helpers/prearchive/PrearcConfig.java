/*
 * org.nrg.xnat.helpers.prearchive.PrearcConfig
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.helpers.prearchive;

public final class PrearcConfig {

	private boolean reloadPrearcDatabaseOnApplicationStartup;
	
	public boolean isReloadPrearcDatabaseOnApplicationStartup() {
		return reloadPrearcDatabaseOnApplicationStartup;
	}

	public void setReloadPrearcDatabaseOnApplicationStartup(
			boolean reloadPrearcDatabaseOnApplicationStartup) {
		this.reloadPrearcDatabaseOnApplicationStartup = reloadPrearcDatabaseOnApplicationStartup;
	}
}
