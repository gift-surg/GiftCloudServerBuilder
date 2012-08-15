package org.nrg.xnat.helpers.prearchive;

/**
 * 
 * @author ehaas01
 * Meant for properties that are not backed by the DB.
 * See https://issues.xnat.org/browse/XNAT-1150 for background.
 */
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
