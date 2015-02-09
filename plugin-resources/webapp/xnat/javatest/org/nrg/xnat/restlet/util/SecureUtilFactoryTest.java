package org.nrg.xnat.restlet.util;

import static org.mockito.Mockito.mock;

import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.nrg.xnat.security.ISecurityUtil;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SecureUtilFactoryTest {
	
	@DataProvider( name = "validSecurityUtil" )
	public Object[][] getValidSecurityUtil() {
		ISecurityUtil securityUtil = SecureUtilFactory.getSecurityUtilInstance();
		securityUtil.setUser(new XDATUser());
		securityUtil.setResource(mock(SecureResource.class));
		return new Object[][]{
				{securityUtil}
		};
	}
	
	@DataProvider( name = "invalidSecurityUtil" )
	public Object[][] getInvalidSecurityUtil() {
		ISecurityUtil securityUtil1 = SecureUtilFactory.getSecurityUtilInstance();
		securityUtil1.setUser(new XDATUser());
		ISecurityUtil securityUtil2 = SecureUtilFactory.getSecurityUtilInstance();
		securityUtil2.setResource(mock(SecureResource.class));
		return new Object[][]{
				{null},
				{securityUtil1},
				{securityUtil2}
		};
	}

	@Test( dataProvider = "validSecurityUtil" )
	public void getSecureItemUtilInstance(ISecurityUtil securityUtil) {
		ISecureItemUtil secureItemUtil = SecureUtilFactory.getSecureItemUtilInstance(securityUtil);
		assert secureItemUtil != null;
	}
	
	@Test(expectedExceptions = { IllegalArgumentException.class }, dataProvider = "invalidSecurityUtil")
    public void getSecureItemUtilInstanceWithNullSecurityUtil(ISecurityUtil securityUtil) throws Exception {
		SecureUtilFactory.getSecureItemUtilInstance(securityUtil);
    }

	@Test
	public void getSecurityUtilInstance() {
		ISecurityUtil securityUtil = SecureUtilFactory.getSecurityUtilInstance();
		assert securityUtil != null;
	}
}
