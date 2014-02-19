/*
 * org.nrg.xnat.helpers.uri.UriParserUtilsTest
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.helpers.uri;

import org.junit.Test;
import org.nrg.xnat.helpers.uri.UriParserUtils.UriParser;
import org.restlet.util.Template;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;


public class UriParserUtilsTest {
	@Test
	public void testURI() throws Exception{
		System.out.println((new UriParser("/prearchive/projects/{PROJECT_ID}/{TIMESTAMP}",Template.MODE_STARTS_WITH)).readUri("/prearchive/projects/X"));
	}

	@Test
	public void testBadURI() throws Exception{
		try {
			UriParserUtils.parseURI("/sdfsdf");
			fail("Should not have succeeded.");
		} catch (MalformedURLException e) {
			
		}
	}


	@Test
	public void testValidURIs() throws Exception{
		class URIe{
			public final String s; 
			public final Class clazz;
			public final int variables;
			public URIe(String s, Class clazz,int variables){
				this.s=s;
				this.clazz=clazz;
				this.variables=variables;
			}
		}
		List<URIe> uris=new ArrayList<URIe>(){{
			add(new URIe("/archive/projects/X/experiments/e".intern(),URIManager.ArchiveURI.class,2));
			add(new URIe("/archive/projects/X/subjects/s".intern(),URIManager.ArchiveURI.class,2));
			add(new URIe("/archive/projects/X/subjects/s/experiments/e".intern(),URIManager.ArchiveURI.class,3));
			add(new URIe("/archive/projects/x/subjects/s/experiments/a/assessors/e".intern(),URIManager.ArchiveURI.class,4));
			add(new URIe("/archive/projects/x/subjects/s/experiments/a/scans/s".intern(),URIManager.ArchiveURI.class,4));
			add(new URIe("/archive/projects/x/subjects/s/experiments/a/reconstructions/r".intern(),URIManager.ArchiveURI.class,4));
			add(new URIe("/archive/projects/x".intern(),URIManager.ArchiveURI.class,1));
			add(new URIe("/archive".intern(),URIManager.ArchiveURI.class,0));
		
			add(new URIe("/archive/experiments/e".intern(),URIManager.ArchiveURI.class,1));
			add(new URIe("/archive/experiments/a/scans/s".intern(),URIManager.ArchiveURI.class,2));
			add(new URIe("/archive/experiments/a/reconstructions/r".intern(),URIManager.ArchiveURI.class,2));
			add(new URIe("/archive/experiments/a/assessors/r".intern(),URIManager.ArchiveURI.class,2));
			add(new URIe("/archive/subjects/s".intern(),URIManager.ArchiveURI.class,1));
			add(new URIe("/prearchive/projects/p/t/s".intern(),URIManager.PrearchiveURI.class,3));
			add(new URIe("/prearchive/projects/p/t".intern(),URIManager.PrearchiveURI.class,2));
			add(new URIe("/prearchive/projects/p".intern(),URIManager.PrearchiveURI.class,1));
			add(new URIe("/prearchive".intern(),URIManager.PrearchiveURI.class,0));
		}};
		
		for(URIe e:uris){
			System.out.println(e.s);
			URIManager.DataURIA _return=UriParserUtils.parseURI(e.s);
			assertTrue("" + _return.getClass().getName() + " should be a " + e.clazz.getName(),e.clazz.isInstance(_return));
			assertEquals(e.s,_return.getProps().size(),e.variables);
		}
	}
}
