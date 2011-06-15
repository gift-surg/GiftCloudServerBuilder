package org.nrg.xnat.helpers.prearchive;

import org.nrg.xnat.helpers.prearchive.PrearcDatabase.SyncFailedException;

/**
 * Modify the session data in some permanent store
 * @author aditya
 *
 */
public interface SessionDataModifierI extends PrearcSessionDataModifierI {
	void move(SessionData s , final String newProj) throws SyncFailedException;
	void delete(SessionData sd) throws SyncFailedException;
}
