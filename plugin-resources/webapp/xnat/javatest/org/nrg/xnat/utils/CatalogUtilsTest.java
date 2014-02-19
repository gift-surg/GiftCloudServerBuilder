/*
 * org.nrg.xnat.utils.CatalogUtilsTest
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.utils;

import org.junit.Test;
import org.nrg.xdat.bean.CatCatalogBean;
import org.nrg.xdat.model.CatEntryI;
import org.nrg.xnat.utils.CatalogUtils.CatEntryFilterI;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class CatalogUtilsTest {
	@Test
	public void testAddRemoveFiles() throws Exception{
		final File src = new File("./CatalogUtilsTest/test_catalog.xml");
		final File target = new File("./CatalogUtilsTest/test/dummy/dummy.txt");
		final File target2 = new File("./CatalogUtilsTest/test/dummy/dummy2.txt");
		
		target.getParentFile().mkdirs();
		target.createNewFile();
		target2.createNewFile();

		CatCatalogBean cat = new CatCatalogBean();
		CatalogUtils.writeCatalogToFile(cat, src);
		
		if(!CatalogUtils.addUnreferencedFiles(src, cat, null, null)){
			fail("CatalogUtils.addUnreferencedFiles failed to find unreferenced file");
		}

		CatalogUtils.writeCatalogToFile(cat, src);
		cat=CatalogUtils.getCatalog(src);
		
		assertEquals("Expected CatalogUtils.addUnreferencedFiles to find 2 files",CatalogUtils.getEntriesByFilter(cat, new CatEntryFilterI(){
			@Override
			public boolean accept(CatEntryI entry) {
				return true;
			}}).size(),2);
		
		
		if(CatalogUtils.getEntryByURI(cat, "test/dummy/dummy.txt")==null){
			fail("CatalogUtils.addUnreferencedFiles failed to add unreferenced file");
			return;
		}

		//delete file, so it can be removed by second formalizeCatalog call
		target.delete();
		
		CatalogUtils.writeCatalogToFile(cat, src);
		cat=CatalogUtils.getCatalog(src);
		
		CatalogUtils.formalizeCatalog(cat, src.getParent(), null, null, false, false);

		if(CatalogUtils.getEntryByURI(cat, "test/dummy/dummy.txt")==null){
			fail("CatalogUtils.formalizeCatalog should not have deleted missing entry yet");
			return;
		}

		CatalogUtils.writeCatalogToFile(cat, src);
		cat=CatalogUtils.getCatalog(src);
		
		CatalogUtils.formalizeCatalog(cat, src.getParent(), null, null, false, true);
		
		if(CatalogUtils.getEntryByURI(cat, "test/dummy/dummy.txt")!=null){
			fail("CatalogUtils.formalizeCatalog failed to delete missing entry");
			return;
		}
		
		src.getParentFile().delete();
	}
}
