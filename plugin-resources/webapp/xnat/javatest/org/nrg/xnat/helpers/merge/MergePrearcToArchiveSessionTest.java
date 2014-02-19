/*
 * org.nrg.xnat.helpers.merge.MergePrearcToArchiveSessionTest
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */

/**
 * 
 */
package org.nrg.xnat.helpers.merge;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nrg.action.ClientException;
import org.nrg.test.BaseXDATTestCase;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.SaveItemHelper;

import java.io.File;

import static org.junit.Assert.fail;

/**
 * @author tolsen01
 *
 */
public class MergePrearcToArchiveSessionTest extends BaseXDATTestCase {
	private static final String MR = "TEST_MR_MERGE_1";
	private static final String TEST_SUB_1 = "TEST_SUB_MERGE_1";
	private static final String PROJECT = "JUNIT_TEST_MERGE";
	private static final String PROJECT2= "JUNIT_TEST_MERGE2";
	private static XnatMrsessiondata mr=null;
	private static XnatSubjectdata subject=null;
	private static XnatProjectdata proj=null;
	
	final File src = new File("./MergeArcSrcTest");
	final File dest = new File("./MergeArcDestTest");

	@After
	public void tearDown() throws Exception {
		deleteDirNoException(src);
		deleteDirNoException(dest);
	}
	
	public void deleteDirNoException(File s) throws Exception{
		if(s.exists())org.apache.commons.io.FileUtils.deleteDirectory(s);
	}
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {	
		proj=XnatProjectdata.getXnatProjectdatasById(PROJECT, user, false);
		if(proj==null){
			proj = new XnatProjectdata((UserI)user);
			proj.setId(PROJECT);
			proj.setSecondaryId(PROJECT);
			proj.setName(PROJECT);
			XnatProjectdata.quickSave(proj, user, false, false,EventUtils.TEST_EVENT(user));
		}
		
		XnatProjectdata proj2=XnatProjectdata.getXnatProjectdatasById(PROJECT2, user, false);
		if(proj2==null){
			proj2 = new XnatProjectdata((UserI)user);
			proj2.setId(PROJECT2);
			proj2.setSecondaryId(PROJECT2);
			proj2.setName(PROJECT2);
			XnatProjectdata.quickSave(proj2, user, false, false,EventUtils.TEST_EVENT(user));
		}
		
		subject=new XnatSubjectdata((UserI)user);
		subject.setId(TEST_SUB_1);
		subject.setProject(PROJECT);
		subject.setLabel(TEST_SUB_1);
		SaveItemHelper.authorizedSave(subject,user, false, false,EventUtils.TEST_EVENT(user));
		
		mr=new XnatMrsessiondata((UserI)user);
		mr.setId(MR);
		mr.setProject(PROJECT);
		mr.setLabel(MR);
		mr.setSubjectId(TEST_SUB_1);
		SaveItemHelper.authorizedSave(mr,user, false, false,EventUtils.TEST_EVENT(user));
	}
	
	@AfterClass
	public static void tearDownAfterClass(){
		if(subject!=null){
			subject.delete(proj, user, true,null);
		}
	}

	/**
	 * Test method for {@link org.nrg.xnat.helpers.merge.MergeSessionsA#checkForConflict()}.
	 */
	@Test
	public void testCheckForConflict() throws Exception {
		XnatMrsessiondata newMR=new XnatMrsessiondata((UserI)user);
		newMR.setId(MR);
		newMR.setProject(PROJECT);
		newMR.setLabel("TEST2");
		newMR.setSubjectId(TEST_SUB_1);
		
		src.mkdirs();
		dest.mkdirs();
		
		createFile(dest, "TEST2.txt", "SDFDSFDSF");
		
		MergePrearcToArchiveSession test=new MergePrearcToArchiveSession("",src,newMR,null,dest,mr,null,false,false,null,null,null);
		try {
			test.checkForConflict();
			fail("Expected failure");
		} catch (ClientException e) {
			if(!e.getMessage().equals(MergePrearcToArchiveSession.HAS_FILES)){
				fail("Expected '" + MergePrearcToArchiveSession.HAS_FILES + "' received: " + e.getMessage());
			}
		}
	}

	/**
	 * Test method for {@link org.nrg.xnat.helpers.merge.MergeSessionsA#checkForConflict()}.
	 */
	@Test
	public void testCheckForConflict2() throws Exception {
		XnatMrsessiondata newMR=new XnatMrsessiondata((UserI)user);
		newMR.setId(MR);
		newMR.setProject(PROJECT);
		newMR.setLabel("TEST2");
		newMR.setSubjectId(TEST_SUB_1);
		
		src.mkdirs();
		dest.mkdirs();
		
		createFile(src, "TEST1.txt", "SDFDSFDSF");
		createFile(dest, "TEST2.txt", "SDFDSFDSF");
		
		MergePrearcToArchiveSession test=new MergePrearcToArchiveSession("",src,newMR,null,dest,mr,null,true,false,null,null,null);
		test.checkForConflict();
	}

	/**
	 * Test method for {@link org.nrg.xnat.helpers.merge.MergeSessionsA#checkForConflict()}.
	 */
	@Test
	public void testCheckForConflict3() throws Exception {
		XnatMrsessiondata newMR=new XnatMrsessiondata((UserI)user);
		newMR.setId(MR);
		newMR.setProject(PROJECT);
		newMR.setLabel("TEST2");
		newMR.setSubjectId(TEST_SUB_1);
		
		src.mkdirs();
		dest.mkdirs();

		createFile(src, "TEST1.txt", "SDFDSFDSF");
		createFile(src, "TEST2.txt", "SDFDSFDSF");

		createFile(dest, "TEST2.txt", "SDFDSFDSF");
		
		MergePrearcToArchiveSession test=new MergePrearcToArchiveSession("",src,newMR,null,dest,mr,null,true,false,null,null,null);
		try {
			test.checkForConflict();
			fail("Expected failure");
		} catch (ClientException e) {
			if(!e.getMessage().equals(MergePrearcToArchiveSession.HAS_FILES)){
				fail("Expected '" + MergePrearcToArchiveSession.HAS_FILES + "' received: " + e.getMessage());
			}
		}
	}


	/**
	 * Test method for {@link org.nrg.xnat.helpers.merge.MergeSessionsA#checkForConflict()}.
	 */
	@Test
	public void testCheckForConflict4() throws Exception {
		XnatMrsessiondata newMR=new XnatMrsessiondata((UserI)user);
		newMR.setId(MR);
		newMR.setProject(PROJECT);
		newMR.setLabel("TEST2");
		newMR.setSubjectId(TEST_SUB_1);
		
		src.mkdirs();
		dest.mkdirs();

		createFile(src, "TEST1.txt", "SDFDSFDSF");
		createFile(src, "TEST2.txt", "SDFDSFDSF");

		createFile(dest, "TEST2.txt", "SDFDSFDSF");
		
		MergePrearcToArchiveSession test=new MergePrearcToArchiveSession("",src,newMR,null,dest,mr,null,true,true,null,null,null);
		test.checkForConflict();

	}
}
