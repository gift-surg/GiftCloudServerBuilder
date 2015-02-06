package org.nrg.xnat.restlet.util;

import java.util.Optional;

import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.nrg.xnat.security.ISecurityUtil;
import org.testng.annotations.Test;

import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

public class SecureUtilFactoryTest {

	@Test
	public void getSecureItemUtilInstance() {
		ISecurityUtil securityUtil = SecureUtilFactory.getSecurityUtilInstance();
		securityUtil.setUser(new XDATUser());
		securityUtil.setResource(mock(SecureResource.class));
		ISecureItemUtil secureItemUtil = SecureUtilFactory.getSecureItemUtilInstance(securityUtil);
		assert secureItemUtil != null;
	}
	
	@Test(expectedExceptions = { IllegalArgumentException.class })
    public void getSecureItemUtilInstanceWithNullSecurityUtil() throws Exception {
		SecureUtilFactory.getSecureItemUtilInstance(null);
    }
	
	@Test(expectedExceptions = { IllegalArgumentException.class })
    public void getSecureItemUtilInstanceWithBadSecurityUtil1() throws Exception {
		ISecurityUtil securityUtil = SecureUtilFactory.getSecurityUtilInstance();
		securityUtil.setUser(new XDATUser());
		SecureUtilFactory.getSecureItemUtilInstance(securityUtil);
    }
	
	@Test(expectedExceptions = { IllegalArgumentException.class })
    public void getSecureItemUtilInstanceWithBadSecurityUtil2() throws Exception {
		ISecurityUtil securityUtil = SecureUtilFactory.getSecurityUtilInstance();
		securityUtil.setResource(mock(SecureResource.class));
		SecureUtilFactory.getSecureItemUtilInstance(securityUtil);
    }

	@Test
	public void getSecurityUtilInstance() {
		ISecurityUtil securityUtil = SecureUtilFactory.getSecurityUtilInstance();
		assert securityUtil != null;
	}
}
