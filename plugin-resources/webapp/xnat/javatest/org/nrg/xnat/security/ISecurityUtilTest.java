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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.XFTItem;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.nrg.xnat.restlet.util.SecureUtilFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ISecurityUtilTest {
	
	@DataProvider( name = "canDo" )
	public Object[][] getCanDoData() throws Exception {
		XFTItem item = new XFTItem();
		XDATUser mockUserThatCan = mock(XDATUser.class);
		when(mockUserThatCan.canRead(item)).thenAnswer(new Answer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				return true;
			}
		});
		when(mockUserThatCan.canEdit(item)).thenAnswer(new Answer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				return true;
			}
		});
		ISecurityUtil utilThatCan = SecureUtilFactory.getSecurityUtilInstance();
		utilThatCan.setUser(mockUserThatCan);

		XDATUser mockUserThatCannot = mock(XDATUser.class);
		when(mockUserThatCannot.canRead(item)).thenAnswer(new Answer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				return false;
			}
		});
		when(mockUserThatCannot.canEdit(item)).thenAnswer(new Answer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				return false;
			}
		});
		ISecurityUtil utilThatCannot = SecureUtilFactory.getSecurityUtilInstance();
		utilThatCannot.setUser(mockUserThatCannot);
		
		return new Object[][]{
				{ utilThatCan, item, true },
				{ utilThatCannot, item, false }
		};
	}
	
	@DataProvider( name = "setGetUser" )
	public Object[][] getSetGetUserData() {
		return new Object[][]{
				{SecureUtilFactory.getSecurityUtilInstance(), mock(XDATUser.class)},
				{SecureUtilFactory.getSecurityUtilInstance(), mock(XDATUser.class)}
		};
	}
	
	@DataProvider( name = "setGetResource" )
	public Object[][] getSetGetResourceData() {
		return new Object[][]{
				{SecureUtilFactory.getSecurityUtilInstance(), mock(SecureResource.class)},
				{SecureUtilFactory.getSecurityUtilInstance(), mock(SecureResource.class)}
		};
	}

	@Test( dataProvider = "canDo" )
	public void canRead(ISecurityUtil util, XFTItem item, boolean expected) {
		assert util.canRead(item) == expected;
	}

	@Test( dataProvider = "canDo" )
	public void canEdit(ISecurityUtil util, XFTItem item, boolean expected) throws Exception {
		assert util.canEdit(item) == expected;
	}

	@Test( dataProvider = "setGetUser" )
	public void setGetUser(ISecurityUtil util, XDATUser user) {
		util.setUser(user);
		assert user == util.getUser();
	}

	@Test( dataProvider = "setGetResource" )
	public void setGetResource(ISecurityUtil util, SecureResource resource) {
		util.setResource(resource);
		assert resource == util.getResource();
	}
}
