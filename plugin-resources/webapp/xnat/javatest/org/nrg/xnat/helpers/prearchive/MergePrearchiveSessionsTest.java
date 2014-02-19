/*
 * org.nrg.xnat.helpers.prearchive.MergePrearchiveSessionsTest
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.helpers.prearchive;

import org.junit.*;
import org.nrg.xnat.helpers.merge.MergePrearchiveSessions;

import java.io.File;

import static org.junit.Assert.fail;

public class MergePrearchiveSessionsTest {
	final File MergeSessions1 = new File("./MergeSessions1");
	final File MergeSessions2 = new File("./MergeSessions2");

	@After
	public void tearDown() throws Exception {
		org.nrg.xft.utils.FileUtils.DeleteFile(MergeSessions1);
		org.nrg.xft.utils.FileUtils.DeleteFile(MergeSessions2);
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}
	
	@Test
	public void testMergeSessions() {
		fail("Not yet implemented");
	}

	@Test
	public void testMergeDirectories() throws Exception{		
		MergeSessions1.mkdir();
		MergeSessions2.mkdir();
		
		final String content ="SFSDFSDFDSFSDFDSFSDFDSFSDFSDFDSF";
		
		final File child=new File(MergeSessions1,"TEST.txt");
		org.apache.commons.io.FileUtils.writeStringToFile(child, content);
		
		final String content2 ="asdfsdfsafdsfdsfad";
		
		final File child2=new File(MergeSessions2,"TEST.txt");
		org.apache.commons.io.FileUtils.writeStringToFile(child2, content2);
		
		MergePrearchiveSessions merger=new MergePrearchiveSessions("", MergeSessions1,null,null, MergeSessions2,null,null, true,false,null,null);
		merger.mergeDirectories(MergeSessions1, MergeSessions2,true);
		
		org.junit.Assert.assertEquals(content, org.apache.commons.io.FileUtils.readFileToString(child2));
	}

	@Test
	public void testMergeDirectories2() throws Exception{		
		MergeSessions1.mkdir();
		MergeSessions2.mkdir();
		
		final String content ="SFSDFSDFDSFSDFDSFSDFDSFSDFSDFDSF";
		
		final File child=new File(MergeSessions1,"TEST.txt");
		org.apache.commons.io.FileUtils.writeStringToFile(child, content);
		
		final String content2 ="asdfsdfsafdsfdsfad";
		
		final File child2=new File(MergeSessions2,"TEST2.txt");
		org.apache.commons.io.FileUtils.writeStringToFile(child2, content2);
		
		MergePrearchiveSessions merger=new MergePrearchiveSessions("", MergeSessions1,null,null, MergeSessions2,null,null, true,false,null,null);
		merger.mergeDirectories(MergeSessions1, MergeSessions2,true);

		final File child3=new File(MergeSessions2,"TEST.txt");
		org.junit.Assert.assertEquals(content, org.apache.commons.io.FileUtils.readFileToString(child3));
		org.junit.Assert.assertEquals(content2, org.apache.commons.io.FileUtils.readFileToString(child2));
		
		org.junit.Assert.assertFalse(child.exists());
	}

	@Test
	public void testMergeDirectories3() throws Exception{		
		MergeSessions1.mkdir();
		
		final String content ="SFSDFSDFDSFSDFDSFSDFDSFSDFSDFDSF";
		
		final File child=new File(MergeSessions1,"TEST.txt");
		org.apache.commons.io.FileUtils.writeStringToFile(child, content);		
		
		MergePrearchiveSessions merger=new MergePrearchiveSessions("", MergeSessions1,null,null, MergeSessions2,null,null, true,false,null,null);
		merger.mergeDirectories(MergeSessions1, MergeSessions2,true);
		
		final File child3=new File(MergeSessions2,"TEST.txt");
		org.junit.Assert.assertEquals(content, org.apache.commons.io.FileUtils.readFileToString(child3));
		
		org.junit.Assert.assertFalse(child.exists());
	}

}
