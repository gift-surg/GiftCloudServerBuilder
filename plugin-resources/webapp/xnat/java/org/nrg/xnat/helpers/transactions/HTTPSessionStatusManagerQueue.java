package org.nrg.xnat.helpers.transactions;

import javax.servlet.http.HttpSession;

import org.nrg.status.StatusList;

public class HTTPSessionStatusManagerQueue implements PersistentStatusQueueManagerI {
	private final HttpSession session;
	
	public HTTPSessionStatusManagerQueue(final HttpSession s){
		this.session=s;
	}
	
	@Override
	public StatusList storeStatusQueue(final String id, final StatusList sq) throws IllegalArgumentException {
		this.session.setAttribute(TransactionUtils.buildTransactionID(id), sq);
		return sq;
	}

	@SuppressWarnings("unchecked")
	@Override
	public StatusList retrieveStatusQueue(final String id) throws IllegalArgumentException {
		return (StatusList) this.session.getAttribute(TransactionUtils.buildTransactionID(id));
	}

	@Override
	public StatusList deleteStatusQueue(final String id) throws IllegalArgumentException {
		final StatusList sq=this.retrieveStatusQueue(TransactionUtils.buildTransactionID(id));
		this.session.removeAttribute(id);
		return sq;
	}

}
