/*
 * org.nrg.xnat.helpers.prearchive.SessionDataModifierI
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
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
