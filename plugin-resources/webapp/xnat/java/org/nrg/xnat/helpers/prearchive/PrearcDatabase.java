package org.nrg.xnat.helpers.prearchive;

import java.io.File;
import java.io.IOException;
import java.io.SyncFailedException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.nrg.status.ListenerUtils;
import org.nrg.status.StatusListenerI;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.archive.PrearcSessionArchiver;
import org.nrg.xnat.archive.XNATSessionBuilder;
import org.nrg.xnat.helpers.prearchive.PrearcUtils.PrearcStatus;
import org.nrg.xnat.restlet.XNATApplication;
import org.nrg.xnat.restlet.services.Archiver;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.xml.sax.SAXException;

/**
 * This class creates a in-memory database that holds all the information in the prearchive 
 * directory. 
 * 
 * All methods in this class are static and a static initializer 
 * @author aditya
 *
 */
public final class PrearcDatabase {
	static Logger logger = Logger.getLogger(PrearcTableBuilder.class);
	private final static String dbName =  "prearchive";
	public static Connection conn = PrearcDatabase.createConnection();
	final static String table = "prearchive";
	private final static String tableSql = PrearcDatabase.createTableSql();
	public static boolean ready = false;
	
	// an object that synchronizes the cache with some permanent store
	private static SessionDataDelegate sessionDelegate;
	
	private static String prearcPath;
	
	/**
	 * Initialize the cache with a path to the prearchive and/or a delegate object
	 * that sync's up the cache with some permanent store.
	 * @param prearcPath
	 * @throws SQLException
	 * @throws IllegalStateException
	 * @throws SessionException
	 * @throws IOException 
	 */
	public static void initDatabase(String prearcPath) throws SQLException, IllegalStateException, SessionException, IOException {
		if (!PrearcDatabase.ready) {
		PrearcDatabase.prearcPath = prearcPath;
		PrearcDatabase.sessionDelegate = new FileSystemSessionData(PrearcDatabase.prearcPath);
		PrearcDatabase.createDatabase();
		PrearcDatabase.ready = true;
	}
	}
	
	public static void initDatabase(String prearcPath, SessionDataDelegate sp) throws SQLException, IllegalStateException, SessionException, IOException {
		if (!PrearcDatabase.ready){ 
		PrearcDatabase.prearcPath = prearcPath;
		PrearcDatabase.sessionDelegate = sp;
		PrearcDatabase.createDatabase();
		PrearcDatabase.ready = true;}
	}
	
	public static void initDatabase(SessionDataDelegate sp) throws SQLException, IllegalStateException, SessionException, IOException {
		if (!PrearcDatabase.ready){ 
		PrearcDatabase.prearcPath = PrearcDatabase.getPrearcPath();
		PrearcDatabase.sessionDelegate = sp;
		PrearcDatabase.createDatabase();
		PrearcDatabase.ready = true;}
	}
	
	/**
	 * The default initializer uses the file system as this cache's permanent store
	 * @throws SQLException
	 * @throws IllegalStateException
	 * @throws SessionException
	 * @throws IOException 
	 */
	public static void initDatabase() throws SQLException, IllegalStateException, SessionException, IOException {
		if (!PrearcDatabase.ready){ 
		PrearcDatabase.prearcPath = PrearcDatabase.getPrearcPath();
		PrearcDatabase.sessionDelegate = new FileSystemSessionData(PrearcDatabase.prearcPath);
		PrearcDatabase.createDatabase();
		PrearcDatabase.ready = true;}
	}
	
	protected static void setSessionDataModifier (SessionDataModifierI sm) {
		PrearcDatabase.sessionDelegate.setSm(sm);
	}
	
	private static void createDatabase() throws SQLException, IllegalStateException, SessionException, IOException {
		PrearcDatabase.conn.setAutoCommit(false);
		PrearcDatabase.createTable(table);
		PrearcDatabase.conn.commit();
		PrearcDatabase.refresh();
	}


	/**
	 * Open a new connection to the database. Set autocommit to false. Make it non-static? Race condition!
	 * @return
	 */
	private static Connection createConnection () {
		Connection conn = null;
		if (PrearcDatabase.conn == null) {
			try {
				Class.forName("org.h2.Driver");
				conn = DriverManager.getConnection("jdbc:h2:mem:" + dbName, "sa", "");
			}
			catch (ClassNotFoundException e) {
				logger.error("Unable to load the org.h2.Driver", e);
			}
			catch (SQLException e) {
				logger.error("Unable to connect to in-memory database " + dbName, e);
			}	
		}
		else {
			conn = PrearcDatabase.conn;
		}
		
		return conn;
	}
	
	/**
	 * 
	 * @return Path to the prearchive on the user filesystem
	 */
	
	protected static String getPrearcPath () {
		return ArcSpecManager.GetInstance().getGlobalPrearchivePath();
	}

	/**
	 * Create the table if it doesn't exist. Should only be called once. Delete table argument
	 * on class load.
	 * @param table
	 * @throws SQLException 
	 */
	private static void createTable (String table) throws SQLException {
		PrearcDatabase.conn.createStatement().execute(tableSql);
	}
	
	/**
	 * Populate the table with sessions in the prearchive directory. Should only be called
	 * once on class load.
	 * @param table
	 * @throws Throwable 
	 * @throws SessionException 
	 * @throws SAXException
	 * @throws SQLException 
	 * @throws IOException 
	 * @throws IllegalStateException 
	 */
	private static void populateTable (String table) throws SQLException, SessionException, IllegalStateException, IOException {
		PrearcDatabase.addSessions(PrearcDatabase.sessionDelegate.get());
	}
	
	private static void addSessions (final Collection<SessionData> ss) throws IllegalStateException, SQLException, SessionException {
		new SessionOp<Void>() {
			public java.lang.Void op () throws SQLException , SessionException {
				PreparedStatement statement = PrearcDatabase.conn.prepareStatement(PrearcDatabase.insertSql());
				for (final SessionData s : ss) {
					for (int i = 0; i < DatabaseSession.values().length; i++) {
						DatabaseSession.values()[i].setInsertStatement(statement, s);
					}
					statement.executeUpdate();
				}
				return null;
			}
		}.run();
	}
	
	/**
	 * Add the given session to the table. Only used when initially populating the database, or
	 * when it is refreshed. 
	 * @param s               The session
	 * @param table           The table to which to add the session   
	 * @throws SQLException
	 * @throws UniqueRowNotFoundException 
	 * @throws BadSessionInformationException 
	 */
	public static void addSession(final SessionData s) throws SQLException, SessionException {
		PrearcDatabase.checkArgs(s);
		new SessionOp<Void>(){
			public java.lang.Void op() throws SQLException, SessionException {
				int rowCount = PrearcDatabase.numDuplicateSessions(s.getFolderName(),s.getTimestamp(),s.getProject());
				if (rowCount >= 1) {
					throw new SessionException("Trying to add an existing session");
				}
				else {
					PreparedStatement statement = this.conn.prepareStatement(PrearcDatabase.insertSql());
					for (int i = 0; i < DatabaseSession.values().length; i++) {
						DatabaseSession.values()[i].setInsertStatement(statement, s);
					}
					statement.executeUpdate();
				}
				this.conn.commit();
				return null;
			}
		}.run();
	}
	
	
	/**
	 * Parse the given uri and return a list of sessions in the database.
	 * @param uri
	 * @return
	 * @throws java.util.IllegalFormatException Thrown if the uri cannot be parsed 
	 * @throws SQLException Thrown if there is an issue with the database connection.
	 * @throws SessionException 
	 */
	public static List<SessionData> getProjects (String uri) throws java.util.IllegalFormatException, SQLException, SessionException {
		final PrearcUriParserUtils.ProjectsParser parser = new PrearcUriParserUtils.ProjectsParser(new PrearcUriParserUtils.UriParser(XNATApplication.PREARC_PROJECT_URI));
		final List<String> projects = parser.readUri(uri);
		return new SessionOp<List<SessionData>>(){
			public List<SessionData> op() throws SQLException, SessionException {
				List<SessionData> ls = new ArrayList<SessionData>();
				String sql = DatabaseSession.PROJECT.allMatchesSql(projects.toArray(new String[projects.size()]));
				ResultSet rs = conn.createStatement().executeQuery(sql);
				while(rs.next()) {
					ls.add(DatabaseSession.fillSession(rs));
				}
				return ls;
			}
		}.run();
	}
	
	/**
	 * Parse uri and return a specific session in the database
	 * @param uri
	 * @return
	 * @throws java.util.IllegalFormatException
	 * @throws SQLException
	 * @throws SessionException
	 */
	public static SessionData getSession (String uri) throws java.util.IllegalFormatException, SQLException, SessionException {
		final PrearcUriParserUtils.SessionParser parser = new PrearcUriParserUtils.SessionParser(new PrearcUriParserUtils.UriParser(XNATApplication.PREARC_SESSION_URI));
		final Map<String,String> sess = parser.readUri(uri);
		return new SessionOp<SessionData>() {
			public SessionData op() throws SQLException, SessionException {
				return PrearcDatabase.getSession(sess.get("SESSION_LABEL"), sess.get("SESSION_TIMESTAMP"), sess.get("PROJECT_ID"));
			}
		}.run();
	}
	
	/**
	 * Path to the project in the users prearchive directory
	 * @param s
	 * @return
	 */
	static String projectPath (String proj) {
		return StringUtils.join(new String[]{PrearcDatabase.prearcPath,proj});
	}
	
	/**
	 * Generate prepared SQL statement to insert a session.
	 * TODO : Move this into DatabaseSession
	 * @return
	 */
	private static String insertSql () {
		List<String> ss = new ArrayList<String>();
		for (int i = 0; i < DatabaseSession.values().length ;i++) {
			ss.add("?");
		}
		return "INSERT INTO " + PrearcDatabase.table + " VALUES(" + StringUtils.join(ss.toArray(),',') + ")";
	}
	
	/**
	 * Recreate the database from scratch. This is an expensive operation.
	 * @throws SQLException
	 * @throws SAXException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public static void refresh() throws IllegalStateException, SQLException, SessionException, IOException {
		try {
			PrearcDatabase.deleteRows();
			PrearcDatabase.populateTable(PrearcDatabase.table);
			PrearcDatabase.conn.commit();
		}
		catch (SQLException e) {
			try {
				PrearcDatabase.conn.rollback();
			}
			catch (SQLException s) {
				throw s;
			}
			throw e;
		}
		catch (SessionException e) {
			try {
				PrearcDatabase.conn.rollback();
			}
			catch (SQLException s) {
				throw s;
			}
			throw e;
		}

	}
	
	/**
	 * Move a session from one project to another. 'oldProj' is allowed to be null or empty to allow
	 * moving from an unassigned project to a real one. The other arguments must be non-empty, non-null 
	 * values. 
	 * @param sess Name of the old session 
	 * @param oldProj Name of the old project 
	 * @param newProj Name of the new Project
	 * @param timestamp timestamp of old session
	 * @param proj project of old session
	 * @return Return true if successful, false otherwise
	 * @throws SessionException
	 * @throws SQLException 
	 * @throws Exception 
	 */
	private static boolean _moveToProject (final String sess, final String timestamp, final String proj, final String newProj) throws SessionException, SyncFailedException, SQLException{
		if (null == newProj || newProj.isEmpty()) {
			throw new SessionException("New project argument is null or empty");
		}
		final SessionData sd = PrearcDatabase.getSession(sess, timestamp, proj);
				
		new LockAndSync<java.lang.Void>(sess,timestamp,proj,sd.getStatus()) {
			java.lang.Void extSync() throws SyncFailedException {
				PrearcDatabase.sessionDelegate.move(sd, newProj);
				return null;
			}
			void cacheSync() throws SQLException, SessionException {
				PrearcDatabase.withSession(sess, timestamp, proj, new SessionOp<Void>(){
					public Void op () throws SQLException, SessionException {
						try {
							PrearcDatabase._unsafeDeleteSession(sess, timestamp,proj);
							SessionData newSd = sd;
							newSd.setProject(newProj);
							newSd.setStatus(PrearcUtils.PrearcStatus.READY);
							PrearcDatabase.addSession(newSd);
						} catch (SyncFailedException e) {
							logger.error(e);
							throw new IllegalStateException(e.getMessage());
						}
						return null;
					}
				});
			}
			@Override
			boolean checkStatus() {
				return sd.getStatus().equals(PrearcStatus.MOVING);
			}
		}.run();
		return true;
	}
	
	public static boolean moveToProject (final String sess, final String timestamp, final String proj, final String newProj) throws SessionException, SyncFailedException, SQLException {
		final SessionData sd = PrearcDatabase.getSession(sess,timestamp,proj);
		if (!sd.getStatus().equals(PrearcStatus.MOVING) && markSession(sd.getSessionDataTriple(), PrearcStatus.MOVING)) {
			return PrearcDatabase._moveToProject(sess, timestamp, proj, newProj);}
		else {
			return false;
		}
	}

	
	/**
	 * Move a session from the prearchive to the archive.
	 * @param sess Name of the old session 
	 * @param oldProj Name of the old project 
	 * @param newProj Name of the new Project
	 * @param timestamp timestamp of old session
	 * @param proj project of old session
	 * @return Return true if successful, false otherwise
	 * @throws SessionException
	 * @throws SQLException 
	 * @throws Exception 
	 */
	public static Map<SessionDataTriple, Boolean> archive(final List<Map<String,Object>> sessions, final String project, final boolean allowDataDeletion, final boolean overwrite, final XDATUser user, final List<StatusListenerI> listeners) throws SQLException, SessionException, SyncFailedException, IllegalStateException {
		List<SessionDataTriple> ss= new ArrayList<SessionDataTriple>();
		
		for(Map<String,Object> map:sessions){
			ss.add(SessionDataTriple.fromFile(project, Archiver.getSrcDIR(map)));
		}
		
		final Map<SessionDataTriple, Boolean> ret = PrearcDatabase.markSessions(ss, PrearcUtils.PrearcStatus.ARCHIVING);
		new Thread() {
			public void run() {
				final java.util.Iterator<Map<String,Object>> i = sessions.iterator();
					while(i.hasNext()){
						Map<String,Object> _s = i.next();
						try {
							PrearcDatabase.archive(_s,project,allowDataDeletion,overwrite,user,listeners);
						} catch (SyncFailedException e) {
							logger.error("",e);
							throw new IllegalStateException();
						}
						catch (SessionException e) {
							logger.error("",e);
					    } 
					  catch (SQLException e) {
						logger.error("",e);
					}
				}		
			}
		}.start();
		return ret;
	}
	
	
	public static String archive (Map<String,Object> session, String project, boolean allowDataDeletion, boolean overwrite, XDATUser user, List<StatusListenerI> listeners) throws SessionException, SyncFailedException, SQLException{
		final PrearcSessionArchiver archiver;
		try {
			archiver = Archiver.buildArchiver(session, project, allowDataDeletion, overwrite, user);
		}catch (Exception e1) {
			throw new IllegalStateException(e1);
		}
		
		ListenerUtils.addListeners(listeners, archiver);
		
		final String prearcDIR=archiver.getSrcDIR().getName();
		final String timestamp=archiver.getSrcDIR().getParentFile().getName();
		
		final SessionData sd = PrearcDatabase.getSession(prearcDIR, timestamp, project);
				
		LockAndSync<String> l = new LockAndSync<String>(prearcDIR,timestamp,project,sd.getStatus()) {
			String extSync() throws SyncFailedException {
				try {
					return archiver.call();
				} catch (Exception e) {
					throw new SyncFailedException(e.getMessage());
				}
			}
			void cacheSync() throws SQLException, SessionException {
				PrearcDatabase.withSession(sess, timestamp, proj, new SessionOp<Void>(){
					public Void op () throws SQLException, SessionException {
						this.conn.createStatement().execute(DatabaseSession.deleteSessionSql(sess,timestamp,proj));
						this.conn.commit();
						return null;
					}
				});
			}
			@Override
			boolean checkStatus() {
				return sd.getStatus().equals(PrearcStatus.ARCHIVING);
			}
		};
		if(!l.run()){
			throw new SyncFailedException("Operation failed");
		}
		return l.s;
	}
	
	public static void buildSession (final File sessionDir, final String session, final String timestamp, final String project) throws SyncFailedException, SQLException, SessionException {
		final SessionData sd = PrearcDatabase.getSession(session, timestamp, project);
		try {
			new LockAndSync<java.lang.Void>(session,timestamp,project,sd.getStatus()) {
				java.lang.Void extSync() throws SyncFailedException {
					try {
						new XNATSessionBuilder(sessionDir,new File(sessionDir.getPath() + ".xml"),project).call();
					} catch (IOException e) {
						throw new SyncFailedException(e.getMessage());
					}
					return null;
				}
				void cacheSync() {}
				@Override
				boolean checkStatus() {
					return sd.getStatus().equals(PrearcStatus.BUILDING);
				}
			}.run();
		} 
		// cacheSync is empty so it can't throw an exception
		catch (SQLException e) {} 
		catch (SessionException e) {}
	}
	
	protected static boolean markSession(SessionDataTriple ss, PrearcUtils.PrearcStatus s) throws SQLException, SessionException{
		return PrearcDatabase.setStatus(ss.getFolderName(), ss.getTimestamp(), ss.getProject(), s);
	}
	
	protected static Map<SessionDataTriple, Boolean> markSessions (List<SessionDataTriple> ss, PrearcUtils.PrearcStatus s) throws SQLException, SessionException{
		java.util.Iterator<SessionDataTriple> i = ss.iterator();
		Map<SessionDataTriple, Boolean> ret = new HashMap<SessionDataTriple, Boolean>();
		while (i.hasNext()) {
			SessionDataTriple t = i.next();
			ret.put(t, PrearcDatabase.markSession(t,s));
		}
		return ret;
	}
	
	/**
	 * Queue a list of sessions to move to a new project.
	 * @param ss
	 * @param newProj
	 * @return A map of the given sessions and flag that indicates whether the session was successfully queued. 
	 * @throws SQLException
	 * @throws SessionException
	 * @throws SyncFailedException
	 */
	public static Map<SessionDataTriple, Boolean> moveToProject(final List<SessionDataTriple> ss, final String newProj) throws SQLException, SessionException, SyncFailedException, IllegalStateException {
		final Map<SessionDataTriple, Boolean> ret = PrearcDatabase.markSessions(ss, PrearcUtils.PrearcStatus.MOVING);
		new Thread() {
			public void run() {
				final java.util.Iterator<SessionDataTriple> i = ss.iterator();
					while(i.hasNext()){
						SessionDataTriple _s = i.next();
								try {
									PrearcDatabase._moveToProject(_s.getFolderName(),_s.getTimestamp(),_s.getProject(),newProj);
								} catch (SyncFailedException e) {
									logger.error(e);
									throw new IllegalStateException();
					}
					  catch (SessionException e) {
						  System.out.println("SessionException : " + e.getMessage());
						  for (StackTraceElement _e : e.getStackTrace())
						        System.out.println(_e);
					  } 
					  catch (SQLException e) {
						Thread.dumpStack();
					}
				}		
			}
		}.start();
		return ret;
	}
	
	/**
	 * Queue a list of sessions for deletion.
	 * @param ss
	 * @param newProj
	 * @return A map of the given sessions and flag that indicates whether the session was successfully queued. 
	 * @throws SQLException
	 * @throws SessionException
	 * @throws SyncFailedException
	 * @throws {@link IllegalStateException} Thrown if any session fails to sync with the cache, irrecoverable because it indicates that the
	 * prearchive directory is in a bad state requiring manual intervention.
	 */
	public static Map<SessionDataTriple, Boolean> deleteSession(final List<SessionDataTriple> ss) throws SQLException, SessionException, SyncFailedException, IllegalStateException {
		Map<SessionDataTriple, Boolean> ret = PrearcDatabase.markSessions(ss, PrearcUtils.PrearcStatus.DELETING);
		new Thread() {
			public void run() {
				java.util.Iterator<SessionDataTriple> i = ss.iterator();
				while(i.hasNext()){
					SessionDataTriple _s = i.next();
					try {
						PrearcDatabase._deleteSession(_s.getFolderName(),_s.getTimestamp(),_s.getProject());
					} catch (SyncFailedException e) {
						logger.error(e);
						throw new IllegalStateException();
					} catch (SQLException e) {
					} catch (SessionException e) {
					}
				}
			}
		}.start();
		return ret;
	}
	
	/**
	 * Move to project by uri.
	 * @param uri
	 * @return
	 * @throws SessionException
	 * @throws SyncFailedException
	 * @throws SQLException
	 */
	public static boolean moveToProject (String uri) throws SessionException, SyncFailedException, SQLException {
		final PrearcUriParserUtils.SessionParser parser = new PrearcUriParserUtils.SessionParser(new PrearcUriParserUtils.UriParser(XNATApplication.PREARC_SESSION_URI));
		Map<String,String> sess = parser.readUri(uri);
		return PrearcDatabase.moveToProject(sess.get("SESSION_LABEL"), sess.get("SESSION_TIMESTAMP"), sess.get("PROJECT_ID"), parser.i.f.getValues("dest"));		
	}
	

	/**
	 * Set the status of an existing session. All arguments must be non-null and non-empty.
	 * @param sess
	 * @param timestamp
	 * @param proj
	 * @param status
	 * @throws SQLException
	 * @throws SessionException
	 */
	public static boolean setStatus (final String sess, final String timestamp, final String proj, final PrearcUtils.PrearcStatus status) throws SQLException, SessionException {
		return setStatus(sess,timestamp,proj,status,false);
	}
	
	/**
	 * Set the status of an existing session. All arguments must be non-null and non-empty.
	 * @param sess
	 * @param timestamp
	 * @param proj
	 * @param status
	 * @throws SQLException
	 * @throws SessionException
	 */
	public static boolean setStatus (final String sess, final String timestamp, final String proj, final PrearcUtils.PrearcStatus status, boolean overrideLock) throws SQLException, SessionException {
		if (!overrideLock && PrearcDatabase.isLocked(sess, timestamp, proj)) {
			return false;
		}
		PrearcDatabase.unsafeSetStatus(sess, timestamp, proj, status); 
		return true;
	}
	
	public static void unsafeSetStatus (final String sess, final String timestamp, final String proj, final PrearcUtils.PrearcStatus status) throws SQLException, SessionException {
		if (null == status) {
			throw new SessionException ("Status argument is null or empty");
		}
		PrearcDatabase.withSession(sess, timestamp, proj, new SessionOp<Void>() {
			public Void op () throws SQLException, SessionException {
				this.conn.createStatement().execute(DatabaseSession.STATUS.updateSessionSql(sess, timestamp, proj, status));
				this.conn.commit();
				return null;
			}
		}); 
	}
	public static boolean setStatus(final String uri, final PrearcUtils.PrearcStatus status) throws SQLException, SessionException {
		return setStatus(uri,status,false);
	}
	
	public static boolean setStatus(final String uri, final PrearcUtils.PrearcStatus status, boolean overrideLock) throws SQLException, SessionException {
		final PrearcUriParserUtils.SessionParser parser = new PrearcUriParserUtils.SessionParser(new PrearcUriParserUtils.UriParser(XNATApplication.PREARC_SESSION_URI));
		final Map<String,String> sess = parser.readUri(uri);
		return PrearcDatabase.setStatus(sess.get("SESSION_LABEL"), sess.get("SESSION_TIMESTAMP"), sess.get("PROJECT_ID"), status,overrideLock);
	}
	
	/**
	 * Delete a session from the prearchive database. 
	 * if the session is locked.
	 * @param sess
	 * @param proj
	 * @return Return true if successful, false
	 * @throws SQLException
	 * @throws SyncFailedException 
	 * @throws UniqueRowNotFoundException thrown if the number of rows with 'sess' and 'proj' is not 1.
	 * @throws BadSessionInformationException thrown if any arguments are null or empty
	 */
	public static boolean deleteCacheRow (final String sess, final String timestamp, final String proj) throws SQLException, SessionException, SyncFailedException {
		final SessionData sd = PrearcDatabase.getSession(sess, timestamp, proj);
		new LockAndSync<java.lang.Void>(sess,timestamp,proj,sd.getStatus()){
			protected boolean checkStatus(){
				return PrearcStatus._DELETING.equals(this.status);
			}			
			
			java.lang.Void extSync() throws SyncFailedException {
				return null;
			}
			void cacheSync() throws SQLException, SessionException {
				PrearcDatabase.withSession(sess,timestamp,proj, new SessionOp<Void>(){
					public java.lang.Void op () throws SQLException, SessionException {
						this.conn.createStatement().execute(DatabaseSession.deleteSessionSql(sess,timestamp,proj));
						this.conn.commit();
						return null;
					}
				});
			}
		}.run();
		return true;
	}
	
	
	/**
	 * Delete a session from the prearchive database. 
	 * if the session is locked.
	 * @param sess
	 * @param proj
	 * @return Return true if successful, false
	 * @throws SQLException
	 * @throws SyncFailedException 
	 * @throws UniqueRowNotFoundException thrown if the number of rows with 'sess' and 'proj' is not 1.
	 * @throws BadSessionInformationException thrown if any arguments are null or empty
	 */
	private static boolean _deleteSession (final String sess, final String timestamp, final String proj) throws SQLException, SessionException, SyncFailedException {
		final SessionData sd = PrearcDatabase.getSession(sess, timestamp, proj);
		new LockAndSync<java.lang.Void>(sess,timestamp,proj,sd.getStatus()){
			protected boolean checkStatus (){
				return PrearcStatus.DELETING.equals(this.status);
			}			
			
			java.lang.Void extSync() throws SyncFailedException {
				sessionDelegate.delete(sd);
				return null;
			}
			void cacheSync() throws SQLException, SessionException {
				PrearcDatabase.withSession(sess,timestamp,proj, new SessionOp<Void>(){
					public java.lang.Void op () throws SQLException, SessionException {
						this.conn.createStatement().execute(DatabaseSession.deleteSessionSql(sess,timestamp,proj));
						this.conn.commit();
						return null;
					}
				});
			}
		}.run();
		return true;
	}
	
	private static boolean _unsafeDeleteSession (final String sess, final String timestamp, final String proj) throws SQLException, SessionException, SyncFailedException {
		final SessionData sd = PrearcDatabase.getSession(sess, timestamp, proj);
		new LockAndSync<java.lang.Void>(sess,timestamp,proj,sd.getStatus()){
			protected boolean checkStatus (){
				return true;
			}			
			
			java.lang.Void extSync() throws SyncFailedException {
				sessionDelegate.delete(sd);
				return null;
			}
			void cacheSync() throws SQLException, SessionException {
				PrearcDatabase.withSession(sess,timestamp,proj, new SessionOp<Void>(){
					public java.lang.Void op () throws SQLException, SessionException {
						this.conn.createStatement().execute(DatabaseSession.deleteSessionSql(sess,timestamp,proj));
						this.conn.commit();
						return null;
					}
				});
			}
		}.run();
		return true;
	}
	
	public static boolean deleteSession(final String sess, final String timestamp, final String proj) throws SQLException, SessionException, SyncFailedException {
		final SessionData sd = PrearcDatabase.getSession(sess,timestamp,proj);
		if (!sd.getStatus().equals(PrearcStatus.DELETING) && markSession(sd.getSessionDataTriple(), PrearcStatus.DELETING)) {
		return PrearcDatabase._deleteSession(sess, timestamp, proj);}
		else {
			return false;
		}
	}
	
	static abstract class LockAndSync<T>{
		final String sess,timestamp,proj;
		final PrearcStatus status;
		T s;
		LockAndSync(String sess, String timestamp, String proj,PrearcStatus status){
			this.sess = sess;
			this.timestamp = timestamp;
			this.proj = proj;
			this.status=status;
		} 
		
		abstract boolean checkStatus ();
		abstract T extSync () throws SyncFailedException;
		abstract void cacheSync () throws SQLException, SessionException;
		boolean run() throws SQLException, SessionException, SyncFailedException{
			try {
				if(!checkStatus()){
					return false;
				}
				lockSession(this.sess, this.timestamp, this.proj);
				s=extSync();
				cacheSync();
				return true;
			}
			catch (SQLException e) {
				PrearcDatabase.unLockSession(this.sess, this.timestamp, this.proj);
				throw e;
			} 
			catch (SessionException e) {
				PrearcDatabase.unLockSession(this.sess, this.timestamp, this.proj);
				throw e;
			}
			catch (java.io.SyncFailedException e) {
				PrearcDatabase.unLockSession(this.sess, this.timestamp, this.proj);
				PrearcDatabase.setStatus(sess, timestamp, proj , PrearcUtils.PrearcStatus.ERROR);
				throw e;
			}
		}
	}
	
	/**
	 * A URI decoding wrapper around {@link PrearcDatabase#isLocked(String,String,String)}
	 * @param uri
	 * @return
	 * @throws SQLException
	 * @throws SessionException
	 */
	protected static boolean isLocked (String uri) throws SQLException, SessionException {
		final PrearcUriParserUtils.SessionParser parser = new PrearcUriParserUtils.SessionParser(new PrearcUriParserUtils.UriParser(XNATApplication.PREARC_SESSION_URI));
		final Map<String,String> sess = parser.readUri(uri);
		return PrearcDatabase.isLocked(sess.get("SESSION_LABEL"), sess.get("SESSION_TIMESTAMP"), sess.get("PROJECT_ID"));
	}
	

	/**
	 * Check to see if the sessions locked against edits. 
	 * @param sess
	 * @param timestamp
	 * @param proj
	 * @return
	 * @throws SQLException
	 * @throws SessionException
	 */
	public static boolean isLocked(final String sess, final String timestamp, final String proj) throws SQLException, SessionException {
		SessionData sd = PrearcDatabase.getSession(sess, timestamp,proj);
		return PrearcUtils.inProcessStatusMap.containsValue(sd.getStatus());
	}
	
	public static void unLockSession(String uri) throws SQLException, SessionException {
		final PrearcUriParserUtils.SessionParser parser = new PrearcUriParserUtils.SessionParser(new PrearcUriParserUtils.UriParser(XNATApplication.PREARC_SESSION_URI));
		final Map<String,String> sess = parser.readUri(uri);
		PrearcDatabase.unLockSession(sess.get("SESSION_LABEL"), sess.get("SESSION_TIMESTAMP"), sess.get("PROJECT_ID"));
	}
	
	protected static void unLockSession(final String sess, final String timestamp, final String proj) throws SQLException, SessionException {
		try {
			PrearcDatabase.getSession(sess, timestamp,proj);
			PrearcDatabase.unsafeSetStatus(sess, timestamp, proj, PrearcUtils.PrearcStatus.READY);
		}
		catch (SessionException e){
			
		}
	}
	/**
	 * A URI decoding wrapper around {@link PrearcDatabase#lockSession(String, String, String)} 
	 * @param uri
	 * @return
	 * @throws SQLException
	 * @throws SessionException
	 */
	protected static boolean lockSession(String uri) throws SQLException, SessionException {
		final PrearcUriParserUtils.SessionParser parser = new PrearcUriParserUtils.SessionParser(new PrearcUriParserUtils.UriParser(XNATApplication.PREARC_SESSION_URI));
		final Map<String,String> sess = parser.readUri(uri);
		return PrearcDatabase.lockSession(sess.get("SESSION_LABEL"), sess.get("SESSION_TIMESTAMP"), sess.get("PROJECT_ID"));
	}
	
	/**
	 * A database row is locked by setting its status to the "locked" version
	 * its current status. {@link PrearcUtils#inProcessStatusMap} shows the mapping.
	 * @param sess
	 * @param timestamp
	 * @param proj
	 * @return
	 * @throws SQLException
	 * @throws SessionException
	 */
	protected static boolean lockSession (final String sess, final String timestamp, final String proj) throws SQLException, SessionException {
		SessionData sd = PrearcDatabase.getSession(sess, timestamp, proj);
		if (PrearcUtils.inProcessStatusMap.containsKey(sd.getStatus())) {
			final PrearcUtils.PrearcStatus inp = PrearcUtils.inProcessStatusMap.get(sd.getStatus());
			PrearcDatabase.withSession(sess,timestamp,proj, new SessionOp<Void>(){
				public java.lang.Void op () throws SQLException, SessionException {
					this.conn.createStatement().execute(DatabaseSession.STATUS.updateSessionSql(sess,timestamp,proj,inp));
					this.conn.commit();
					return null;
				}
			});
			return true;
		}
		else {
			return false;
		}
	}
	
	/** 
	 * A URI decoding wrapper for {@link PrearcDatabase#deleteSession(String,String,String)}
	 * @param uri
	 * @throws SQLException
	 * @throws SessionException
	 * @throws SyncFailedException 
	 */
	public static boolean deleteSession (String uri) throws SQLException, SessionException, SyncFailedException {
		final PrearcUriParserUtils.SessionParser parser = new PrearcUriParserUtils.SessionParser(new PrearcUriParserUtils.UriParser(XNATApplication.PREARC_SESSION_URI));
		final Map<String,String> sess = parser.readUri(uri);
		return PrearcDatabase.deleteSession(sess.get("SESSION_LABEL"), sess.get("SESSION_TIMESTAMP"), sess.get("PROJECT_ID"));
	}
	
	
	/**
	 * Search for a session given its name and project.
	 * @param sess
	 * @param proj
	 * @return
	 * @throws SQLException
	 * @throws SessionException Throws if the given arguments match more than one session 
	 */
	public static SessionData getSession(final String sess, final String timestamp, final String proj) throws SQLException, SessionException {
		return PrearcDatabase.withSession(sess, timestamp, proj, new SessionOp<SessionData>() {
			public SessionData op () throws SQLException {
				ResultSet rs = this.conn.createStatement().executeQuery(DatabaseSession.findSessionSql(sess, timestamp, proj)); 
				rs.next();
				return DatabaseSession.fillSession(rs);
			}
		});
	}
	
	
	/**
	 * Search for a session given its UID.
	 * @param uid
	 * @return
	 * @throws SQLException
	 * @throws SessionException Throws if the given arguments match more than one session 
	 */
	public static Collection<SessionData> getSessionByUID(final String uid) throws SQLException, SessionException {
		return new SessionOp<Collection<SessionData>>() {
			public Collection<SessionData> op() throws SQLException {
				List<SessionData> matches=new ArrayList<SessionData>();
				ResultSet rs = this.conn.createStatement().executeQuery(DatabaseSession.TAG.findSql(uid)); 
				while(rs.next()) {
					matches.add(DatabaseSession.fillSession(rs));
				}
				return matches;
			}
		}.run();
	}

	/**
	 * Count the number of session in the database with the given name associated with the 
	 * given project.
	 * @param sess
	 * @param proj
	 * @return
	 * @throws SQLException
	 * @throws SessionException 
	 */
	public static int numDuplicateSessions(final String sess, final String timestamp, final String proj) throws SQLException, SessionException {
		return new SessionOp<Integer>() {
			public Integer op() throws SQLException, SessionException {
				ResultSet rs = this.conn.createStatement().executeQuery(DatabaseSession.countSessionSql(sess,timestamp, proj));
				rs.next();
				return rs.getInt(1);
			}
		}.run();
	}

	/**
	 * Delete all the rows in the prearchive table.
	 * @throws SQLException
	 */
	private static void deleteRows() throws SQLException {
		try {
			new SessionOp<Void>(){
				public Void op() throws SQLException {
					this.conn.createStatement().execute("DROP TABLE IF EXISTS " + PrearcDatabase.table);
					this.conn.createStatement().execute(DatabaseSession.createTableSql());
					return null;
				}
			}.run();
		} catch (SessionException e) {
			// should never happen
		}
	}
	
	/**
	 * Debug method: Print the tables in the prearchive database.
	 * @return
	 * @throws SQLException
	 */
	private static String[] showTables () throws SQLException {
		ResultSet rs = PrearcDatabase.conn.createStatement().executeQuery("SHOW TABLES");
		ArrayList<String> as = new ArrayList<String>();
		while (rs.next()) {
			as.add(rs.getString(1));
		}
		return as.toArray(new String[as.size()]);
	}

	/**
	 * Debug method : Print the rows of the given ResultSet.
	 * @return
	 * @throws SQLException
	 */
	private static String showRows (ResultSet rs) throws SQLException {
		StringBuilder sb = new StringBuilder();
		while (rs.next()) {
			sb.append("[");
			for (DatabaseSession d : DatabaseSession.values()) {
				sb.append(d.getColumnName());
				sb.append(":");
				String tmp = d.resultToString(rs); 
				sb.append(tmp == null? "NULL" : tmp);
				sb.append("\n");
			}
			sb.append("]");
		}
		return sb.toString();
	}
	
	/**
	 * Debug method : Show all the rows in the prearchive database.
	 * @return
	 * @throws SQLException
	 */
	
	public static String showAllRows () {
		String ret = null;
		try {
			ret = new SessionOp<String>() {
				public String op() throws SQLException {
					ResultSet r = this.conn.createStatement().executeQuery("SELECT * FROM " + PrearcDatabase.table);
					return PrearcDatabase.showRows(r);
				}
			}.run();
		} catch (SessionException e) {}
		  catch (SQLException e) {
			  System.out.println(e.getMessage());
		  }
		return ret;
	}
	
	// prevent instantiation
	private PrearcDatabase (){}
	
	/**
	 * Generate SQL statement to create the table. 
	 * @return
	 */
	private static String createTableSql () {
		StringBuilder s = new StringBuilder();
		s.append("CREATE TABLE IF NOT EXISTS " + PrearcDatabase.table + "(");
		List<String> values = new ArrayList<String>();
		for (DatabaseSession d : DatabaseSession.values()) {
			values.add(d.getColumnName() + " " + d.getColumnDefinition());
		}
		s.append(StringUtils.join(values.toArray(), ','));
		s.append(")");
		return s.toString();
	}
	
	/**
	 * Build a list of sessions in the given projects. 
	 * @param projects
	 * @return
	 * @throws SQLException
	 * @throws SessionException
	 */
	public static ArrayList<ArrayList<Object>> buildRows (final String[] projects) throws SQLException, SessionException {
		return new SessionOp<ArrayList<ArrayList<Object>>>(){
			public ArrayList<ArrayList<Object>> op() throws SQLException, SessionException {
				ArrayList<ArrayList<Object>> ao = new ArrayList<ArrayList<Object>>();
				ResultSet rs = this.conn.createStatement().executeQuery(DatabaseSession.PROJECT.allMatchesSql(projects));
				ao=convertRStoList(rs);
				return ao;
			}
		}.run();
	}
	
	/**
	 * Build a list of sessions in the given projects. 
	 * @param projects
	 * @return
	 * @throws SQLException
	 * @throws SessionException
	 */
	public static ArrayList<ArrayList<Object>> buildRows (final Collection<SessionDataTriple> ss) throws SQLException, SessionException {
		return new SessionOp<ArrayList<ArrayList<Object>>>(){
			public ArrayList<ArrayList<Object>> op() throws SQLException, SessionException {
				ArrayList<ArrayList<Object>> ao = new ArrayList<ArrayList<Object>>();
				for(final SessionDataTriple s: ss){
					ResultSet rs = this.conn.createStatement().executeQuery(DatabaseSession.findSessionSql(s.getFolderName(), s.getTimestamp(), s.getProject()));
					ao.addAll(convertRStoList(rs));
				}
				return ao;
			}
		}.run();
	}
	
	private static ArrayList<ArrayList<Object>> convertRStoList(ResultSet rs) throws SQLException{
		ArrayList<ArrayList<Object>> ao = new ArrayList<ArrayList<Object>>();
				while(rs.next()) {
					ArrayList<Object> al = new ArrayList<Object>();
					for (DatabaseSession d : DatabaseSession.values()) {
						if(d.equals(DatabaseSession.URL)){
							final String project=DatabaseSession.PROJECT.getFromResult(rs);
							final String timestamp=DatabaseSession.TIMESTAMP.getFromResult(rs);
							final String session=DatabaseSession.FOLDER_NAME.getFromResult(rs);
							al.add(String.format("/prearchive/projects/%s/%s/%s",project,timestamp,session));
						}else{
							al.add(d.getFromResult(rs));
						}
					}
					ao.add(al);
				}
				return ao;
			}
	
	/**
	 * Get the columns in the database table.
	 * @return
	 * @throws SQLException 
	 */
	public static ArrayList<String> getCols () {
		ArrayList<String> s = new ArrayList<String>();
		for (DatabaseSession d : DatabaseSession.values()) {
			s.add(d.getColumnName());
		}
		return s;
	}
	
	public static String printCols() throws SQLException {
		ResultSet rs = PrearcDatabase.conn.createStatement().executeQuery("SHOW COLUMNS FROM " + PrearcDatabase.table);
		ArrayList<String> as = new ArrayList<String>();
		while (rs.next()) {
			as.add(rs.getString(1));
		}
		return StringUtils.join(as.toArray(new String[as.size()]), ",");
	}
	

	
	/**
	 * A generic class that stores a database operation on a session. 
	 * It assumes that PrearcTable.conn is a valid connection, and the operations that change
	 * the database run a conn.commit() after they are done.
	 * @author aditya
	 *
	 * @param <T> The type of data returned by the operation, use java.lang.Void of the operation returns nothing
	 */
	
	static abstract class SessionOp<T> {
		Connection conn;
		public void createConnection() throws ClassNotFoundException, SQLException {
			conn = DriverManager.getConnection("jdbc:h2:mem:" + dbName, "sa", "");
		}
		public abstract T op() throws SQLException, SessionException;
		public T run () throws SQLException, SessionException {
			conn = DriverManager.getConnection("jdbc:h2:mem:" + dbName, "sa", "");
			Object o = null;
			try {
				o = this.op();
			}
			catch (SQLException e) {
				// all changes are rolled back so any SessionOp's that change the database should 
				// run conn.commit() after they are done.
				conn.rollback();
				throw e;
			}
			finally {
				conn.close();
			}
			return (T) o; // unchecked cast
		}
	}
		
	/**
	 * Check that session arguments are valid and there is unique session that
	 * matches the arguments.
	 * If 'proj' is null it is assumed that the session is "Unassigned"
	 * @param sess
	 * @param timestamp
	 * @param proj
	 * @throws SQLException
	 * @throws SessionException
	 */
	private static void checkSession (String sess, String timestamp, String proj) throws SQLException, SessionException {
		PrearcDatabase.checkArgs(sess, timestamp, proj);
		PrearcDatabase.checkUniqueRow(sess, timestamp, proj);
	}
	private static void checkArgs (String sess, String timestamp, String proj) throws SQLException, SessionException {
		if (null == sess || sess.isEmpty()) {
			throw new SessionException("Session argument is null or empty");
		}
		if (null == timestamp || timestamp.isEmpty()) {
			throw new SessionException("Timestamp argument is null or empty");
		}
	}
	private static void checkArgs (SessionData s) throws SQLException, SessionException {
		PrearcDatabase.checkArgs(s.getFolderName(), s.getTimestamp(), s.getProject());
	}
	
	private static void checkUniqueRow (String sess, String timestamp, String proj) throws SQLException, SessionException {
		int rowCount = PrearcDatabase.numDuplicateSessions(sess,timestamp,proj);
		if (rowCount == 0) {
			throw new SessionException("A record with session " + sess + ", timestamp " + timestamp + " and project " + proj + " could not be found.");
		}
		if (rowCount > 1) {
			throw new SessionException("Multiple records with session " + sess + ", timestamp " + timestamp + " and project " + proj + " were found.");
		}
	}
	
	/**
	 * Check session parameters and run the operation
	 * @param <T>
	 * @param sess
	 * @param timestamp
	 * @param proj
	 * @param op
	 * @return
	 * @throws SQLException
	 * @throws SessionException
	 */
	
    private static <T extends Object> T withSession (String sess, String timestamp, String proj, SessionOp<T> op) throws SQLException, SessionException {
		PrearcDatabase.checkSession(sess,timestamp,proj);
		return op.run();
	}
}
