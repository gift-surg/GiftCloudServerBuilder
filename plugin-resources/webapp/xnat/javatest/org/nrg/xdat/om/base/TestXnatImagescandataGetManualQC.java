/*
 * org.nrg.xdat.om.base.TestXnatImagescandataGetManualQC
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
import org.nrg.xdat.model.XnatQcscandataI;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatQcscandata;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestXnatImagescandataGetManualQC {
	private static final String SCAN_ID = "ABC-1234";

	private FakeXnatImagescandata scan;

	private XnatQcmanualassessordataI assessor;

	private ArrayList<XnatQcscandataI> qcList;

	@Before
	public void setUp() throws Exception {
		scan = new FakeXnatImagescandata();
		scan.id = SCAN_ID;
		scan.session = mock(XnatImagesessiondata.class);

		assessor = mock(XnatQcmanualassessordataI.class);
		when(scan.session.getManualQC()).thenReturn(assessor);

		qcList = new ArrayList<XnatQcscandataI>();
		when(assessor.getScans_scan()).thenReturn((List<XnatQcscandataI>) qcList);
	}

	@Test
	public void shouldNotReturnWhenNullAssessments() {
		when(scan.session.getManualQC()).thenReturn(null);

		assertNull(scan.getManualQC());
	}

	@Test
	public void shouldNotReturnWhenNullQCScans() {
		when(assessor.getScans_scan()).thenReturn(null);

		assertNull(scan.getManualQC());
	}

	@Test
	public void shouldNotReturnWhenEmptyQCScans() {
		when(assessor.getScans_scan()).thenReturn(new ArrayList<XnatQcscandataI>());

		assertNull(scan.getManualQC());
	}

	@Test
	public void shouldNotReturnWhenNullImageScanId() {
		addQc(null);

		assertNull(scan.getManualQC());
	}

	@Test
	public void shouldReturnQCWithSameID() {
		XnatQcscandata qc = addQc(SCAN_ID);

		assertEquals(qc, scan.getManualQC());
	}

	@Test
	public void shouldReturnQCWithSameIDWhenMultipleQCs() {
		addQc("fake-a");
		XnatQcscandata qc = addQc(SCAN_ID);
		addQc("fake-b");

		assertEquals(qc, scan.getManualQC());
	}

	@Test(expected = NullPointerException.class)
	public void shouldThrowExceptionWhenNullSession() {
		// getImageSession()'s contract indicates it never returns null
		scan.session = null;

		assertNull(scan.getManualQC());
	}

	private XnatQcscandata addQc(String id) {
		XnatQcscandata qc = mock(XnatQcscandata.class);
		when(qc.getImagescanId()).thenReturn(id);
		qcList.add(qc);
		return qc;
	}

	static class FakeXnatImagescandata extends XnatImagescandata {
		String id;

		XnatImagesessiondata session;

		@Override
		public String getId() {
			return id;
		}

		@Override
		public XnatImagesessiondata getImageSessionData() {
			return session;
		}
	}
}
