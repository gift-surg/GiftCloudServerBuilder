package org.nrg.xnat.helpers.transactions;

import org.apache.commons.lang.StringUtils;

public class TransactionUtils {
	
	private static final String TRANSACTION_PREFIX = "_TR";

	public static String buildTransactionID(final String s) throws IllegalArgumentException{
		if(StringUtils.isEmpty(s))throw new IllegalArgumentException();
		
		if(!s.startsWith(TRANSACTION_PREFIX))
			return TRANSACTION_PREFIX+s;
		else
			return s;
	}
}
