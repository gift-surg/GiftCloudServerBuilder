// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Provide some helper methods to SimpleDateFormat for testing. Never throw a
 * checked exception.
 */
public class SimpleDateFormatUtil {
	private SimpleDateFormat formatter;

	public SimpleDateFormatUtil(String pattern) {
		formatter = new SimpleDateFormat(pattern);
	}

	public static Date parse(String pattern, String source) {
		// TODO: this could be improved by keeping a cache of
		// SimpleDateFormatters based on pattern, but it would have to be
		// thread-safe
		SimpleDateFormatUtil formatter = new SimpleDateFormatUtil(pattern);
		return formatter.parse(source);
	}

	public static String format(String pattern, Date date) {
		SimpleDateFormatUtil formatter = new SimpleDateFormatUtil(pattern);
		return formatter.format(date);
	}

	public Date parse(String source) {
		try {
			return formatter.parse(source);
		} catch (ParseException e) {
			throw new ParseRuntimeException(e);
		}
	}

	public String format(Date date) {
		return formatter.format(date);
	}

	class ParseRuntimeException extends RuntimeException {
		private static final long serialVersionUID = -8950847589621013220L;

		public ParseRuntimeException(ParseException e) {
			super(e);
		}
	}
}
