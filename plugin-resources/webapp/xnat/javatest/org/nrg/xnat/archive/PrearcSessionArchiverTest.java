package org.nrg.xnat.archive;

import java.util.Hashtable;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import org.nrg.test.BaseXDATTestCase;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xft.security.UserI;



public class PrearcSessionArchiverTest extends BaseXDATTestCase {
	private static final String MR = "TEST_MR_1";
	private static final String TEST_SUB_1 = "TEST_SUB_1";
	private static final String PROJECT = "JUNIT_TEST_XYZ";
	private static final String PROJECT2= "JUNIT_TEST_XYZ2";
	private static XnatMrsessiondata mr=null;
	private static XnatSubjectdata subject=null;
	private static XnatProjectdata proj=null;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {	
		proj=XnatProjectdata.getXnatProjectdatasById(PROJECT, user, false);
		if(proj==null){
			proj = new XnatProjectdata((UserI)user);
			proj.setId(PROJECT);
			proj.setSecondaryId(PROJECT);
			proj.setName(PROJECT);
			XnatProjectdata.quickSave(proj, user, false, false);
		}
		
		XnatProjectdata proj2=XnatProjectdata.getXnatProjectdatasById(PROJECT2, user, false);
		if(proj2==null){
			proj2 = new XnatProjectdata((UserI)user);
			proj2.setId(PROJECT2);
			proj2.setSecondaryId(PROJECT2);
			proj2.setName(PROJECT2);
			XnatProjectdata.quickSave(proj2, user, false, false);
		}
		
		subject=new XnatSubjectdata((UserI)user);
		subject.setId(TEST_SUB_1);
		subject.setProject(PROJECT);
		subject.setLabel(TEST_SUB_1);
		subject.save(user, false, false);
		
		mr=new XnatMrsessiondata((UserI)user);
		mr.setId(MR);
		mr.setProject(PROJECT);
		mr.setLabel(MR);
		mr.setSubjectId(TEST_SUB_1);
		mr.save(user, false, false);
	}
	
	@Test
	public void shouldCheckForLblConflict() throws ArchivingException{
		XnatMrsessiondata newMR=new XnatMrsessiondata((UserI)user);
		newMR.setId(MR);
		newMR.setProject(PROJECT);
		newMR.setLabel("TEST2");
		newMR.setSubjectId(TEST_SUB_1);
		
		PrearcSessionArchiver test=new PrearcSessionArchiver(newMR,user, PROJECT,new Hashtable<String,Object>(), false, false);
		try {
			test.retrieveExistingExpt();
		} catch (ArchivingException e) {
			if(!e.getMessage().contains("new session label matches preexisting label")){
				assertTrue("Expected 'new session label matches preexisting label...' received: " + e.getMessage(),false);
			}
		}
	}
	
	@Test
	public void shouldCheckForProjectConflict() throws ArchivingException{
		XnatMrsessiondata newMR=new XnatMrsessiondata((UserI)user);
		newMR.setId(MR);
		newMR.setProject(PROJECT2);
		newMR.setLabel(MR);
		newMR.setSubjectId(TEST_SUB_1);
		
		PrearcSessionArchiver test=new PrearcSessionArchiver(newMR,user, PROJECT2,new Hashtable<String,Object>(), false, false);
		try {
			test.retrieveExistingExpt();
		} catch (ArchivingException e) {
			if(!e.getMessage().contains("illegal project change")){
				assertTrue("Expected 'illegal project change...' received: " + e.getMessage(),false);
			}
		}
	}
	
	@Test
	public void shouldCheckForOverride() throws ArchivingException{
		XnatMrsessiondata newMR=new XnatMrsessiondata((UserI)user);
		newMR.setId(MR);
		newMR.setProject(PROJECT);
		newMR.setLabel(MR);
		newMR.setSubjectId(TEST_SUB_1);
		
		PrearcSessionArchiver test=new PrearcSessionArchiver(newMR,user, PROJECT,new Hashtable<String,Object>(), false, false);
		try {
			test.retrieveExistingExpt();
		} catch (ArchivingException e) {
			if(!e.getMessage().contains("conflict")){
				assertTrue("Expected 'conflict' received: " + e.getMessage(),false);
			}
		}
	}
	
	@AfterClass
	public static void tearDownAfterClass(){
		if(subject!=null){
			subject.delete(proj, user, true);
		}
	}
}
