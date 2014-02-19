/*
 * org.nrg.xdat.om.base.TestBaseXnatSubjectdataGetAge
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xdat.om.base;

import org.junit.Before;
import org.junit.Test;
import org.nrg.xnat.restlet.util.SimpleDateFormatUtil;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class TestBaseXnatSubjectdataGetAge {
	private static final SimpleDateFormatUtil DATE_FORMAT = new SimpleDateFormatUtil(
			"yyyy-MM-dd");
	private FakeXnatSubjectdata subject;
	private Date experimentDate;

	@Before
	public void setUp() throws Exception {
		subject = new FakeXnatSubjectdata();
		experimentDate = DATE_FORMAT.parse("2009-12-09");
	}

	@Test
	public void shouldDefaultWhenNoExperimentDate() {
		subject.dob = DATE_FORMAT.parse("2007-12-09");

		assertEquals("--", subject.getAge(null));
	}

	@Test
	public void shouldDefaultWhenNoAgeOrBirthDateOrBirthYear() {
		assertEquals("--", subject.getAge(experimentDate));
	}

	@Test
	public void shouldUseBirthYearWhenNoBirthDate() {
		subject.yob = 2006;

		assertEquals("3.00", subject.getAge(experimentDate));
	}

	@Test
	public void shouldUseAgeWhenNoBirthYear() {
		subject.age = 56;

		assertEquals("56", subject.getAge(experimentDate));
	}

	@Test
	public void shouldUseAgeWhenNoBirthDateOrBirthYear() {
		subject.age = 56;

		assertEquals("56", subject.getAge(experimentDate));
	}

	@Test
	public void shouldHandleLeapYearsExactly2Years() {
		subject.dob = DATE_FORMAT.parse("2007-12-09");

		assertEquals("2.00", subject.getAge(experimentDate));
	}

	@Test
	public void shouldHandleLeapYearsOver2Years() {
		subject.dob = DATE_FORMAT.parse("2007-12-8");

		assertEquals("2.00", subject.getAge(experimentDate));
	}

	@Test
	public void shouldHandleLeapYearsAlmost2Years() {
		subject.dob = DATE_FORMAT.parse("2007-12-10");

		assertEquals("1.00", subject.getAge(experimentDate));
	}

	static class FakeXnatSubjectdata extends BaseXnatSubjectdata {
		Exception throwException;
		Date dob;
		Integer age;
		Integer yob;

		@Override
		public Date getDOB() {
			return dob;
		}

		@Override
		public Integer getAge() {
			return age;
		}

		@Override
		public Integer getYOB() {
			return yob;
		}
	}
}
