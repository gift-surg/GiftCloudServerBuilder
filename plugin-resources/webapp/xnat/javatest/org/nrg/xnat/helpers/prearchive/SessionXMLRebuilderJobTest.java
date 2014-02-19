/*
 * org.nrg.xnat.helpers.prearchive.SessionXMLRebuilderJobTest
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.helpers.prearchive;

import junit.framework.Assert;
import org.junit.Test;

import java.util.Calendar;

public class SessionXMLRebuilderJobTest {
	@Test
	public final void testDiffInMinutes() {
		long now = Calendar.getInstance().getTimeInMillis();
		long then = now - (1000 * 60 * 6);
		double minutes = SessionXMLRebuilderJob.diffInMinutes(then, now);
		Assert.assertEquals(6.0, minutes);
	}
}
