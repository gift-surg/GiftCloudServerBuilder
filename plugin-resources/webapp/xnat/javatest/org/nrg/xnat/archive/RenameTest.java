package org.nrg.xnat.archive;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nrg.xdat.XDATTool;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.security.SecurityManager;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.XFT;
import org.nrg.xft.db.DBItemCache;
import org.nrg.xft.security.UserI;

public class RenameTest {

	
	final File move_src = new File("./ExptRenameMoveTest");
	final File move_dest = new File("./ExptRenameMoveTest2");
	final File copy_src = new File("./ExptRenameCopyTest");
	final File copy_dest = new File("./ExptRenameCopyTest2");
	
	final File archive = new File("./archive");


	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
		deleteDirNoException(move_src);
		deleteDirNoException(move_dest);
		deleteDirNoException(copy_src);
		deleteDirNoException(copy_dest);
	}
	
	public void deleteDirNoException(File s) throws Exception{
		if(s.exists())org.apache.commons.io.FileUtils.deleteDirectory(s);
	}

	@Test
	public void testMove()  throws Exception{
		move_src.mkdir();
		
		final String content ="SFSDFSDFDSFSDFDSFSDFDSFSDFSDFDSF";
		
		final File child=new File(move_src,"TEST.txt");
		org.apache.commons.io.FileUtils.writeStringToFile(child, content);
				
		final Rename rnm=new Rename();
		rnm.move(move_src, move_dest);
		
		org.junit.Assert.assertFalse((child.exists()));

		final File child2=new File(move_dest,"TEST.txt");
		org.junit.Assert.assertEquals(content, FileUtils.readFileToString(child2));
	}

	@Test
	public void testCopy() throws Exception{
		copy_src.mkdir();
		
		System.out.println(copy_src.getAbsolutePath());
		
		final String content ="SFSDFSDFDSFSDFDSFSDFDSFSDFSDFDSF";
		
		final File child=new File(copy_src,"TEST.txt");
		org.apache.commons.io.FileUtils.writeStringToFile(child, content);
		
		final Rename rnm=new Rename();
		rnm.copy(copy_src, copy_dest);
		
		org.junit.Assert.assertTrue((child.exists()));

		final File child2=new File(copy_dest,"TEST.txt");
		org.junit.Assert.assertEquals(FileUtils.readFileToString(child), FileUtils.readFileToString(child2));
	}
	

	@Test
	public void testModifyResource() throws Throwable{
		final File session_dir=new File("test/mr1/");
		final File session_dir2=new File("test/mr2/");
		final String snapshot = "SCANS/1/SNAPSHOTS/x.gif";
		
		final XnatResource res=new XnatResource((UserI)admin_user);
		res.setUri((new File(session_dir,snapshot)).getAbsolutePath());
		
		final SecurityManager sm= SecurityManager.GetInstance();
		final DBItemCache cache = new DBItemCache();
		
		final Rename rnm=new Rename();
		rnm.modifyResource(res, session_dir.toURI(), session_dir2.getAbsolutePath(), null, sm, cache);
		
		final String new_path=new File(session_dir2,snapshot).getAbsolutePath();
		
		//should be changed
		org.junit.Assert.assertEquals(new File(session_dir2,snapshot).toURI(),new File(res.getUri()).toURI());
	}

	@Test
	public void testModifyResourceOutsideSession() throws Throwable{
		final File session_dir=new File("test/mr1/");
		final File session_dir2=new File("test/mr2/");
		final File session_dir3=new File("test/mr3/");
		final String snapshot = "SCANS/1/SNAPSHOTS/x.gif";
		
		final XnatResource res=new XnatResource((UserI)admin_user);
		res.setUri((new File(session_dir3,snapshot)).getAbsolutePath());
		
		final SecurityManager sm= SecurityManager.GetInstance();
		final DBItemCache cache = new DBItemCache();
		
		final Rename rnm=new Rename();
		rnm.modifyResource(res, session_dir.toURI(), session_dir2.getAbsolutePath(), null, sm, cache);
		
		final String new_path=new File(session_dir2,snapshot).getAbsolutePath();
		
		//should be unchanged
		org.junit.Assert.assertEquals(new File(session_dir3,snapshot).toURI(),new File(res.getUri()).toURI());
		
		org.junit.Assert.assertEquals(0,cache.getStatements().size());
	}

	
	/*************************************
	 * The rest is copied from BaseXDATTestCase and should be removed when this is integrated into more recent development
	 */

	static XDATTool tool=null;
	public static XDATUser admin_user=null;
	public static XDATUser user=null;
	private static final String XNAT_INSTANCE_FOLDER = "deployments/xnat/";
	private static final String USER = "tolsen";
	private static final String PASS = "tolsen";
	
	private static final String ADMIN_USER = "admin";
	private static final String ADMIN_PASS = "admin";

	@BeforeClass
	public static void setUpBeforeClassInit() throws Exception {
		XFT.VERBOSE=true;
		
		if(tool==null){
			File file = new File(XNAT_INSTANCE_FOLDER + "InstanceSettings.xml");
			if(file.exists())
				tool = new XDATTool(XNAT_INSTANCE_FOLDER);
			else
				throw new FileNotFoundException(XNAT_INSTANCE_FOLDER + "InstanceSettings.xml");
		}

		
		admin_user=new XDATUser(ADMIN_USER);
		admin_user.login(ADMIN_PASS);
		
		try {
			XDATUser temp=new XDATUser(USER);
			temp.login(PASS);
			
			user=temp;
		} catch (Exception e) {
		}
	}
}
