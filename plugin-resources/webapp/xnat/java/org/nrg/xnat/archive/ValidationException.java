/**
 * Copyright 2010 Washington University
 */
package org.nrg.xnat.archive;

import java.util.Arrays;

import org.nrg.xft.utils.ValidationUtils.ValidationResults;
import org.restlet.data.Status;

/**
 * Indicates that a validation has failed.
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public class ValidationException extends ArchivingException {
	private static final long serialVersionUID = 1L;
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");
	private static final Status status = Status.SERVER_ERROR_INTERNAL;
	private final ValidationResults results;
	
	/**
	 * @param results cause of the ValidationException: must not be valid
	 */
	public ValidationException(final ValidationResults results) {
		super(status, makeMessage(results));
		if (results.isValid()) {
			throw new IllegalArgumentException("validation must have failed");
		}
		this.results = results;
	}
	
	/**
	 * Retrieves the ValidationResults underlying this exception.
	 * @return ValidationResults
	 */
	public ValidationResults getValidationResults() {
		return results;
	}
	
	private static String makeMessage(final ValidationResults results) {
		final StringBuilder sb = new StringBuilder();
		for (final Object[] result : results.getResults()) {
			sb.append(Arrays.asList(result)).append(LINE_SEPARATOR);
		}
		return sb.toString();
	}
}
