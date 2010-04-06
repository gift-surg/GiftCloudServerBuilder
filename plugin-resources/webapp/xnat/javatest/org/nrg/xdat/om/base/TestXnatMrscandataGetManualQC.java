// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xdat.om.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatMrqcscandata;
import org.nrg.xdat.om.XnatMrscandata;
import org.nrg.xdat.om.XnatQcmanualassessordataI;
import org.nrg.xdat.om.XnatQcscandata;

public class TestXnatMrscandataGetManualQC {
	private static final String SCAN_ID = "ABC-1234";

	private FakeXnatMrscandata scan;

	private XnatQcmanualassessordataI assessor;

	private ArrayList<XnatQcscandata> qcList;

	@Before
	public void setUp() throws Exception {
		scan = new FakeXnatMrscandata();
		scan.id = SCAN_ID;
		scan.session = mock(XnatImagesessiondata.class);

		assessor = mock(XnatQcmanualassessordataI.class);
		when(scan.session.getManualQC()).thenReturn(assessor);

		qcList = new ArrayList<XnatQcscandata>();
		when(assessor.getScans_scan()).thenReturn(qcList);
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
		when(assessor.getScans_scan()).thenReturn(new ArrayList<XnatQcscandata>());

		assertNull(scan.getManualQC());
	}

	@Test
	public void shouldNotReturnWhenNullImageScanId() {
		addQc(null);

		assertNull(scan.getManualQC());
	}

	@Test
	public void shouldReturnQCWithSameID() {
		XnatMrqcscandata qc = addQc(SCAN_ID);

		assertEquals(qc, scan.getManualQC());
	}

	@Test
	public void shouldReturnQCWithSameIDWhenMultipleQCs() {
		addQc("fake-a");
		XnatMrqcscandata qc = addQc(SCAN_ID);
		addQc("fake-b");

		assertEquals(qc, scan.getManualQC());
	}

	@Test(expected = ClassCastException.class)
	public void shouldNotReturnWhenNotMrScan() {
		XnatQcscandata qc = mock(XnatQcscandata.class);
		when(qc.getImagescanId()).thenReturn(SCAN_ID);
		qcList.add(qc);

		scan.getManualQC();
	}

	@Test(expected = NullPointerException.class)
	public void shouldThrowExceptionWhenNullSession() {
		// getImageSession()'s contract indicates it never returns null
		scan.session = null;

		scan.getManualQC();
	}

	private XnatMrqcscandata addQc(String id) {
		XnatMrqcscandata qc = mock(XnatMrqcscandata.class);
		when(qc.getImagescanId()).thenReturn(id);
		qcList.add(qc);
		return qc;
	}

	static class FakeXnatMrscandata extends XnatMrscandata {
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
