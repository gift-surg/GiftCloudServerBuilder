/*
 * org.nrg.xnat.restlet.util.SimpleDateFormatUtil
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.restlet.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
