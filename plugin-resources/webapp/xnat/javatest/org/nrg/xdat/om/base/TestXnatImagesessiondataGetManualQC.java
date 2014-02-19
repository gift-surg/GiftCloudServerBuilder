/*
 * org.nrg.xdat.om.base.TestXnatImagesessiondataGetManualQC
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
import org.nrg.xdat.model.XnatQcmanualassessordataI;
import org.nrg.xdat.om.XnatImageassessordata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatQcmanualassessordata;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TestXnatImagesessiondataGetManualQC {
	private FakeXnatImagesessiondata sessionData;

	@Before
	public void setUp() throws Exception {
		sessionData = new FakeXnatImagesessiondata();
	}

	@Test
	public void shouldNotReturnManualQCWhenNullAssessorList() {
		sessionData.retVal = null;
		assertNull(sessionData.getManualQC());
	}

	@Test
	public void shouldNotReturnManualQCWhenEmptyAssessorList() {
		sessionData.retVal = list();
		assertNull(sessionData.getManualQC());
	}

	@Test
	public void shouldReturnManualQCWhenSingleAssessor() {
		XnatImageassessordata assessor = new XnatQcmanualassessordata();
		sessionData.retVal = list();
		sessionData.retVal.add(assessor);
		assertEquals(assessor, sessionData.getManualQC());
	}

	@Test
	public void shouldReturnFirstManualQCWhenMultipleAssessors() throws Exception {
		sessionData.retVal = list();
		sessionData.retVal.add(new XnatQcmanualassessordata());
		sessionData.retVal.add(new XnatQcmanualassessordata());
		sessionData.retVal.add(new XnatQcmanualassessordata());

		XnatQcmanualassessordataI result = sessionData.getManualQC();
		assertEquals(sessionData.retVal.get(2), result);
		assert (sessionData.retVal.get(1) != result);
		assert (sessionData.retVal.get(0) != result);
	}

	@Test
	public void shouldUseProperElementName() {
		sessionData.getManualQC();
		assertEquals("xnat:qcManualAssessorData", sessionData.elementName);
	}

	private ArrayList<XnatImageassessordata> list() {
		return new ArrayList<XnatImageassessordata>();
	}

	static class FakeXnatImagesessiondata extends XnatImagesessiondata {
		ArrayList<XnatImageassessordata> retVal;

		String elementName;

		@Override
		public ArrayList<XnatImageassessordata> getMinimalLoadAssessors(String elementName) {
			this.elementName = elementName;
			return retVal;
		}
	}
}
