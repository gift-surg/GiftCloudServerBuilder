package org.nrg.xnat.helpers.transactions;

import org.nrg.status.StatusList;

public interface PersistentStatusQueueManagerI {
	public StatusList storeStatusQueue(final String id, final StatusList sq) throws IllegalArgumentException;
	public StatusList retrieveStatusQueue(final String id) throws IllegalArgumentException;
	public StatusList deleteStatusQueue(final String id) throws IllegalArgumentException;
}
