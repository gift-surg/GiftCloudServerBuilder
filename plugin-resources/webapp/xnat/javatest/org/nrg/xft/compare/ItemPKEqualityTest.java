/**
 * Copyright 2010 Washington University
 */
package org.nrg.xft.compare;

import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nrg.test.BaseXDATTestCase;
import org.nrg.xft.XFTItem;

/**
 * @author timo
 *
 */
public class ItemPKEqualityTest extends BaseXDATTestCase{
	
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
		
		newI.setProperty("ID", "DSCDSCDSC");
		oldI.setProperty("ID", "DSCDSCDSC");
		
		final ItemEqualityI equalizer=new ItemPKEquality();
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
		
		newI.setProperty("ID", "DSCDSCDsdfSC");
		oldI.setProperty("ID", "DSCDSCDSC");
		
		final ItemEqualityI equalizer=new ItemPKEquality();
		if(equalizer.isEqualTo(newI, oldI)){
			fail("These should not be equal.");
		}
	}

}
