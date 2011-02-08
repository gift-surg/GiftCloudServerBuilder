package org.nrg.xnat.helpers.prearchive;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.SyncFailedException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nrg.xnat.helpers.prearchive.PrearcUtils.PrearcStatus;

public class PrearcDatabaseTest {

	private static SessionDataDelegate sd;
	private static int numSessions = 1000;
	private static ArrayList<String> test_projects = new ArrayList<String>(Arrays.asList(new String[]{"proj_0", "proj_1", "proj_2", "proj_3", PrearcUtils.COMMON}));
	private static Collection<SessionData> sessions;
	
	/**
	 * Iterate through a Collection, when the end is 
	 * reached start over 
	 * @author aditya
	 *
	 * @param <T>
	 */
	static abstract class Cycle<T>{
		Collection<T> c;
		Iterator<T> i;
		public Cycle (Collection<T> c) {
			this.c = c;
			this.i = c.iterator();
		}
		public T next() {
			if (!this.i.hasNext() && this.c.size() != 0) {
				this.i = this.c.iterator();
			}
			T ret = i.next();
			return ret;
		}
	}
	
	/**
	 * Generate the next T on demand.
	 * @author aditya
	 *
	 * @param <T>
	 */
	static abstract class Infinite<T>{
		private T prefix;
		private int counter;
		public Infinite(T prefix){
			this.prefix = prefix;
			this.counter = 0;
		}
		public Infinite(T prefix, int counter) {
			this.prefix = prefix;
			this.counter = counter;
		}
		public abstract T next();
	}
	
	/**
	 * Return a random T from a collection
	 * @author aditya
	 *
	 * @param <T>
	 */
	static abstract class Any<T>{
		private Collection<T> c;
		public Any(Collection<T> c) {
			this.c = c;
		}
		public abstract T any();
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		SessionDataProducerI sp = new SessionDataProducerI() {
			@Override
			/**
			 * Create a test set of sessions
			 */
			public Collection<SessionData> get() {				
				ArrayList<SessionData> ss = new ArrayList<SessionData>();
				
				// There are a finite set of projects in the set that get cycled over. 
				// This way we can guarantee that all projects are used. Sessions and timestamps 
				// are generated sequentially and subjects are chosen at random from a pre-defined pool.
				Cycle<String> projectCycler = new Cycle<String>(PrearcDatabaseTest.test_projects){};
				Infinite<String> sessNameGen = new Infinite<String>("sess_"){
					public String next() {
						String s = super.prefix + super.counter;
						super.counter++;
						return s;
					}
				};
				
				Infinite<Integer> timestampGen = new Infinite<Integer>(1000){
					public Integer next() {
						Integer s = super.prefix + super.counter;
						super.counter++;
						return s;
					}
				};
				
				Any<String> anySubj = new Any<String>(new ArrayList<String>(Arrays.asList(RandomPatientInfo.randomNames("subj_", numSessions / 4)))){
					public String any() {
						String [] s = super.c.toArray(new String[super.c.size()]);
						return s[(RandomPatientInfo.randomIndex(s))];
					}
				};
				
				// Create the test data set. Project, timestamp, subject and label are 
				// generated as describe above. The rest of the attributes
				// are filler and generated randomly.
				for (int i = 0; i < numSessions; i++) {
					SessionData s = new SessionData();
					s.setFolderName(sessNameGen.next())
					 .setProject(projectCycler.next())
					 .setTimestamp(timestampGen.next().toString())
					 .setSubject(anySubj.any())
					 .setScan_date(RandomPatientInfo.randomDate())
					 .setUploadDate(RandomPatientInfo.after(s.getScan_date()))
					 .setLastBuiltDate(RandomPatientInfo.after(s.getUploadDate()))
					 .setScan_time(RandomPatientInfo.randomTimestamp())
					 .setUrl(StringUtils.join(new String[]{"/tmp/".intern(),s.getTimestamp(),"/".intern(),s.getFolderName()}));
					if ("Unassigned" == s.getProject()) {
						s.setStatus(PrearcUtils.PrearcStatus.ERROR);
					}
					else {
						s.setStatus(RandomPatientInfo.randomStatus());
					}
					ss.add(s);
				}
				return ss;
			}
		};
		// For testing there is no permanent store so this object does nothing 
		SessionDataModifierI sm = new SessionDataModifierI() {
			public void move(SessionData s, String newProj) {}
			public void delete(SessionData sd) {}
			public void setStatus(SessionData sd, PrearcStatus status) {}
		};		
		PrearcDatabaseTest.sd = new SessionDataDelegate(sp,sm) {};
		PrearcDatabaseTest.sessions = sd.get();
		try {
			PrearcDatabase.initDatabase("/home/aditya/Java/PRE_ARCHIVE_NEW/", PrearcDatabaseTest.sd);
		} catch (SessionException e) {
			fail("SessionException " + e);
		}
	}	
	
	@Before
	public void reinitDatabase () {
		try {
			PrearcDatabase.refresh();
		} catch (IllegalStateException e) {
			fail("IllegalStateException " + e);
		} catch (SQLException e) {
			fail("SQLException " + e);
		} catch (IOException e) {
			fail("IOException " + e);
		} catch (SessionException e) {
			fail("SessionException " + e);
		}
	}
	

	@Test
	public final void testGetUnassigned () {
		String unassignedUri = "prearchive/projects/Unassigned";
		List<SessionData> projs = null;
		try {
			projs = PrearcDatabase.getProjects(unassignedUri);
		}
		catch (SQLException e) {
			fail ("SQLException" + e.getMessage());
		}
		catch (SessionException e) {
			fail ("SessionException" + e.getMessage());
		}
		int numUnassigned = 0;
		Iterator<SessionData> i = PrearcDatabaseTest.sessions.iterator();
		while (i.hasNext()){
			SessionData s = i.next();
			if (s.getProject().equals(PrearcUtils.COMMON)) {
				numUnassigned++;
			}
		}
		assert(projs.size() == numUnassigned);
	}


	/**
	 * Count up the number of sessions for each project in the given collection
	 */
	private Map<String,Integer> countSessions (Collection<SessionData> ss) {
		Map<String, Integer> sessionCount = new HashMap<String,Integer>();
		Iterator<SessionData> i = ss.iterator();
		while(i.hasNext()){
			String proj = i.next().getProject();
			if (!sessionCount.containsKey(proj)){
				sessionCount.put(proj, 0);
			}
			else {
				sessionCount.put(proj, sessionCount.get(proj) + 1);
			}
		}
		return sessionCount;
	}

	@Test
	public final void testGetProjects() {
		// construct a uri with all projects.
		String allProjUri = "prearchive/projects/" + StringUtils.join(test_projects.toArray(new String[test_projects.size()]), ",");

		// get the projects
		List<SessionData> projs = null;
		try {
			projs = PrearcDatabase.getProjects(allProjUri);
		}
		catch (SQLException e) {
			fail ("SQLException" + e.getMessage());
		}
		catch (SessionException e) {
			fail ("SessionException" + e.getMessage());
		}
		
		// map each project in the database to the number of sessions in that project
		Map<String, Integer> cache = countSessions(projs);
		
		// map each project in the local store to the number of sessions in that project
		Map<String, Integer> local = countSessions(PrearcDatabaseTest.sessions);
		
		// check that the database matches the local store
		Iterator<String> localKeys = local.keySet().iterator();
		while(localKeys.hasNext()) {
			String proj = localKeys.next();
			//check that database has this project
			Assert.assertTrue(cache.containsKey(proj));
			// and check that the number of sessions in the database matches the local store
			Assert.assertEquals(cache.get(proj), local.get(proj));
		}
	}

	@Test
	public final void testGetSession() {
		this.getExistingSession();
		this.getNonExistingSession();
	}
	
	public final void getExistingSession() {
		String uri = "prearchive/projects/proj_0/1000/sess_0";
		SessionData s = null;
		try {
			s = PrearcDatabase.getSession(uri);
		}
		catch (SQLException e) {
			fail ("SQLException " + e.getMessage());
		}
		catch (SessionException e) {
			fail ("SessionException " + e.getMessage());
		}
		
		Assert.assertEquals("proj_0", s.getProject());
		Assert.assertEquals("1000", s.getTimestamp());
		Assert.assertEquals("sess_0", s.getFolderName());
	}
	
	public final void getNonExistingSession() {
		// test that a non-existent project throws a SessionException
		String uri = "prearchive/projects/nonExistentProj/nonExistentTimestamp/nonExistentSession";
		try {
			PrearcDatabase.getSession(uri);
			fail("Should have thrown a SessionException");
		}
		catch (SQLException e) {
			fail ("SQLException " + e.getMessage());
		}
		catch (SessionException e) {
		}
	}
	

	
	@Test
	public final void testSessionLock () {
		String uri = "prearchive/projects/proj_0/1000/sess_0";
		for (PrearcUtils.PrearcStatus s : PrearcUtils.PrearcStatus.values()) {
			// lock the session with the appropriate lock
			SessionData sd = null;
			try {
				PrearcDatabase.setStatus(uri, s);
				PrearcDatabase.lockSession(uri);
				sd = PrearcDatabase.getSession(uri);
				// the correct locked status is the current status with an '_' in front of it.
				if (s != PrearcStatus.READY && s != PrearcStatus.ERROR && s.toString().charAt(0) != '_') {
					// check that the session is locked with the correct value
					Assert.assertEquals(PrearcUtils.inProcessStatusMap.get(s).toString(), sd.getStatus().toString());
				}
				PrearcDatabase.unLockSession(uri);
			}
			catch (SQLException e) {
				fail("Threw a SQLException " + e);
			}
			catch (SessionException e) {
				fail("Threw a SessionException " + e);
			}
		}	
	}
	
	
	
	
	@Test
	public final void testSessionUnlock () {
		String uri = "prearchive/projects/proj_0/1000/sess_0";
		try {
			PrearcDatabase.setStatus(uri, PrearcUtils.PrearcStatus.BUILDING);
			PrearcDatabase.lockSession(uri);
			if (PrearcDatabase.isLocked(uri)) {
				PrearcDatabase.unLockSession(uri);
				Assert.assertFalse(PrearcDatabase.isLocked(uri));
			}
			else {
				fail ("Could not lock session");
			}
		} catch (SQLException e) {
			fail("Threw a SQLException " + e);
		} catch (SessionException e) {
			fail("Threw a SessionException " + e);
		}
	}
	
	@Test
	public final void testIsLocked() {
		String uri = "prearchive/projects/proj_0/1000/sess_0";
		try {
			PrearcDatabase.setStatus(uri,PrearcUtils.PrearcStatus.BUILDING);
			PrearcDatabase.lockSession(uri);
			Assert.assertTrue(PrearcDatabase.isLocked(uri));
		} catch (SQLException e) {
			fail("Threw a SQLException " + e);
		} catch (SessionException e) {
			fail("Threw a SessionException " + e);
		}
	}
	
	@Test
	public final void testMoveToProject() {
		// move a session to a new project
		String uri = "prearchive/projects/proj_0/1000/sess_0?dest=proj_newProj";
		try {
			Assert.assertTrue(PrearcDatabase.moveToProject(uri));
		}
		catch (SQLException e) {
			fail ("SQLException " + e.getMessage());
		}
		catch (SessionException e) {
			fail ("SessionException " + e.getMessage());
		} catch (Exception e) {
			fail ("Exception " + e.getMessage());
		}
		
		// retrieve the session and make sure that the project name is the new project
		String newUri = "prearchive/projects/proj_newProj/1000/sess_0";
		SessionData s = null;
		try {
			s = PrearcDatabase.getSession(newUri);
		}
		catch (SQLException e) {
			fail("Threw a SQLException");
		}
		catch (SessionException e) {
			fail("Threw a SessionException");
		}
		assert(s.getProject() == "proj_newProj");
	}
	
	
	
	@Test
	public final void testDeleteLockedSession () {
		String uri = "prearchive/projects/proj_0/1000/sess_0";
		try {
			PrearcDatabase.setStatus(uri, PrearcUtils.PrearcStatus.BUILDING);
			PrearcDatabase.lockSession(uri);
		} catch (SQLException e) {
			fail("Threw a SQLException " + e);
		} catch (SessionException e) {
			fail("Threw a SessionException " + e);
		}

		try {
			Assert.assertFalse(PrearcDatabase.deleteSession(uri));
		} catch (SQLException e) {
			fail("Threw a SQLException " + e);
		} catch (SessionException e) {
			fail("Threw a SessionException" + e);
		} catch (SyncFailedException e) {
			fail("Threw a SyncFailedException" + e);
		}
	}
	
	@Test
	public final void testMoveLockedSession () {
		String uri = "prearchive/projects/proj_0/1000/sess_0?dest=proj_newProj";
		try {
			PrearcDatabase.setStatus(uri, PrearcUtils.PrearcStatus.BUILDING);
			PrearcDatabase.lockSession(uri);
		} catch (SQLException e) {
			fail("Threw a SQLException " + e);
		} catch (SessionException e) {
			fail("Threw a SessionException " + e);
		}

		try {
			Assert.assertFalse(PrearcDatabase.moveToProject(uri));
		} catch (SQLException e) {
			fail("Threw a SQLException " + e);
		} catch (SessionException e) {
			fail("Threw a SessionException" + e);
		} catch (SyncFailedException e) {
			fail("Threw a SyncFailedException" + e);
		}
	}
		
	@Test
	public final void testSetStatus() {
		String uri = "prearchive/projects/proj_0/1000/sess_0";
		try {
			Assert.assertTrue(PrearcDatabase.setStatus(uri, PrearcUtils.PrearcStatus.ERROR));
		}
		catch (SQLException e) {
			fail("Threw a SQLException " + e);
		}
		catch (SessionException e) {
			fail("Threw a SessionException " + e);
		}
		SessionData s = null;
		try {
			s = PrearcDatabase.getSession(uri);
		}
		catch (SQLException e) {
			fail("Threw a SQLException " + e);
		}
		catch (SessionException e) {
			fail("Threw a SessionException " + e);
		}
		Assert.assertEquals(PrearcUtils.PrearcStatus.ERROR, s.getStatus());
	}
	
	@Test
	public final void testSetStatusLockedSession () {
		String uri = "prearchive/projects/proj_0/1000/sess_0";
		try {
			PrearcDatabase.setStatus(uri, PrearcUtils.PrearcStatus.BUILDING);
			PrearcDatabase.lockSession(uri);
			Assert.assertFalse(PrearcDatabase.setStatus(uri, PrearcUtils.PrearcStatus.ARCHIVING));
		} catch (SQLException e) {
			fail("Threw a SQLException " + e);
		} catch (SessionException e) {
			fail("Threw a SessionException " + e);
		}
	}

	@Test
	public final void testDeleteSession() {
		//delete a single session
		String uri = "prearchive/projects/proj_0/1000/sess_0";
		try {
			PrearcDatabase.deleteSession(uri);			
		}
		catch (SQLException e) {
			fail("Threw a SQLException " + e);
		}
		catch (SessionException e) {
			fail("Threw a SessionException " + e);
		} catch (SyncFailedException e) {
			fail ("Threw a SyncFailedException" + e);
		}
		try {
			PrearcDatabase.getSession(uri);
			fail("Should have thrown a SessionException");
		}
		catch (SQLException e) {}
		catch (SessionException e) {}
	}
	@Test
	public final void multipleDeleteSession () {
		// delete multiple sessions
		String uri = "prearchive/projects/proj_3";
		List<SessionData> projs = null;
		try {
			projs = PrearcDatabase.getProjects(uri);
		}
		catch (SQLException e) {
			fail ("SQLException" + e.getMessage());
		}
		catch (SessionException e) {
			fail ("SessionException" + e.getMessage());
		}
		Iterator<SessionData> di = projs.iterator();
		List<SessionDataTriple> ls = new ArrayList<SessionDataTriple>();
		while(di.hasNext()) {
			SessionData _s = di.next();
			ls.add(_s.getSessionDataTriple());
		}
		
		Map<SessionDataTriple,Boolean> m = null;
		try {
			m = PrearcDatabase.deleteSession(ls);
		} 
		catch (SyncFailedException e) {
			fail ("SyncFailedException " + e.getMessage());
		} 
		catch (SQLException e) {
			fail ("SQLException" + e.getMessage());
		}
		catch (SessionException e) {
			fail ("SessionException" + e.getMessage());
		}
		
		// make sure each one fails to retrieve
		// block for up to 3 seconds.
		Iterator<SessionDataTriple> li = ls.iterator();
		int counter = 3;
		boolean notFound = false;
		while(li.hasNext()){
			SessionDataTriple _s = li.next();
			while(counter != 0) {
			try {
				PrearcDatabase.getSession(_s.getFolderName(),_s.getTimestamp(),_s.getProject());
					notFound = false;
			} 
				catch (SQLException e) {
					fail ("SQLException" + e.getMessage());
				}
				catch (SessionException e) {
					notFound = true;
				}
				if (!notFound) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {}
					
					counter--;
				}
				else {
					break;
				}
			}
			Assert.assertTrue(notFound);
		}
	}
	
	@Test
	public final void testMoveToProjectWithSyncException () {
		SessionDataModifierI sm = new SessionDataModifierI() {
			public void setStatus(SessionData sd, PrearcStatus status) {}
			public void move(SessionData s, String newProj) throws SyncFailedException {
				throw new SyncFailedException("Test exception");				
			}
			public void delete(SessionData sd) throws SyncFailedException {}
		};
		PrearcDatabase.setSessionDataModifier(sm);
		
		String uri = "prearchive/projects/proj_0/1000/sess_0?dest=proj_newProj";
		try {
			Assert.assertTrue(PrearcDatabase.moveToProject(uri));
			fail("Should have thrown a SyncFailedException");
		}
		catch (SQLException e) {
			fail ("SQLException " + e.getMessage());
		}
		catch (SessionException e) {
			fail ("SessionException " + e.getMessage());
		} 
		catch (SyncFailedException e) {
			try {
				// make sure the session has been unlocked
				Assert.assertFalse(PrearcDatabase.isLocked(uri));
				// make sure that the session is in an ERROR state
				SessionData se = PrearcDatabase.getSession(uri);
				Assert.assertEquals(se.getStatus(), PrearcUtils.PrearcStatus.ERROR);
			}
			catch (SQLException f) {
				fail ("SQLException " + f.getMessage());
			}
			catch (SessionException f) {
				fail ("SessionException " + f.getMessage());
			}
		}
	}
	
	@Test
	public final void testDeleteSessionWithSyncException () {
		SessionDataModifierI sm = new SessionDataModifierI() {
			public void setStatus(SessionData sd, PrearcStatus status) {}
			public void move(SessionData s, String newProj) throws SyncFailedException {}
			public void delete(SessionData sd) throws SyncFailedException {
				throw new SyncFailedException("Test exception");				
			}
		};
		PrearcDatabase.setSessionDataModifier(sm);
		
		String uri = "prearchive/projects/proj_0/1000/sess_0";
		try {
			Assert.assertTrue(PrearcDatabase.deleteSession(uri));
			fail("Should have thrown a SyncFailedException");
		}
		catch (SQLException e) {
			fail ("SQLException " + e.getMessage());
		}
		catch (SessionException e) {
			fail ("SessionException " + e.getMessage());
		} 
		catch (SyncFailedException e) {
			try {
				// make sure the session has been unlocked
				Assert.assertFalse(PrearcDatabase.isLocked(uri));
				// make sure that the session is in an ERROR state
				SessionData se = PrearcDatabase.getSession(uri);
				Assert.assertEquals(se.getStatus(), PrearcUtils.PrearcStatus.ERROR);
			}
			catch (SQLException f) {
				fail ("SQLException " + f.getMessage());
			}
			catch (SessionException f) {
				fail ("SessionException " + f.getMessage());
			}
		}
	}
	
	@Test
	public final void testMarkSessions () {
		String uri = "prearchive/projects/proj_100";
		List<SessionData> projs = null;
		try {
			projs = PrearcDatabase.getProjects(uri);
		}
		catch (SQLException e) {
			fail ("SQLException" + e.getMessage());
		}
		catch (SessionException e) {
			fail ("SessionException" + e.getMessage());
		}
		Iterator<SessionData> di = projs.iterator();
		List<SessionDataTriple> ls = new ArrayList<SessionDataTriple>();
		while(di.hasNext()) {
			SessionData _s = di.next();
			ls.add(_s.getSessionDataTriple());
		}
		
		Map<SessionDataTriple,Boolean> m = null;
		try {
			m = PrearcDatabase.markSessions(ls, PrearcUtils.PrearcStatus.DELETING);
		} catch (SQLException e) {
			fail ("SQLException" + e.getMessage());
		} catch (SessionException e) {
			fail ("SessionException" + e.getMessage());
		}
		
		// make sure they queued
		Iterator<SessionDataTriple> j = m.keySet().iterator();
		while(j.hasNext()){
			SessionDataTriple _s = j.next();
			Assert.assertTrue(m.get(_s));
		}
		
		// make sure the new status is correct
		projs = null;
		try {
			projs = PrearcDatabase.getProjects(uri);
		}
		catch (SQLException e) {
			fail ("SQLException" + e.getMessage());
		}
		catch (SessionException e) {
			fail ("SessionException" + e.getMessage());
		}
		
		di = projs.iterator();
		while(di.hasNext()) {
			SessionData _s = di.next();
			Assert.assertEquals(PrearcUtils.PrearcStatus.DELETING, _s.getStatus());
		}
	}
	@Test
	public final void multipleMoveToProject () {
		// move multiple sessions to a new project
		String uri = "prearchive/projects/proj_3";
		List<SessionData> projs = null;
		try {
			projs = PrearcDatabase.getProjects(uri);
		}
		catch (SQLException e) {
			fail ("SQLException" + e.getMessage());
		}
		catch (SessionException e) {
			fail ("SessionException" + e.getMessage());
		}
		Iterator<SessionData> di = projs.iterator();
		List<SessionDataTriple> ls = new ArrayList<SessionDataTriple>();
		while(di.hasNext()) {
			SessionData _s = di.next();
			ls.add(_s.getSessionDataTriple());
		}
		
		Map<SessionDataTriple,Boolean> m = null;
		try {
			m = PrearcDatabase.moveToProject(ls,"proj_1");
		} 
		catch (SyncFailedException e) {
			fail ("SyncFailedException " + e.getMessage());
		} 
		catch (SQLException e) {
			fail ("SQLException" + e.getMessage());
		}
		catch (SessionException e) {
			fail ("SessionException" + e.getMessage());
		}
		
		// make sure each one is renamed
		// block for up to 3 seconds for each session.
		Iterator<SessionDataTriple> li = ls.iterator();
		int counter = 3;
		boolean found = false;
		while(li.hasNext()){
			SessionDataTriple _s = li.next();
			SessionData sd = null;
			while(counter != 0) {
			try {
					sd = PrearcDatabase.getSession(_s.getFolderName(),_s.getTimestamp(), "proj_1");
					found = true;
			} 
			catch (SQLException e) {
				fail ("SQLException" + e.getMessage());
			}
			catch (SessionException e) {
					found = false;
			}
				if (!found) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {}
			
					counter--;
				}
				else {
					break;
				}
			}
			Assert.assertTrue(found);
			Assert.assertEquals("proj_1", sd.getProject());
			Assert.assertEquals(_s.getTimestamp(), sd.getTimestamp());
			Assert.assertEquals(_s.getFolderName(), sd.getFolderName());
		}
	}
}
