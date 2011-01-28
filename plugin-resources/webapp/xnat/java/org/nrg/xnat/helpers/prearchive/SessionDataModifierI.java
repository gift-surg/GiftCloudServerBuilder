package org.nrg.xnat.helpers.prearchive;
/**
 * Modify the session data in some permanent store
 * @author aditya
 *
 */
public interface SessionDataModifierI extends PrearcSessionDataModifierI {
	void move(SessionData s , final String newProj) throws java.io.SyncFailedException;
	void delete(SessionData sd) throws java.io.SyncFailedException;
}
