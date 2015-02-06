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
package org.nrg.xnat.security;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.XFTItem;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.nrg.xnat.restlet.util.SecureUtilFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ISecurityUtilTest {
	SecureResource mockResource;
	XDATUser mockUser;
	
	@BeforeTest
	public void populate() {
		mockResource = Mockito.mock(SecureResource.class);
		mockUser = Mockito.mock(XDATUser.class);
	}
	
	@Test
	public void canRead() {
		ISecurityUtil util = SecureUtilFactory.getSecurityUtilInstance();
		util.setUser(mockUser);
		XFTItem item = new XFTItem();
		try {
			when(mockUser.canRead(item)).thenAnswer(new Answer<Boolean>() {
				@Override
				public Boolean answer(InvocationOnMock invocation) throws Throwable {
					return false;
				}
			});
			assert !util.canRead(item);
			
			when(mockUser.canRead(item)).thenAnswer(new Answer<Boolean>() {
				@Override
				public Boolean answer(InvocationOnMock invocation) throws Throwable {
					return true;
				}
			});
			assert util.canRead(item);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void canEdit() {
		ISecurityUtil util = SecureUtilFactory.getSecurityUtilInstance();
		util.setUser(mockUser);
		XFTItem item = new XFTItem();
		try {
			when(mockUser.canEdit(item)).thenAnswer(new Answer<Boolean>() {
				@Override
				public Boolean answer(InvocationOnMock invocation) throws Throwable {
					return false;
				}
			});
			assert !util.canEdit(item);
			
			when(mockUser.canEdit(item)).thenAnswer(new Answer<Boolean>() {
				@Override
				public Boolean answer(InvocationOnMock invocation) throws Throwable {
					return true;
				}
			});
			assert util.canEdit(item);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void setGetUser() {
		ISecurityUtil util = SecureUtilFactory.getSecurityUtilInstance();
		XDATUser user1 = new XDATUser();
		util.setUser(user1);
		assert user1==util.getUser();
		XDATUser user2 = new XDATUser();
		util.setUser(user2);
		assert user2==util.getUser();
	}
	
	@Test
	public void setGetResource() {
		ISecurityUtil util = SecureUtilFactory.getSecurityUtilInstance();
		util.setResource(mockResource);
		assert mockResource == util.getResource();
	}
}
