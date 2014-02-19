/*
 * org.nrg.xnat.archive.PrearcSessionArchiverTest
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.archive;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nrg.action.ClientException;
import org.nrg.test.BaseXDATTestCase;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.SaveItemHelper;

import java.io.File;
import java.util.Hashtable;

import static org.junit.Assert.fail;



public class PrearcSessionArchiverTest extends BaseXDATTestCase {
	private static final String MR = "TEST_MR_1";
	private static final String TEST_SUB_1 = "TEST_SUB_1";
	private static final String PROJECT = "JUNIT_TEST_XYZ";
	private static final String PROJECT2= "JUNIT_TEST_XYZ2";
	private static XnatMrsessiondata mr=null;
	private static XnatSubjectdata subject=null;
	private static XnatProjectdata proj=null;
	
	final File src = new File("./MergeSrcTest");
	final File dest = new File("./MergeDestTest");

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
			XnatProjectdata.quickSave(proj, user, false, false,null);
		}
		
		XnatProjectdata proj2=XnatProjectdata.getXnatProjectdatasById(PROJECT2, user, false);
		if(proj2==null){
			proj2 = new XnatProjectdata((UserI)user);
			proj2.setId(PROJECT2);
			proj2.setSecondaryId(PROJECT2);
			proj2.setName(PROJECT2);
			XnatProjectdata.quickSave(proj2, user, false, false,null);
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
		mr.setUid(MR);
		mr.setSubjectId(TEST_SUB_1);
		SaveItemHelper.authorizedSave(mr,user, false, false,EventUtils.TEST_EVENT(user));
	}
	
	@AfterClass
	public static void tearDownAfterClass(){
		if(subject!=null){
			subject.delete(proj, user, true,null);
		}
	}
	
	@Test
	public void shouldCheckForLblConflict() throws Exception{
		XnatMrsessiondata newMR=new XnatMrsessiondata((UserI)user);
		newMR.setId(MR);
		newMR.setProject(PROJECT);
		newMR.setLabel("TEST2");
		newMR.setSubjectId(TEST_SUB_1);
		
		PrearcSessionArchiver test=new PrearcSessionArchiver(newMR,null,user,PROJECT, new Hashtable<String,Object>(), true, true,true,false);
		try {
			XnatImagesessiondata existing=test.retrieveExistingExpt();

			test.checkForConflicts(newMR, src, existing, dest);
			fail("Expected failure");
		} catch (ClientException e) {
			if(!e.getMessage().equals(PrearcSessionArchiver.LABEL_MOD)){
				fail("Expected '" + PrearcSessionArchiver.LABEL_MOD + "' received: " + e.getMessage());
			}
		}
	}
	
	@Test
	public void shouldCheckForUIDConflict() throws Exception{
		XnatMrsessiondata newMR=new XnatMrsessiondata((UserI)user);
		newMR.setId(MR);
		newMR.setProject(PROJECT);
		newMR.setLabel(MR);
		newMR.setSubjectId(TEST_SUB_1);
		newMR.setUid(MR+"x");
		
		PrearcSessionArchiver test=new PrearcSessionArchiver(newMR,null,user,PROJECT, new Hashtable<String,Object>(), false, true,true,false);
		try {
			XnatImagesessiondata existing=test.retrieveExistingExpt();

			test.checkForConflicts(newMR, src, existing, dest);
			fail("Expected failure");
		} catch (ClientException e) {
			if(!e.getMessage().equals(PrearcSessionArchiver.UID_MOD)){
				fail("Expected '" + PrearcSessionArchiver.UID_MOD + "' received: " + e.getMessage());
			}
		}
	}
	
	@Test
	public void shouldCheckForProjectConflict() throws Exception{
		XnatMrsessiondata newMR=new XnatMrsessiondata((UserI)user);
		newMR.setId(MR);
		newMR.setProject(PROJECT2);
		newMR.setLabel(MR);
		newMR.setSubjectId(TEST_SUB_1);
		
		PrearcSessionArchiver test=new PrearcSessionArchiver(newMR,null,user,PROJECT, new Hashtable<String,Object>(), true, true,true,false);
		try {
			XnatImagesessiondata existing=test.retrieveExistingExpt();

			test.checkForConflicts(newMR, src, existing, dest);
			fail("Expected failure");
		} catch (ClientException e) {
			if(!e.getMessage().equals(PrearcSessionArchiver.PROJ_MOD)){
				fail("Expected '" + PrearcSessionArchiver.PROJ_MOD + "' received: " + e.getMessage());
			}
		}
	}
	
	@Test
	public void shouldCheckForOverride() throws Exception{
		XnatMrsessiondata newMR=new XnatMrsessiondata((UserI)user);
		newMR.setId(MR);
		newMR.setProject(PROJECT);
		newMR.setLabel(MR);
		newMR.setSubjectId(TEST_SUB_1);
		PrearcSessionArchiver test=new PrearcSessionArchiver(newMR,null,user,PROJECT, new Hashtable<String,Object>(), false, false,true,false);
		try {
			XnatImagesessiondata existing=test.retrieveExistingExpt();
		
			test.checkForConflicts(newMR, src, existing, dest);
			fail("Expected failure");
		} catch (ClientException e) {
			if(!e.getMessage().equals(PrearcSessionArchiver.PRE_EXISTS)){
				fail("Expected '" + PrearcSessionArchiver.PRE_EXISTS + "' received: " + e.getMessage());
			}
		}
	}
	
	@Test
	public void shouldAllowOverride() throws Exception{
		XnatMrsessiondata newMR=new XnatMrsessiondata((UserI)user);
		newMR.setId(MR);
		newMR.setProject(PROJECT);
		newMR.setLabel(MR);
		newMR.setSubjectId(TEST_SUB_1);
		PrearcSessionArchiver test=new PrearcSessionArchiver(newMR,null,user,PROJECT, new Hashtable<String,Object>(), false, true,true,false);
		
		XnatImagesessiondata existing=test.retrieveExistingExpt();

		test.checkForConflicts(newMR, src, existing, dest);
		}
	}
