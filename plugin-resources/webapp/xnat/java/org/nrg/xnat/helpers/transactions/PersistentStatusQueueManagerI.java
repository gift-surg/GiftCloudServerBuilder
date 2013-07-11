/*
 * org.nrg.xnat.helpers.transactions.PersistentStatusQueueManagerI
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.helpers.transactions;

import org.nrg.status.StatusList;

public interface PersistentStatusQueueManagerI {
	public StatusList storeStatusQueue(final String id, final StatusList sq) throws IllegalArgumentException;
	public StatusList retrieveStatusQueue(final String id) throws IllegalArgumentException;
	public StatusList deleteStatusQueue(final String id) throws IllegalArgumentException;
}
