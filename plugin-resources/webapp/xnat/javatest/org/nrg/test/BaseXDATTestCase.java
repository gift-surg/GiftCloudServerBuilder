/*
 * org.nrg.test.BaseXDATTestCase
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.test;

import org.junit.BeforeClass;
import org.nrg.xdat.XDATTool;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.XFT;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public abstract class BaseXDATTestCase{
	static XDATTool tool=null;
	public static XDATUser admin_user=null;
	public static XDATUser user=null;
	private static final String XNAT_INSTANCE_FOLDER = "deployments/xnat/";
	private static final String USER = "testuser";
	private static final String PASS = "testuser";
	
	private static final String ADMIN_USER = "admin";
	private static final String ADMIN_PASS = "admin";

	public BaseXDATTestCase() {
		super();
	}

	@BeforeClass
	public static void setUpBeforeClassInit() throws Exception {
		init();
		
		XDATUser temp=new XDATUser(USER);
		temp.login(PASS);
		
		user=temp;
		
		admin_user=new XDATUser(ADMIN_USER);
		admin_user.login(ADMIN_PASS);
	}
	
	public static void init() throws Exception {
		XFT.VERBOSE=true;
		
		if(tool==null){
			File file = new File(XNAT_INSTANCE_FOLDER + "InstanceSettings.xml");
			if(file.exists())
				tool = new XDATTool(XNAT_INSTANCE_FOLDER);
			else
				throw new FileNotFoundException(XNAT_INSTANCE_FOLDER + "InstanceSettings.xml");
		}
	}
	
	public static File createFile(File dir, String name, String content) throws IOException{
		if(!dir.exists())dir.mkdirs();
		File f=new File(dir,name);
		org.apache.commons.io.FileUtils.writeStringToFile(new File(dir,name), content);
		return f;
	}
	
	public static String get(File dir, String name) throws IOException{
		return org.apache.commons.io.FileUtils.readFileToString(new File(dir,name));
	}
}