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

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.nrg.xdat.exceptions.IllegalAccessException;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.XFTItem;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.nrg.xnat.security.ISecurityUtil;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;

//TODO - commented out because of the PowerMock initializer problem, see https://code.google.com/p/powermock/issues/detail?id=414
//@PrepareForTest(AutoExtSubjectpseudonym.class)
public class ISecureItemUtilTest {
	ISecurityUtil mockSecurityUtil;
	XDATUser mockUser;
	SecureResource mockResource;
	ISecureItemUtil secureItemUtil;
	String pseudoId;
	XnatSubjectdata mockSubject;
	XFTItem xftItem;
	
	@BeforeClass
	public final void populate() { //TODO - user of "final" because of the PowerMock initializer problem, see https://code.google.com/p/powermock/issues/detail?id=414
		mockSecurityUtil = Mockito.mock(ISecurityUtil.class);
		mockUser = Mockito.mock(XDATUser.class);
		mockResource = Mockito.mock(SecureResource.class);
		Mockito.when(mockSecurityUtil.getUser()).thenAnswer(new Answer<XDATUser>() {
			@Override
			public XDATUser answer(InvocationOnMock invocation)
					throws Throwable {
				return mockUser;
			}
		});
		Mockito.when(mockSecurityUtil.getResource()).thenAnswer(new Answer<SecureResource>() {
			@Override
			public SecureResource answer(InvocationOnMock invocation)
					throws Throwable {
				return mockResource;
			}
		});
		
		secureItemUtil = SecureUtilFactory.getSecureItemUtilInstance(mockSecurityUtil);
		mockSubject = Mockito.mock(XnatSubjectdata.class);
		xftItem = new XFTItem();
		Mockito.when(mockSubject.getItem()).thenAnswer(new Answer<XFTItem>() {
			@Override
			public XFTItem answer(InvocationOnMock invocation) throws Throwable {
				return xftItem;
			}
		});
		pseudoId = "dummy";
	}
	
	@BeforeGroups( groups = { "affirmative" } )
	public void configureAffirmative() {
		Mockito.when(mockSecurityUtil.canEdit(xftItem)).thenAnswer(new Answer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				return true;
			}
		});
	}
	
	@BeforeGroups( groups = { "exception" } )
	public void configureException() {
		Mockito.when(mockSecurityUtil.canEdit(xftItem)).thenAnswer(new Answer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				return false;
			}
		});
	}

	@Test( groups = { "affirmative" } )
	public void addPseudoId() throws IllegalAccessException {
		try {
			secureItemUtil.addPseudoId(mockSubject, "dummy");
		} catch (NullPointerException e) { // this is only for ignoring parts of the class we're not interested in
			e.printStackTrace();
		}
		Mockito.verify(mockSecurityUtil, Mockito.times(1)).canEdit(mockSubject.getItem());
	}
	
	@Test( groups = { "exception" }, expectedExceptions = { IllegalAccessException.class } )
	public void addPseudoIdWithIllegalAccess() throws Exception {
		try {
			secureItemUtil.addPseudoId(mockSubject, pseudoId);
		} catch (NullPointerException e) { // this is only for ignoring parts of the class we're not interested in
			e.printStackTrace();
		}
	}

	// TODO - commented out because of the PowerMock initializer problem, see https://code.google.com/p/powermock/issues/detail?id=414
//	@ObjectFactory
//	/**
//	 * Configure TestNG to use the PowerMock object factory.
//	 */
//	public IObjectFactory getObjectFactory() {
//		return new org.powermock.modules.testng.PowerMockObjectFactory();
//	}
//	
//	@Test( groups = { "affirmative" } )
//	public final void getPseudonym() throws Exception {	// written based on guidelines in https://code.google.com/p/powermock/wiki/MockitoUsage13	
//		PowerMockito.spy(AutoExtSubjectpseudonym.class);
//		Mockito.when(AutoExtSubjectpseudonym.getExtSubjectpseudonymsById(pseudoId, mockUser, false)).thenReturn(new ExtSubjectpseudonym());
//		secureItemUtil.getPseudonym(pseudoId);
////		PowerMockito.verifyStatic(Mockito.times(1));
////		AutoExtSubjectpseudonym.getExtSubjectpseudonymsById(pseudoId, mockUser, false);		
//		Mockito.verify(mockSecurityUtil, Mockito.times(1)).canRead(mockSubject.getItem());
//	}

	// TODO public void getMatchingSubject()

	// TODO public void getSubjectByLabelOrId()
}
