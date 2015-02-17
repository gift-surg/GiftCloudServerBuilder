package org.nrg.xnat.restlet.util;

import org.mockito.Mockito;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.nrg.xnat.security.ISecurityUtil;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SecureUtilFactoryTest {
	XDATUser user;
	SecureResource mockResource;
	IItemUtil validItemUtil;
	ISecurityUtil validSecurityUtil;
	
	@BeforeClass
	public void populate() {
		user = new XDATUser();
		mockResource = Mockito.mock(SecureResource.class);
		
		validItemUtil = SecureUtilFactory.getItemUtilInstance();
		validItemUtil.setUser(user);
		validItemUtil.setResource(mockResource);
		
		validSecurityUtil = SecureUtilFactory.getSecurityUtilInstance();
		validSecurityUtil.setUser(user);
		validSecurityUtil.setResource(mockResource);
	}
	
	@DataProvider( name = "valid" )
	public Object[][] getValidArguments() {
		return new Object[][]{
				{validItemUtil, validSecurityUtil}
		};
	}
	
	@DataProvider( name = "invalid" )
	public Object[][] getInvalidArguments() {
		
		// invalid security utils
		ISecurityUtil invalidSecurityUtil1 = SecureUtilFactory.getSecurityUtilInstance(); // no user, no resource
		ISecurityUtil invalidSecurityUtil2 = SecureUtilFactory.getSecurityUtilInstance(); // no user
		invalidSecurityUtil2.setResource(mockResource);
		ISecurityUtil invalidSecurityUtil3 = SecureUtilFactory.getSecurityUtilInstance(); // no resource
		invalidSecurityUtil3.setUser(user);
		
		// invalid item utils
		IItemUtil invalidItemUtil1 = SecureUtilFactory.getItemUtilInstance(); // no user, no resource
		IItemUtil invalidItemUtil2 = SecureUtilFactory.getItemUtilInstance(); // no user
		invalidItemUtil2.setResource(mockResource);
		IItemUtil invalidItemUtil3 = SecureUtilFactory.getItemUtilInstance(); // no resource
		invalidItemUtil3.setUser(user);
		
		// not matching item util
		IItemUtil notMatchingItemUtil = SecureUtilFactory.getItemUtilInstance();
		notMatchingItemUtil.setUser(user);
		notMatchingItemUtil.setResource(mockResource);
		
		// not matching security utils
		ISecurityUtil notMatchingSecurityUtil1 = SecureUtilFactory.getSecurityUtilInstance(); // user and resource not matching
		notMatchingSecurityUtil1.setUser(new XDATUser());
		notMatchingSecurityUtil1.setResource(Mockito.mock(SecureResource.class));
		ISecurityUtil notMatchingSecurityUtil2 = SecureUtilFactory.getSecurityUtilInstance(); // user not matching
		notMatchingSecurityUtil2.setUser(new XDATUser());
		notMatchingSecurityUtil2.setResource(mockResource);
		ISecurityUtil notMatchingSecurityUtil3 = SecureUtilFactory.getSecurityUtilInstance(); // resource not matching
		notMatchingSecurityUtil3.setUser(user);
		notMatchingSecurityUtil3.setResource(Mockito.mock(SecureResource.class));
		
		return new Object[][]{
				{null, null},
				
				{validItemUtil, null},
				{validItemUtil, invalidSecurityUtil1},
				{validItemUtil, invalidSecurityUtil2},
				{validItemUtil, invalidSecurityUtil3},
				
				{null, validSecurityUtil},
				{invalidItemUtil1, validSecurityUtil},
				{invalidItemUtil2, validSecurityUtil},
				{invalidItemUtil3, validSecurityUtil},
				
				{notMatchingItemUtil, notMatchingSecurityUtil1},
				{notMatchingItemUtil, notMatchingSecurityUtil2},
				{notMatchingItemUtil, notMatchingSecurityUtil3},
		};
	}

	@Test( dataProvider = "valid" )
	public void getSecureItemUtilInstance(IItemUtil itemUtil, ISecurityUtil securityUtil) throws IllegalArgumentException {
		Object secureItemUtil = SecureUtilFactory.getSecureItemUtilInstance(itemUtil, securityUtil);
		assert secureItemUtil != null;
		assert secureItemUtil instanceof ISecureItemUtil;
	}
	
	@Test( expectedExceptions = { IllegalArgumentException.class }, dataProvider = "invalid" )
    public void getSecureItemUtilInstanceWithInvalidArguments(IItemUtil itemUtil, ISecurityUtil securityUtil) throws Exception {
		SecureUtilFactory.getSecureItemUtilInstance(itemUtil, securityUtil);
    }
	
	@Test
	public void getItemUtilInstance() {
		Object object = SecureUtilFactory.getItemUtilInstance();
		assert object != null;
		assert object instanceof IItemUtil;
	}
	
	@Test
	public void getSecurityUtilInstance() {
		Object object = SecureUtilFactory.getSecurityUtilInstance();
		assert object != null;
		assert object instanceof ISecurityUtil;
	}
}
