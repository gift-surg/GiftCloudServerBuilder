/*=============================================================================

  GIFT-Cloud: A data storage and collaboration platform

  Copyright (c) University College London (UCL). All rights reserved.

  Parts of this software are derived from XNAT
    http://www.xnat.org
    Copyright (c) 2014, Washington University School of Medicine
    All Rights Reserved
    Released under the Simplified BSD.

  This software is distributed WITHOUT ANY WARRANTY; without even
  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
  PURPOSE.

  See LICENSE.txt in the top level directory for details.

=============================================================================*/
package org.nrg.xnat.restlet.util;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.fail;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.nrg.xdat.exceptions.IllegalAccessException;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.XFTItem;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.nrg.xnat.security.ISecurityUtil;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class ISecureItemUtilTest {
	ISecurityUtil mockSecurityUtil;
	XDATUser mockUser;
	SecureResource mockResource;
	ISecureItemUtil secureItemUtil;
	String pseudoId;
	
	@BeforeTest
	public void populate() {
		mockSecurityUtil = mock(ISecurityUtil.class);
		mockUser = mock(XDATUser.class);
		mockResource = mock(SecureResource.class);
		when(mockSecurityUtil.getUser()).thenAnswer(new Answer<XDATUser>() {
			@Override
			public XDATUser answer(InvocationOnMock invocation)
					throws Throwable {
				return mockUser;
			}
		});
		when(mockSecurityUtil.getResource()).thenAnswer(new Answer<SecureResource>() {
			@Override
			public SecureResource answer(InvocationOnMock invocation)
					throws Throwable {
				return mockResource;
			}
		});
		secureItemUtil = SecureUtilFactory.getSecureItemUtilInstance(mockSecurityUtil);
		pseudoId = "dummy";
	}

	@Test
	public void addPseudoId() {
		XnatSubjectdata mockSubject = mock(XnatSubjectdata.class);
		String pseudoId = "dummy";
		when(mockSecurityUtil.canEdit(isA(XFTItem.class))).thenAnswer(new Answer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				return true;
			}
		});
		when(mockSubject.getItem()).thenAnswer(new Answer<XFTItem>() {
			@Override
			public XFTItem answer(InvocationOnMock invocation) throws Throwable {
				return new XFTItem();
			}
		});
		// TODO mocking the static stuff comes in here
		try {
			secureItemUtil.addPseudoId(mockSubject, pseudoId);
		} catch (IllegalAccessException e) {
			fail("Illegal access thrown, whereas everything is perfectly legal");
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		verify(mockSecurityUtil, times(1)).canEdit(mockSubject.getItem());
	}
	
	@Test(expectedExceptions = { IllegalAccessException.class })
	public void addPseudoIdWithIllegalAccess() throws Exception {
		XnatSubjectdata mockSubject = mock(XnatSubjectdata.class);
		String pseudoId = "dummy";
		when(mockSecurityUtil.canEdit(isA(XFTItem.class))).thenAnswer(new Answer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				return false;
			}
		});
		when(mockSubject.getItem()).thenAnswer(new Answer<XFTItem>() {
			@Override
			public XFTItem answer(InvocationOnMock invocation) throws Throwable {
				return new XFTItem();
			}
		});
		// TODO mocking the static stuff comes in here
		try {
			secureItemUtil.addPseudoId(mockSubject, pseudoId);
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		verify(mockSecurityUtil, times(1)).canEdit(mockSubject.getItem());
	}

	@Test
	public void getMatchingSubject() {
		String pseudoId = "dummy";
		try {
			secureItemUtil.getMatchingSubject(pseudoId);
			verify(secureItemUtil, times(1)).getPseudonym(pseudoId);
		} catch (Throwable e) {
			// no need to do anything, just print the stack
			e.printStackTrace();
		}
	}

	// TODO public void getPseudonym()

	// TODO public void getSubjectByLabelOrId()
}
