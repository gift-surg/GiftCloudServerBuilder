/*
 * org.nrg.xft.compare.ItemUniqueEqualityTest
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xft.compare;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nrg.test.BaseXDATTestCase;
import org.nrg.xft.XFTItem;

import static org.junit.Assert.fail;

/**
 * @author timo
 *
 */
public class ItemUniqueEqualityTest  extends BaseXDATTestCase{
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link org.nrg.xft.compare.ItemPKEquality#doCheck(org.nrg.xft.XFTItem, org.nrg.xft.XFTItem)}.
	 */
	@Test
	public void testDoCheck() throws Exception{
		final XFTItem newI=XFTItem.NewItem("xnat:mrSessionData", user);
		final XFTItem oldI=XFTItem.NewItem("xnat:mrSessionData", user);
		
		newI.setProperty("project", "DSCDSCDSC");
		newI.setProperty("label", "DSCDSCDSC1");
		
		oldI.setProperty("project", "DSCDSCDSC");
		oldI.setProperty("label", "DSCDSCDSC1");
		
		final ItemEqualityI equalizer=new ItemUniqueEquality();
		if(!equalizer.isEqualTo(newI, oldI)){
			fail("These should be equal.");
		}
	}

	/**
	 * Test method for {@link org.nrg.xft.compare.ItemPKEquality#doCheck(org.nrg.xft.XFTItem, org.nrg.xft.XFTItem)}.
	 */
	@Test
	public void testDoCheck2() throws Exception{
		final XFTItem newI=XFTItem.NewItem("xnat:mrSessionData", user);
		final XFTItem oldI=XFTItem.NewItem("xnat:mrSessionData", user);
		
		newI.setProperty("project", "DSCDSCsdfsDSC");
		newI.setProperty("label", "DSCDSCDSC1");
		
		oldI.setProperty("project", "DSCDSCDSC");
		oldI.setProperty("label", "DSCDSCDSC1");
		
		final ItemEqualityI equalizer=new ItemUniqueEquality();
		if(equalizer.isEqualTo(newI, oldI)){
			fail("These not should be equal.");
		}
	}

	/**
	 * Test method for {@link org.nrg.xft.compare.ItemPKEquality#doCheck(org.nrg.xft.XFTItem, org.nrg.xft.XFTItem)}.
	 */
	@Test
	public void testDoCheck3() throws Exception{
		final XFTItem newI=XFTItem.NewItem("xnat:mrSessionData", user);
		final XFTItem oldI=XFTItem.NewItem("xnat:mrSessionData", user);
		
		newI.setProperty("project", "DSCDSCDSC");
		newI.setProperty("label", "DSCDSCsdfsDSC1");
		
		oldI.setProperty("project", "DSCDSCDSC");
		oldI.setProperty("label", "DSCDSCDSC1");
		
		final ItemEqualityI equalizer=new ItemUniqueEquality();
		if(equalizer.isEqualTo(newI, oldI)){
			fail("These not should be equal.");
		}
	}

}
