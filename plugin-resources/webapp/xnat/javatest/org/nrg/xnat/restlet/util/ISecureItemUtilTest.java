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

import java.util.ArrayList;
import java.util.Optional;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.nrg.xdat.exceptions.IllegalAccessException;
import org.nrg.xdat.om.ExtSubjectpseudonym;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.XFTItem;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.nrg.xnat.security.ISecurityUtil;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;

public class ISecureItemUtilTest {
	XDATUser mockUser;
	SecureResource mockResource;
	
	IItemUtil mockItemUtil;
	ISecurityUtil mockSecurityUtil;
	ISecureItemUtil secureItemUtil;
	
	String pseudoId, existingPseudoId;
	XnatSubjectdata mockSubject;
	XFTItem mockSubjectItem;
	String mockSubjectLabel;
	
	ExtSubjectpseudonym mockPseudonym;
	XFTItem mockPseudonymItem;
	
	Answer<Boolean> affirm, deny;
	ArrayList<XFTItem> items;
	
	@BeforeClass
	public void populate() {
		items = new ArrayList<XFTItem>();
		pseudoId = "dummyPseudoId";
		existingPseudoId = "existingDummyPseudoId";
		mockSubjectLabel = "dummySubject";
		
		// mockPseudonym
		mockPseudonymItem = new XFTItem();
		items.add(mockPseudonymItem);
		mockPseudonym = Mockito.mock(ExtSubjectpseudonym.class);
		Mockito.when(mockPseudonym.getItem()).thenAnswer(new Answer<XFTItem>() {
			@Override
			public XFTItem answer(InvocationOnMock invocation) throws Throwable {
				return mockPseudonymItem;
			}
		});
		Mockito.when(mockPseudonym.getSubject()).thenAnswer(new Answer<String>() {
			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				return mockSubjectLabel;
			}
		});
		Answer< Optional<ExtSubjectpseudonym> > mockPseudonymAnswer = new Answer< Optional<ExtSubjectpseudonym> >() {
			@Override
			public Optional<ExtSubjectpseudonym> answer(InvocationOnMock invocation)
					throws Throwable {
				return Optional.of(mockPseudonym);
			}
		};
		// ====================
		
		// mockSubject
		mockSubjectItem = new XFTItem();
		items.add(mockSubjectItem);
		mockSubject = Mockito.mock(XnatSubjectdata.class);
		Mockito.when(mockSubject.getItem()).thenAnswer(new Answer<XFTItem>() {
			@Override
			public XFTItem answer(InvocationOnMock invocation) throws Throwable {
				return mockSubjectItem;
			}
		});
		Answer< Optional<XnatSubjectdata> > mockSubjectAnswer = new Answer< Optional<XnatSubjectdata> >() {
			@Override
			public Optional<XnatSubjectdata> answer(InvocationOnMock invocation)
					throws Throwable {
				return Optional.of(mockSubject);
			}
		};
		// ====================
		
		// mockUser & mockResource
		mockUser = Mockito.mock(XDATUser.class);
		Answer<XDATUser> answerGetUser = new Answer<XDATUser>() {
			@Override
			public XDATUser answer(InvocationOnMock invocation)
					throws Throwable {
				return mockUser;
			}
		};
		mockResource = Mockito.mock(SecureResource.class);
		Answer<SecureResource> answerGetResource = new Answer<SecureResource>() {
			@Override
			public SecureResource answer(InvocationOnMock invocation)
					throws Throwable {
				return mockResource;
			}
		};
		// ====================
		
		// mockItemUtil
		mockItemUtil = Mockito.mock(IItemUtil.class);
		Mockito.when(mockItemUtil.getUser()).thenAnswer(answerGetUser);
		Mockito.when(mockItemUtil.getResource()).thenAnswer(answerGetResource);
		
		Mockito.when(mockItemUtil.getMatchingSubjectImpl(pseudoId)).thenAnswer(mockSubjectAnswer);
		Mockito.when(mockItemUtil.getPseudonymImpl(pseudoId)).thenAnswer(mockPseudonymAnswer);
		Mockito.when(mockItemUtil.getSubjectByLabelOrIdImpl(mockSubjectLabel)).thenAnswer(mockSubjectAnswer);
		Mockito.when(mockItemUtil.addPseudoIdImpl(mockSubject, pseudoId)).thenAnswer(mockPseudonymAnswer);
		Mockito.when(mockItemUtil.addPseudoIdImpl(mockSubject, existingPseudoId)).thenThrow(new IllegalStateException("Pseudonym "+existingPseudoId+" already exists"));
		// ====================
		
		// mockSecurityUtil
		mockSecurityUtil = Mockito.mock(ISecurityUtil.class);
		Mockito.when(mockSecurityUtil.getUser()).thenAnswer(answerGetUser);
		Mockito.when(mockSecurityUtil.getResource()).thenAnswer(answerGetResource);
		// ====================
		
		// mockSecureItemUtil
		secureItemUtil = SecureUtilFactory.getSecureItemUtilInstance(mockItemUtil, mockSecurityUtil);
		// ====================
		
		
		affirm = new Answer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				return true;
			}
		};
		deny = new Answer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				return false;
			}
		};
	}
	
	@BeforeGroups( groups = { "affirmative" } )
	public void configureAffirmative() {
		for (XFTItem item : items) {
			Mockito.when(mockSecurityUtil.canRead(item)).thenAnswer(affirm);
			Mockito.when(mockSecurityUtil.canEdit(item)).thenAnswer(affirm);
		}
	}
	
	@BeforeGroups( groups = { "exception" } )
	public void configureException() {
		for (XFTItem item : items) {
			Mockito.when(mockSecurityUtil.canRead(item)).thenAnswer(deny);
			Mockito.when(mockSecurityUtil.canEdit(item)).thenAnswer(deny);
		}
	}
	
	@Test( groups = { "affirmative" } )
	public void getPseudonym() throws IllegalAccessException {
		assert mockPseudonym == secureItemUtil.getPseudonym(pseudoId).get();
		Mockito.verify(mockSecurityUtil, Mockito.atLeastOnce()).canRead(mockPseudonymItem);
	}
	
	@Test( groups = { "exception" }, expectedExceptions = { IllegalAccessException.class } )
	public void getPseudonymWithIllegalAccess() throws Exception {
		secureItemUtil.getPseudonym(pseudoId);
	}

	@Test( groups = { "affirmative" } )
	public void getMatchingSubject() throws IllegalAccessException {
		assert mockSubject == secureItemUtil.getMatchingSubject(pseudoId).get();
		Mockito.verify(mockSecurityUtil, Mockito.atLeastOnce()).canRead(mockSubjectItem);
	}
	
	@Test( groups = { "exception" }, expectedExceptions = { IllegalAccessException.class } )
	public void getMatchingSubjectWithIllegalAccess() throws Exception {
		secureItemUtil.getMatchingSubject(pseudoId);
	}

	@Test( groups = { "affirmative" } )
	public void getSubjectByLabelOrId() throws IllegalAccessException {
		assert mockSubject == secureItemUtil.getSubjectByLabelOrId(mockSubjectLabel).get();
		Mockito.verify(mockSecurityUtil, Mockito.atLeastOnce()).canRead(mockSubjectItem);
	}
	
	@Test( groups = { "exception" }, expectedExceptions = { IllegalAccessException.class } )
	public void getSubjectByLabelOrIdWithIllegalAccess() throws Exception {
		secureItemUtil.getSubjectByLabelOrId(mockSubjectLabel);
	}

	@Test( groups = { "affirmative" } )
	public void addPseudoId() throws IllegalAccessException {
		assert mockPseudonym == secureItemUtil.addPseudoId(mockSubject, pseudoId).get();
		Mockito.verify(mockSecurityUtil, Mockito.atLeastOnce()).canEdit(mockSubjectItem);
	}
	
	@Test( groups = { "exception" }, expectedExceptions = { IllegalAccessException.class } )
	public void addPseudoIdWithIllegalAccess() throws Exception {
		secureItemUtil.addPseudoId(mockSubject, pseudoId);
	}
	
	@Test( groups = { "affirmative" }, expectedExceptions = { IllegalStateException.class } ) // can edit, however pseudonym already exists!
	public void addPseudoIdAtIllegalState() throws Exception {
		secureItemUtil.addPseudoId(mockSubject, existingPseudoId);
	}
}
