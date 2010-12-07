package org.nrg.xft.utils;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nrg.xft.exception.InvalidValueException;

public class FileUtilsTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testValidateUriAgainstRootStringStringString() {
		final File session_dir=new File("test/mr1");
		final File session_dir2=new File("test/mr2");
		final String snapshot = "SCANS/1/SNAPSHOTS/x.gif";
		
		try {
			FileUtils.ValidateUriAgainstRoot((new File(session_dir,snapshot)).getAbsolutePath(), session_dir.getAbsolutePath(), "");
			fail("This should not succeed.");
		} catch (InvalidValueException e) {
			
		}
	}

}
