/*
 * org.nrg.xnat.helpers.merge.CopyOpTest
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.helpers.merge;


import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nrg.transaction.RollbackException;
import org.nrg.transaction.Run;
import org.nrg.transaction.TransactionException;

import java.io.*;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class CopyOpTest {
	static File tmpDir = new File(System.getProperty("java.io.tmpdir"));
	static File dirA;
	static File backupDir;
	static File dirB;

	@Before
	public void setUp() throws Exception {
		dirA = new File(tmpDir,"a");
		if (dirA.exists()) {
			FileUtils.deleteDirectory(dirA);
		}
		
		backupDir = new File(tmpDir,"backup");
		if (backupDir.exists()) {
			FileUtils.deleteDirectory(backupDir);
		}
		dirA.mkdirs();
		backupDir.mkdirs();
		addToFile("Hello Java");
	}	

	@After
	public void tearDown() throws Exception {
		if (dirA != null) {
			FileUtils.deleteDirectory(dirA);
		}
		if (backupDir != null) {
			FileUtils.deleteDirectory(backupDir);
		}
	}
	
	public void addToFile(String l) throws IOException {
		FileWriter fstream = new FileWriter(new File(dirA, "out.txt"),false);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write(l);
		out.close();
	}
	
	
	
	public String readFile() throws IOException{
		StringBuffer fileData = new StringBuffer(1000);
		BufferedReader reader = new BufferedReader(
				new FileReader(new File(dirA,"out.txt")));
		char[] buf = new char[1024];
		int numRead=0;
		while((numRead=reader.read(buf)) != -1){
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
			buf = new char[1024];
		}
		reader.close();
		return fileData.toString();
	}
	
	@Test
	public final void successfulTest() {
		Callable<Void> c = new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				addToFile("Goodbye Java");
				return null;
			}
			
		};
		CopyOp<Void> op = new CopyOp<Void>(c,dirA,backupDir,"src_dir");
		
		try {
			Run.runTransaction(op);
		} catch (RollbackException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransactionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String fileContents = null;
		try {
			fileContents = readFile();
		} catch (IOException e) {
			fail("");
		}
		assertEquals(fileContents,"Goodbye Java");
	}
	
	@Test
	public final void unSuccessfulTest() {
		Callable<Void> c = new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				addToFile("Goodbye Java");
				throw new Exception();
			}
			
		};
		CopyOp<Void> op = new CopyOp<Void>(c,dirA,backupDir,"src_dir");
		
		try {
			Run.runTransaction(op);
		} catch (RollbackException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransactionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String fileContents = null;
		try {
			fileContents = readFile();
		} catch (IOException e) {
			fail("");
		}
		assertEquals(fileContents,"Hello Java");
	}
}
