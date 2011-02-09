package org.nrg.xnat.helpers.prearchive;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.SyncFailedException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nrg.xdat.bean.XnatImagesessiondataBean;
import org.nrg.xnat.helpers.prearchive.FileSystemSessionDataModifier.Move;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class FileSystemSessionDataModifierTest {
	static Move copyException, setXmlException, writeXmlException;
	static String base = "/home/aditya/Java/PRE_ARCHIVE_NEW/";
	static String basePath = base + "tmp/";
	static String newProj = "proj_101";
	static String sess = "0000101_PIB1";
	static String timestamp = "20110110_205530";
	static String oldProj = "proj_100";
	static String oldProjDir = basePath + oldProj;
	static String newProjDir = basePath + newProj;
	static String tsdir = basePath + oldProj + "/" + timestamp;
	static String newTsdir = basePath + newProj + "/" + timestamp;
	static String uri = tsdir + "/" + sess;
	static String xml = uri + ".xml";
	static FileSystemSessionDataModifier fs;
	static FileSystemSessionDataModifier.Move move;
	static FileSystemSessionDataModifier.Move.Copy copy;
	static FileSystemSessionDataModifier.Move.SetXml setXml;
	static FileSystemSessionDataModifier.Move.WriteXml writeXml;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		fs = new FileSystemSessionDataModifier(basePath);
		move = new FileSystemSessionDataModifier.Move(basePath,sess,uri,newProj);
		copy = move.new Copy(new File(tsdir), new File(newTsdir), sess);
		setXml = move.new SetXml(new File(xml), newProj,new File(newTsdir, sess).getAbsolutePath());
		copyException = new Move(basePath,sess,uri,newProj) {
			{
				this.copy = new Except(tsdir, newTsdir, sess);
			}
			class Except extends Copy{
				public Except (File tsdir, File newTsdir, String sess){
					super(tsdir,newTsdir,sess);
				}
				public java.lang.Void run() throws SyncFailedException {
					throw new SyncFailedException("Test Exception");
				}
			}
		};
		setXmlException = new Move(basePath,sess,uri,newProj) {
			{
				this.setXml = new Except(xml,newProj);
			}
			class Except extends SetXml {
				public Except (File xml, String sess){
					super(xml,sess,null);
				}
				public XnatImagesessiondataBean run () throws SyncFailedException {
					throw new SyncFailedException("Test exception");
				}
			}
		};
		writeXmlException = new Move(basePath,sess,uri,newProj) {
			{
				this.writeXml = new Except(tsdir,sess,xml);
			}
			class Except extends WriteXml {
				public Except (File tsdir, String sess, File xml){
					super(tsdir, sess);
				}
				public java.lang.Void run () throws SyncFailedException {
					throw new SyncFailedException("Test exception");
				}
			}
		};
	}
	@Before
	public final void setupTmp () {
		//create tmp space	
		new File(basePath + oldProj + "/" + timestamp).mkdirs();
		// copy project to tmp space 
		try {
			FileUtils.copyDirectoryToDirectory(new File(base + oldProj + "/" + timestamp + "/" + sess), 
					                		   new File(basePath + oldProj + "/" + timestamp + "/")); 
			FileUtils.copyFileToDirectory(new File(base + oldProj + "/" + timestamp + "/" + sess + ".xml"),
					                      new File(basePath + oldProj + "/" + timestamp + "/"));
		} catch (IOException e) {
			fail(e.getMessage());
		}
	}
	
	@After
	public final void deleteTmp () {
		//delete tmp space
		try {
			FileUtils.deleteDirectory(new File(basePath));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public final void testCopyRollback() {
		FileSystemSessionDataModifier fs = new FileSystemSessionDataModifier(basePath);
		try {
			fs._move(copyException);
			fail("Should have thrown a SyncFailedException");
		}
		catch (SyncFailedException e) {
			// check that the rollback worked.
			boolean newDirExists = new File(basePath + newProj + "/" + timestamp + "/" + sess).exists();
			Assert.assertFalse(newDirExists);
		}
	}
	@Test
	public final void testSetXmlRollback() {
		FileSystemSessionDataModifier fs = new FileSystemSessionDataModifier(basePath);
		try {
			fs._move(setXmlException);
			fail("Should have thrown a SyncFailedException");
		}
		catch (SyncFailedException e) {
			// check that the rollback worked.
			boolean baseDirExists = new File(basePath + newProj).exists();
			boolean newDirExists = new File(basePath + newProj + "/" + timestamp + "/" + sess).exists();
			Assert.assertFalse(newDirExists);
			Assert.assertTrue(baseDirExists);
		}
	}
	@Test
	public final void testWriteXmlRollback() {
		FileSystemSessionDataModifier fs = new FileSystemSessionDataModifier(basePath);
		try {
			fs._move(writeXmlException);
			fail("Should have thrown a SyncFailedException");
		}
		catch (SyncFailedException e) {
			// check that the rollback worked.
			boolean baseDirExists = new File(basePath + newProj).exists();
			boolean newDirExists = new File(basePath + newProj + "/" + timestamp + "/" + sess).exists();
			Assert.assertFalse(newDirExists);
			File f = new File(basePath + newProj + "/" + timestamp + "/" + sess + ".xml");
			boolean xmlExists = f.exists();
			Assert.assertFalse(xmlExists);
			Assert.assertTrue(baseDirExists);
		}
	}
	@Test 
	public final void testMove() {
		try {
			fs._move(new Move(basePath,sess,uri,newProj));
		} catch (SyncFailedException e) {
			fail("SyncFailedException thrown " + e);
		}
		File newXml = new File(newTsdir + "/" + sess + ".xml");
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		Document doc = null;
		try {
			docBuilder = docFactory.newDocumentBuilder();
			doc = docBuilder.parse(newXml);
		} catch (ParserConfigurationException e) {
			fail("Exception thrown " + e.getMessage());
		} catch (SAXException e) {
			fail("Exception thrown " + e.getMessage());
		} catch (IOException e) {
			fail("Exception thrown " + e.getMessage());
		}
		
		Element e = doc.getDocumentElement();
		e.normalize();
		Assert.assertEquals(newProj, e.getAttribute("project"));
	}
}
