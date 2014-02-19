/*
 * org.nrg.xft.presentation.ItemPropBuilderTest
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xft.presentation;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nrg.test.BaseXDATTestCase;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xft.XFTItem;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.presentation.FlattenedItem.FlattenedItemModifierI;
import org.nrg.xft.presentation.FlattenedItemA.ChildCollection;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xnat.presentation.ChangeSummaryBuilderA;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ItemPropBuilderTest extends BaseXDATTestCase{

	@BeforeClass
	public static void setUpBeforeClassInit() throws Exception {
		BaseXDATTestCase.setUpBeforeClassInit();
	}

	static XnatProjectdata proj=null;
	private static XnatProjectdata getProject() throws Exception{
		if(proj==null){
			proj=new XnatProjectdata((UserI)user);
			String id=RandomStringUtils.randomAlphanumeric(16);
			proj.setId(id);
			proj.setSecondaryId(id);
			proj.setName(id);
			
			XnatProjectdata.createProject(proj, user, true,false, EventUtils.TEST_EVENT(user), "private");
		}
		
		return proj;
	}

	public XnatSubjectdata getSubject() throws Exception{
		String subjectid=RandomStringUtils.randomAlphanumeric(6);
		XnatSubjectdata subj=new XnatSubjectdata((UserI)user);
		subj.setId(subjectid);
		subj.setLabel(subjectid);
		subj.setProject(getProject().getId());
		subj.setProperty("xnat:subjectData/demographics[@xsi:type=xnat:demographicData]/race", "y");
		
		SaveItemHelper.authorizedSave(subj, user, false, true, EventUtils.TEST_EVENT(user));
		
		return subj;
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception{
		proj.delete(true, admin_user, EventUtils.TEST_EVENT(user));
	}

	@Test
	public void testDemographicsMod() throws Exception {		
		XnatSubjectdata subj=getSubject();
		
		subj.setProperty("xnat:subjectData/demographics[@xsi:type=xnat:demographicData]/race", "x");
		SaveItemHelper.authorizedSave(subj, user, false, false, EventUtils.TEST_EVENT(user));
		
		XFTItem mod=subj.getItem().getCurrentDBVersion();
		FlattenedItemI fi=ItemMerger.merge(ItemPropBuilder.build(mod, FlattenedItemA.GET_ALL, new ArrayList<FlattenedItemModifierI>()));
		
		ChildCollection cc=fi.getChildCollections().get("demographics");
		
		assertNotNull(cc);
		
		assertEquals(1,cc.getChildren().size());

		FlattenedItemI original=cc.getChildren().get(0);
		assertEquals(1,original.getHistory().size());
		
		List<FlattenedItemI> flattened=ChangeSummaryBuilderA.flatten(original);
		
		assertEquals("y",flattened.get(0).getFields().getParams().get("race"));
		assertEquals("x",flattened.get(1).getFields().getParams().get("race"));
	}
	
	@Test
	public void testInvestigatorMod() throws Exception {			
		XnatSubjectdata subj1=getSubject();
		
		String id=RandomStringUtils.randomAlphanumeric(6);
		XnatMrsessiondata mr=new XnatMrsessiondata((UserI)user);
		mr.setId(id);
		mr.setLabel(id);
		mr.setProject(getProject().getId());
		mr.setSubjectId(subj1.getId());
		mr.setProperty("xnat:mrSessionData/investigator/ID", "x1");
		mr.setProperty("xnat:mrSessionData/investigator/lastname", "v1");
		SaveItemHelper.authorizedSave(mr, user, false, true, EventUtils.TEST_EVENT(user));
		
		Thread.sleep(1);

		mr.setProperty("xnat:mrSessionData/investigator/lastname", "v2");
		SaveItemHelper.authorizedSave(mr, user, false, false, EventUtils.TEST_EVENT(user));
		
		Thread.sleep(1);
		
		FlattenedItemI fi=ItemMerger.merge(ItemPropBuilder.build(mr.getItem().getCurrentDBVersion(), FlattenedItemA.GET_ALL, new ArrayList<FlattenedItemModifierI>()));
			
		ChildCollection cc=fi.getChildCollections().get("investigator");
		
		assertNotNull(cc);
		
		assertEquals(1,cc.getChildren().size());

		FlattenedItemI original=cc.getChildren().get(0);
		assertEquals(1,original.getHistory().size());
		
		List<FlattenedItemI> flattened=ChangeSummaryBuilderA.flatten(original);
		
		assertEquals("v1",flattened.get(0).getFields().getParams().get("lastname"));
		assertEquals("v2",flattened.get(1).getFields().getParams().get("lastname"));
	}
	
	@Test
	public void testScanMod() throws Exception {			
		XnatSubjectdata subj1=getSubject();
		
		String id=RandomStringUtils.randomAlphanumeric(6);
		XnatMrsessiondata mr=new XnatMrsessiondata((UserI)user);
		mr.setId(id);
		mr.setLabel(id);
		mr.setProject(getProject().getId());
		mr.setSubjectId(subj1.getId());
		mr.setProperty("xnat:mrSessionData/scans/scan[@xsi:type=xnat:mrScanData][0]/ID", "x1");
		mr.setProperty("xnat:mrSessionData/scans/scan[@xsi:type=xnat:mrScanData][0]/type", "v1");
		SaveItemHelper.authorizedSave(mr, user, false, true, EventUtils.TEST_EVENT(user));
		
		Thread.sleep(1);

		mr.setProperty("xnat:mrSessionData/scans/scan[@xsi:type=xnat:mrScanData][0]/type", "v2");
		SaveItemHelper.authorizedSave(mr, user, false, false, EventUtils.TEST_EVENT(user));
		
		Thread.sleep(1);

		mr.setProperty("xnat:mrSessionData/scans/scan[@xsi:type=xnat:mrScanData][0]/type", "v3");
		SaveItemHelper.authorizedSave(mr, user, false, false, EventUtils.TEST_EVENT(user));
		
		Thread.sleep(1);

		mr.setProperty("xnat:mrSessionData/scans/scan[@xsi:type=xnat:mrScanData][0]/type", "v4");
		SaveItemHelper.authorizedSave(mr, user, false, false, EventUtils.TEST_EVENT(user));
		
		Thread.sleep(1);
		
		FlattenedItemI fi=ItemMerger.merge(ItemPropBuilder.build(mr.getItem().getCurrentDBVersion(), FlattenedItemA.GET_ALL, new ArrayList<FlattenedItemModifierI>()));
			
		ChildCollection cc=fi.getChildCollections().get("scans/scan");
		
		assertNotNull(cc);
		
		assertEquals(1,cc.getChildren().size());

		FlattenedItemI original=cc.getChildren().get(0);
		assertEquals(3,original.getHistory().size());
		
		List<FlattenedItemI> flattened=ChangeSummaryBuilderA.flatten(original);
		
		assertEquals("v1",flattened.get(0).getFields().getParams().get("type"));
		assertEquals("v2",flattened.get(1).getFields().getParams().get("type"));
		assertEquals("v3",flattened.get(2).getFields().getParams().get("type"));
		assertEquals("v4",flattened.get(3).getFields().getParams().get("type"));
	}
}
