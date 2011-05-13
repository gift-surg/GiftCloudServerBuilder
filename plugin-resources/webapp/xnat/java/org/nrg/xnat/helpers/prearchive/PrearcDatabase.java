package org.nrg.xnat.helpers.prearchive;

import java.io.File;
import java.io.IOException;
import java.io.SyncFailedException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.nrg.status.ListenerUtils;
import org.nrg.status.StatusListenerI;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.db.PoolDBUtils;
import org.nrg.xft.exception.DBPoolException;
import org.nrg.xnat.archive.PrearcSessionArchiver;
import org.nrg.xnat.archive.XNATSessionBuilder;
import org.nrg.xnat.helpers.prearchive.PrearcUtils.PrearcStatus;
import org.nrg.xnat.restlet.XNATApplication;
import org.nrg.xnat.restlet.actions.PrearcImporterA.PrearcSession;
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
	public static Connection conn;
	final static String table = "prearchive";
	final static String tableWithSchema = PoolDBUtils.search_schema_name + "." + PrearcDatabase.table;
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
	public static void initDatabase(String prearcPath) throws Exception, SQLException, IllegalStateException, SessionException, IOException {
		if (!PrearcDatabase.ready) {
		PrearcDatabase.prearcPath = prearcPath;
		PrearcDatabase.sessionDelegate = new FileSystemSessionData(PrearcDatabase.prearcPath);
		PrearcDatabase.createDatabase();
		PrearcDatabase.ready = true;
	}
	}
	
	public static void initDatabase(String prearcPath, SessionDataDelegate sp) throws Exception, SQLException, IllegalStateException, SessionException, IOException {
		if (!PrearcDatabase.ready){ 
		PrearcDatabase.prearcPath = prearcPath;
		PrearcDatabase.sessionDelegate = sp;
		PrearcDatabase.createDatabase();
		PrearcDatabase.ready = true;}
	}
	
	public static void initDatabase(SessionDataDelegate sp) throws Exception , SQLException, IllegalStateException, SessionException, IOException {
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
	public static void initDatabase() throws Exception, SQLException, IllegalStateException, SessionException, IOException {
		if (!PrearcDatabase.ready){ 
		PrearcDatabase.prearcPath = PrearcDatabase.getPrearcPath();
		PrearcDatabase.sessionDelegate = new FileSystemSessionData(PrearcDatabase.prearcPath);
		PrearcDatabase.createDatabase();
		PrearcDatabase.ready = true;}
	}
	
	protected static void setSessionDataModifier (SessionDataModifierI sm) {
		PrearcDatabase.sessionDelegate.setSm(sm);
	}
	
	private static void createDatabase() throws Exception, SQLException, IllegalStateException, SessionException, IOException {
		PrearcDatabase.createTable(tableWithSchema);
		PrearcDatabase.refresh();
	}
	
	/**
	 * 
	 * @return Path to the prearchive on the user filesystem
	 */
	
	protected static String getPrearcPath () {
		return ArcSpecManager.GetInstance(false).getGlobalPrearchivePath();
	}

	/**
	 * Create the table if it doesn't exist. Should only be called once. Delete table argument
	 * on class load.
	 * @param table
	 * @throws SQLException 
	 */
	private static void createTable (String table) throws SQLException, Exception{
		try {
			new SessionOp<Void>() {
				public Void op() throws Exception, SQLException, SessionException {
					String query ="SELECT relname FROM pg_catalog.pg_class WHERE  relname=LOWER('" + PrearcDatabase.table + "');";
					String exists =(String)PoolDBUtils.ReturnStatisticQuery(query, "relname", null,null);
					if (exists==null){
						PoolDBUtils.ExecuteNonSelectQuery(tableSql, null , null);
					}
					return null;
				}
			}.run();
		}
		// can't happen
		catch (SessionException e){
			logger.error("",e);
		}
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
	private static void populateTable (String table) throws Exception, SQLException, SessionException, IllegalStateException, IOException {
		PrearcDatabase.addSessions(PrearcDatabase.sessionDelegate.get());
	}
	
	private static void addSessions (final Collection<SessionData> ss) throws Exception, IllegalStateException, SQLException, SessionException {
		new SessionOp<Void>() {
			public java.lang.Void op () throws Exception, SQLException , SessionException {
				PreparedStatement statement = this.pdb.getPreparedStatement(null, PrearcDatabase.insertSql());
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
	 * @param tableWithSchema           The table to which to add the session   
	 * @throws SQLException
	 * @throws UniqueRowNotFoundException 
	 * @throws BadSessionInformationException 
	 */
	public static void addSession(final SessionData s) throws Exception, SQLException, SessionException {
		PrearcDatabase.checkArgs(s);
		new SessionOp<Void>(){
			public java.lang.Void op() throws Exception, SQLException, SessionException {
				int rowCount = PrearcDatabase.numDuplicateSessions(s.getFolderName(),s.getTimestamp(),s.getProject());
				if (rowCount >= 1) {
					throw new SessionException("Trying to add an existing session");
				}
				else {
					PreparedStatement statement = this.pdb.getPreparedStatement(null,PrearcDatabase.insertSql());
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
	 * Parse the given uri and return a list of sessions in the database.
	 * @param uri
	 * @return
	 * @throws java.util.IllegalFormatException Thrown if the uri cannot be parsed 
	 * @throws SQLException Thrown if there is an issue with the database connection.
	 * @throws SessionException 
	 */
	public static List<SessionData> getProjects (String uri) throws java.util.IllegalFormatException, Exception, SQLException, SessionException {
		final PrearcUriParserUtils.ProjectsParser parser = new PrearcUriParserUtils.ProjectsParser(new PrearcUriParserUtils.UriParser(XNATApplication.PREARC_PROJECT_URI));
		final List<String> projects = parser.readUri(uri);
		return new SessionOp<List<SessionData>>(){
			public List<SessionData> op() throws SQLException, SessionException, Exception{
				List<SessionData> ls = new ArrayList<SessionData>();
				String sql = DatabaseSession.PROJECT.allMatchesSql(projects.toArray(new String[projects.size()]));
				ResultSet rs;
				try {
					rs = this.pdb.executeQuery(null, sql, null);
				}
				catch (DBPoolException e) {
					throw new Exception(e.getMessage());
				}
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
	public static SessionData getSession (String uri) throws java.util.IllegalFormatException, Exception, SQLException, SessionException {
		final PrearcUriParserUtils.SessionParser parser = new PrearcUriParserUtils.SessionParser(new PrearcUriParserUtils.UriParser(XNATApplication.PREARC_SESSION_URI));
		final Map<String,String> sess = parser.readUri(uri);
		return new SessionOp<SessionData>() {
			public SessionData op() throws Exception, SQLException, SessionException {
				return PrearcDatabase.getSession(sess.get("SESSION_LABEL"), sess.get("SESSION_TIMESTAMP"), sess.get("PROJECT_ID"));
			}
		}.run();
	}
	
//	public static synchronized SessionData getOrCreateSession(final SessionData _s) throws java.util.IllegalFormatException, Exception, SQLException, SessionException{
//		SessionData s = _s;
//		try  {
//			PrearcDatabase.getSession(_s.getFolderName(),_s.getTimestamp(),_s.getProject());
//		}
//		catch (SessionException e) {
//			PrearcDatabase.addSession(_s);
//			s = PrearcDatabase.getSession(_s.getFolderName(),_s.getTimestamp(),_s.getProject());
//		}
//		return s;
//	}
		
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
		return "INSERT INTO " + PrearcDatabase.tableWithSchema + " VALUES(" + StringUtils.join(ss.toArray(),',') + ")";
	}
	
	/**
	 * Recreate the database from scratch. This is an expensive operation.
	 * @throws SQLException
	 * @throws SAXException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public static void refresh() throws IllegalStateException, Exception, SQLException, SessionException, IOException {
		try {
			PrearcDatabase.deleteRows();
			PrearcDatabase.populateTable(PrearcDatabase.tableWithSchema);
			// PrearcDatabase.conn.commit();
		}
		catch (SQLException e) {
//			try {
//				// PrearcDatabase.conn.rollback();
//			}
//			catch (SQLException s) {
//				throw s;
//			}
			throw e;
		}
		catch (SessionException e) {
//			try {
//				// PrearcDatabase.conn.rollback();
//			}
//			catch (SQLException s) {
//				throw s;
//			}
			throw e;
		}
		finally {
			// PrearcDatabase.conn.close();
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
	private static synchronized boolean _moveToProject (final String sess, final String timestamp, final String proj, final String newProj) throws Exception, SessionException, SyncFailedException, SQLException{
		if (null == newProj || newProj.isEmpty()) {
			throw new SessionException("New project argument is null or empty");
		}
		final SessionData sd = PrearcDatabase.getSession(sess, timestamp, proj);
				
		LockAndSync<java.lang.Void> l =  new LockAndSync<java.lang.Void>(sess,timestamp,proj,sd.getStatus()) {
			java.lang.Void extSync() throws SyncFailedException {
				PrearcDatabase.sessionDelegate.move(sd, newProj);
				return null;
			}
			void cacheSync() throws SQLException, SessionException, Exception {
				PrearcDatabase.modifySession(sess, timestamp, proj, new SessionOp<Void>(){
					public Void op () throws SQLException, SessionException, Exception {
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
		};
		boolean ran = true;
		Exception e = null;
		try {
			ran = l.run();
		}
		catch (Exception _e) {
			logger.error("",_e);
			e =  _e;
			ran = false;
		}
		
		if(!ran){
			throw new SyncFailedException("Operation Failed: " + e.getMessage());
		}		
		return true;
	}
	
	public static boolean moveToProject (final String sess, final String timestamp, final String proj, final String newProj) throws Exception, SessionException, SyncFailedException, SQLException {
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
	public static Map<SessionDataTriple, Boolean> archive(final List<PrearcSession> sessions, final boolean allowDataDeletion, final boolean overwrite, final XDATUser user, final Set<StatusListenerI> listeners) throws Exception, SQLException, SessionException, SyncFailedException, IllegalStateException {
		List<SessionDataTriple> ss= new ArrayList<SessionDataTriple>();
		
		for(PrearcSession map:sessions){
			ss.add(SessionDataTriple.fromPrearcSession(map));
		}
		
		final Map<SessionDataTriple, Boolean> ret = PrearcDatabase.markSessions(ss, PrearcUtils.PrearcStatus.ARCHIVING);
		new Thread() {
			public void run() {
				final java.util.Iterator<PrearcSession> i = sessions.iterator();
					while(i.hasNext()){
						PrearcSession _s = i.next();
						try {
							PrearcDatabase.archive(_s,allowDataDeletion,overwrite,user,listeners);
						} catch (SyncFailedException e) {
							logger.error("",e);
						}
				}		
			}
		}.start();
		return ret;
	}
	
	
	public static String archive (PrearcSession session, boolean allowDataDeletion, boolean overwrite, XDATUser user, Set<StatusListenerI> listeners) throws SyncFailedException {
		final PrearcSessionArchiver archiver;
		try {
			archiver = Archiver.buildArchiver(session, allowDataDeletion, overwrite, user);
		}catch (Exception e1) {
			throw new IllegalStateException(e1);
		}
		
		ListenerUtils.addListeners(listeners, archiver);
		
		final String prearcDIR=session.getFolderName();
		final String timestamp=session.getTimestamp();
		final String project=session.getProject();
		
		
		final SessionData sd;
		try {
			sd = session.getSessionData();
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
				
		LockAndSync<String> l = new LockAndSync<String>(prearcDIR,timestamp,project,sd.getStatus()) {
			String extSync() throws SyncFailedException {
				try {
					return archiver.call();
				} catch (Exception e) {
					throw new SyncFailedException(e.getMessage());
				}
			}
			void cacheSync() throws SQLException, SessionException, Exception {
				PrearcDatabase.modifySession(sess, timestamp, proj, new SessionOp<Void>(){
					public Void op () throws SQLException, SessionException, Exception {
					    PoolDBUtils.ExecuteNonSelectQuery(DatabaseSession.deleteSessionSql(sess,timestamp,proj), null,null);
						return null;
					}
				});
			}
			@Override
			boolean checkStatus() {
				return sd.getStatus().equals(PrearcStatus.ARCHIVING);
			}
		};
		boolean ran = true;
		Exception e = null;
		try {
			ran = l.run();
				
		}
		catch (Exception _e) {
			logger.error("",_e);
			e =  _e;
			ran = false;
		}
		
		if(!ran){
			throw new SyncFailedException("Operation Failed: " + e.getMessage());
		}		
		return l.s;
	}
	
	public static void buildSession (final File sessionDir, final String session, final String timestamp, final String project) throws Exception, SyncFailedException, SQLException, SessionException {
		final SessionData sd = PrearcDatabase.getSession(session, timestamp, project);
		try {
			new LockAndSync<java.lang.Void>(session,timestamp,project,sd.getStatus()) {
				java.lang.Void extSync() throws SyncFailedException {
					
					final Boolean r=new XNATSessionBuilder(sessionDir,new File(sessionDir.getPath() + ".xml"),project,true).call();
					
					if(!r){
						throw new SyncFailedException("Error building session");
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
	
	protected static boolean markSession(SessionDataTriple ss, PrearcUtils.PrearcStatus s) throws Exception, SQLException, SessionException{
		return PrearcDatabase.setStatus(ss.getFolderName(), ss.getTimestamp(), ss.getProject(), s);
	}
	
	protected static Map<SessionDataTriple, Boolean> markSessions (List<SessionDataTriple> ss, PrearcUtils.PrearcStatus s) throws Exception, SQLException, SessionException{
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
	public static Map<SessionDataTriple, Boolean> moveToProject(final List<SessionDataTriple> ss, final String newProj) throws Exception, SQLException, SessionException, SyncFailedException, IllegalStateException {
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
								} catch (Exception e) {
									logger.error(e);
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
	public static Map<SessionDataTriple, Boolean> deleteSession(final List<SessionDataTriple> ss) throws Exception, SQLException, SessionException, SyncFailedException, IllegalStateException {
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
					} catch (Exception e) {
						logger.error(e);
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
	public static boolean moveToProject (String uri) throws Exception, SessionException, SyncFailedException, SQLException {
		final PrearcUriParserUtils.SessionParser parser = new PrearcUriParserUtils.SessionParser(new PrearcUriParserUtils.UriParser(XNATApplication.PREARC_SESSION_URI));
		Map<String,String> sess = parser.readUri(uri);
		return PrearcDatabase.moveToProject(sess.get("SESSION_LABEL"), sess.get("SESSION_TIMESTAMP"), sess.get("PROJECT_ID"), parser.i.f.getValues("dest"));		
	}
	

	/**
	 * Set the status of an existing session. All arguments must be non-null and non-empty. Allows the user to set an inprocess status (i.e a status that begins with '_')
	 * @param sess
	 * @param timestamp
	 * @param proj
	 * @param status
	 * @throws SQLException
	 * @throws SessionException
	 */
	public static boolean setStatus (final String sess, final String timestamp, final String proj, final PrearcUtils.PrearcStatus status) throws Exception, SQLException, SessionException {
		return setStatus(sess,timestamp,proj,status,false);
	}
	
	/**
	 * Set the status of an existing session. All arguments must be non-null and non-empty. Allows the user to set an inprocess status (i.e a status that begins with '_')
	 * @param sess
	 * @param timestamp
	 * @param proj
	 * @param status
	 * @throws SQLException
	 * @throws SessionException
	 */
	public static boolean setStatus (final String sess, final String timestamp, final String proj, final PrearcUtils.PrearcStatus status, boolean overrideLock) throws Exception, SQLException, SessionException {
		if (!overrideLock && PrearcDatabase.isLocked(sess, timestamp, proj)) {
			return false;
		}
		PrearcDatabase.unsafeSetStatus(sess, timestamp, proj, status); 
		return true;
	}
	
	/**
	 * Set the status of a session, accept the status 
	 * as a string and before setting it first check that 
	 * the given status isn't one that can lock a 
	 * session (i.e begins with '_').
	 * 
	 * However a status of "_RECEIVING" is allowed because it 
	 * allows the sys admin to lock a session directory if they 
	 * need to mess with it manually.
	 * 
	 * @return
	 */
	public static boolean setStatus (final String sess, final String timestamp, final String proj, final String status) throws Exception, SQLException, SessionException {
		PrearcUtils.PrearcStatus p = PrearcUtils.PrearcStatus.valueOf(status);
		if (p != null){
			if (PrearcUtils.inProcessStatusMap.containsValue(p)) {
				throw new SessionException("Cannot set session status to " + status);
			}
			else {
				return PrearcDatabase.setStatus(sess,timestamp,proj,p);
			}	
		}
		else {
			throw new SessionException ("Status " + status.toString() + " is not a legitimate status");
		}
	}
	
	/**
	 * Set the status of an existing session. No check is performed to see if the database is locked. Allows the user to set an inprocess status (i.e a status that begins with '_') 
	 * @param sess
	 * @param timestamp
	 * @param proj
	 * @param status
	 * @throws Exception
	 * @throws SQLException
	 * @throws SessionException
	 */
	public static void unsafeSetStatus (final String sess, final String timestamp, final String proj, final PrearcUtils.PrearcStatus status) throws Exception, SQLException, SessionException {
		if (null == status) {
			throw new SessionException ("Status argument is null or empty");
		}
		PrearcDatabase.modifySession(sess, timestamp, proj, new SessionOp<Void>() {
			public Void op () throws SQLException, SessionException, Exception {
				PoolDBUtils.ExecuteNonSelectQuery(DatabaseSession.STATUS.updateSessionSql(sess, timestamp, proj, status), null, null);
				return null;
			}
		}); 
	}
	/**
	 * Set the status given a uri specifying the project, timestamp and session and the new status. Allows the user to set an inprocess status (i.e a status that begins with '_')
	 * @param uri
	 * @param status
	 * @return
	 * @throws Exception
	 * @throws SQLException
	 * @throws SessionException
	 */
	public static boolean setStatus(final String uri, final PrearcUtils.PrearcStatus status) throws Exception, SQLException, SessionException {
		return setStatus(uri,status,false);
	}
	
	/**
	 * Set the status given the uri specifying the project,timestamp and session, and an 
	 * override lock that will that will set status even if the session is locked. Allows 
	 * the user to set an inprocess status (i.e a status that begins with '_')
	 * @param uri
	 * @param status
	 * @param overrideLock
	 * @return
	 * @throws Exception
	 * @throws SQLException
	 * @throws SessionException
	 */
	public static boolean setStatus(final String uri, final PrearcUtils.PrearcStatus status, boolean overrideLock) throws Exception, SQLException, SessionException {
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
	public static boolean deleteCacheRow (final String sess, final String timestamp, final String proj) throws Exception, SQLException, SessionException, SyncFailedException {
		final SessionData sd = PrearcDatabase.getSession(sess, timestamp, proj);
		new LockAndSync<java.lang.Void>(sess,timestamp,proj,sd.getStatus()){
			protected boolean checkStatus(){
				return PrearcStatus._DELETING.equals(this.status);
			}			
			
			java.lang.Void extSync() throws SyncFailedException {
				return null;
			}
			void cacheSync() throws Exception, SQLException, SessionException {
				PrearcDatabase.modifySession(sess,timestamp,proj, new SessionOp<Void>(){
					public java.lang.Void op () throws SQLException, SessionException, Exception {
						PoolDBUtils.ExecuteNonSelectQuery(DatabaseSession.deleteSessionSql(sess,timestamp,proj), null, null);
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
	private static boolean _deleteSession (final String sess, final String timestamp, final String proj) throws Exception, SQLException, SessionException, SyncFailedException {
		final SessionData sd = PrearcDatabase.getSession(sess, timestamp, proj);
		LockAndSync<java.lang.Void> l = new LockAndSync<java.lang.Void>(sess,timestamp,proj,sd.getStatus()){
			protected boolean checkStatus (){
				return PrearcStatus.DELETING.equals(this.status);
			}			
			
			java.lang.Void extSync() throws SyncFailedException {
				sessionDelegate.delete(sd);
				return null;
			}
			void cacheSync() throws Exception, SQLException, SessionException {
				PrearcDatabase.withSession(sess,timestamp,proj, new SessionOp<Void>(){
					public java.lang.Void op () throws SQLException, SessionException, Exception {
						PoolDBUtils.ExecuteNonSelectQuery(DatabaseSession.deleteSessionSql(sess,timestamp,proj), null, null);
						return null;
					}
				});
			}
		};
		
		boolean ran = true;
		Exception e = null;
		try {
			ran = l.run();
				
		}
		catch (Exception _e) {
			logger.error("",_e);
			e =  _e;
			ran = false;
		}
		
		if(!ran){
			throw new SyncFailedException("Operation Failed: " + e.getMessage());
		}		
		return true;
	}
	
	private static boolean _unsafeDeleteSession (final String sess, final String timestamp, final String proj) throws Exception, SQLException, SessionException, SyncFailedException {
		final SessionData sd = PrearcDatabase.getSession(sess, timestamp, proj);
		new LockAndSync<java.lang.Void>(sess,timestamp,proj,sd.getStatus()){
			protected boolean checkStatus (){
				return true;
			}			
			
			java.lang.Void extSync() throws SyncFailedException {
				sessionDelegate.delete(sd);
				return null;
			}
			void cacheSync() throws Exception, SQLException, SessionException {
				PrearcDatabase.modifySession(sess,timestamp,proj, new SessionOp<Void>(){
					public java.lang.Void op () throws SQLException, SessionException, Exception{
						PoolDBUtils.ExecuteNonSelectQuery(DatabaseSession.deleteSessionSql(sess,timestamp,proj), null, null);
						return null;
					}
				});
			}
		}.run();
		return true;
	}
	
	public static boolean deleteSession(final String sess, final String timestamp, final String proj) throws Exception, SQLException, SessionException, SyncFailedException {
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
		abstract void cacheSync () throws SQLException, SessionException, Exception;
		boolean run() throws SQLException, SessionException, SyncFailedException, Exception {
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
				logger.error("",e);
				PrearcDatabase.unLockSession(this.sess, this.timestamp, this.proj);
				throw e;
			} 
			catch (SessionException e) {
				logger.error("",e);
				PrearcDatabase.unLockSession(this.sess, this.timestamp, this.proj);
				throw e;
			}
			catch (java.io.SyncFailedException e) {
				logger.error("",e);
				PrearcDatabase.unLockSession(this.sess, this.timestamp, this.proj);
				PrearcDatabase.setStatus(sess, timestamp, proj , PrearcUtils.PrearcStatus.ERROR);
				throw e;
			}
			catch (Exception e) {
				logger.error("",e);
				PrearcDatabase.unLockSession(this.sess, this.timestamp, this.proj);
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
	protected static boolean isLocked (String uri) throws Exception, SQLException, SessionException {
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
	public static boolean isLocked(final String sess, final String timestamp, final String proj) throws Exception, SQLException, SessionException {
		SessionData sd = PrearcDatabase.getSession(sess, timestamp,proj);
		return PrearcUtils.inProcessStatusMap.containsValue(sd.getStatus());
	}
	
	public static void unLockSession(String uri) throws Exception, SQLException, SessionException {
		final PrearcUriParserUtils.SessionParser parser = new PrearcUriParserUtils.SessionParser(new PrearcUriParserUtils.UriParser(XNATApplication.PREARC_SESSION_URI));
		final Map<String,String> sess = parser.readUri(uri);
		PrearcDatabase.unLockSession(sess.get("SESSION_LABEL"), sess.get("SESSION_TIMESTAMP"), sess.get("PROJECT_ID"));
	}
	
	protected static void unLockSession(final String sess, final String timestamp, final String proj) throws Exception, SQLException, SessionException {
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
	protected static boolean lockSession(String uri) throws Exception, SQLException, SessionException {
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
	protected static boolean lockSession (final String sess, final String timestamp, final String proj) throws Exception, SQLException, SessionException {
		SessionData sd = PrearcDatabase.getSession(sess, timestamp, proj);
		if (PrearcUtils.inProcessStatusMap.containsKey(sd.getStatus())) {
			final PrearcUtils.PrearcStatus inp = PrearcUtils.inProcessStatusMap.get(sd.getStatus());
			PrearcDatabase.modifySession(sess,timestamp,proj, new SessionOp<Void>(){
				public java.lang.Void op () throws SQLException, SessionException , Exception {
					PoolDBUtils.ExecuteNonSelectQuery(DatabaseSession.STATUS.updateSessionSql(sess,timestamp,proj,inp), null, null);
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
	public static boolean deleteSession (String uri) throws Exception, SQLException, SessionException, SyncFailedException {
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
	public static SessionData getSession(final String sess, final String timestamp, final String proj) throws Exception, SQLException, SessionException {
		return PrearcDatabase.withSession(sess, timestamp, proj, new SessionOp<SessionData>() {
			public SessionData op () throws SQLException, Exception{
				ResultSet rs = this.pdb.executeQuery(null, DatabaseSession.findSessionSql(sess, timestamp, proj), null); 
				rs.next();
				return DatabaseSession.fillSession(rs);
			}
		});
	}
	
	/**
	 * Return all sessions with the given session, timestamp and project. There should only be one row returned, but if not
	 * this function will return all the duplicate rows. 
	 * @param sess
	 * @param timestamp
	 * @param proj
	 * @return
	 * @throws Exception
	 * @throws SQLException
	 * @throws SessionException
	 */
	private static Collection<SessionData> unsafeGetSession (final String sess, final String timestamp, final String proj) throws Exception, SQLException, SessionException {
		return new SessionOp<Collection<SessionData>>() {
			public Collection<SessionData> op () throws SQLException, Exception {
				ResultSet rs = this.pdb.executeQuery(null, DatabaseSession.findSessionSql(sess,timestamp,proj), null);
				Collection<SessionData> ss = new ArrayList<SessionData>();
				while (rs.next()){
					ss.add(DatabaseSession.fillSession(rs));
				}
				return ss;
			}
		}.run();
	}
	
	public static List<SessionData> getAllSessions () throws Exception, SessionException, SQLException {
		final List<SessionData> sds = new ArrayList<SessionData>();
		new SessionOp<Void>() {
			public Void op() throws SQLException, Exception {
				ResultSet rs = pdb.executeQuery(null, DatabaseSession.getAllRows(), null);
				while (rs.next()) {
					sds.add(DatabaseSession.fillSession(rs));
				}
				return null;
			}
		}.run();
		return sds;
	}
	
	
	/**
	 * Search for a session given its UID.
	 * @param uid
	 * @return
	 * @throws SQLException
	 * @throws SessionException Thrown if the given arguments match more than one session 
	 */
	public static Collection<SessionData> getSessionByUID(final String uid) throws Exception, SQLException, SessionException {
		return new SessionOp<Collection<SessionData>>() {
			public Collection<SessionData> op() throws SQLException, Exception {
				List<SessionData> matches=new ArrayList<SessionData>();
				ResultSet rs = this.pdb.executeQuery(null, DatabaseSession.TAG.findSql(uid), null); 
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
	public static int numDuplicateSessions(final String sess, final String timestamp, final String proj) throws Exception, SQLException, SessionException {
		return new SessionOp<Integer>() {
			public Integer op() throws SQLException, SessionException, Exception {
				ResultSet rs = this.pdb.executeQuery(null, DatabaseSession.countSessionSql(sess,timestamp, proj), null);
				rs.next();
				return rs.getInt(1);
			}
		}.run();
	}
	

	public static synchronized SessionData getOrCreateSession (final String project, final String suid, final SessionData s) throws SQLException, SessionException, Exception {
		return new SessionOp<SessionData>(){
			public SessionData op() throws SQLException, SessionException, Exception {
				String [] constraints = {
										  DatabaseSession.PROJECT.searchSql(project), 
										  DatabaseSession.TAG.searchSql(suid) 		
										};		
				ResultSet rs = this.pdb.executeQuery (null, DatabaseSession.findSessionSql(constraints), null);
				if (rs.next()) {
					return DatabaseSession.fillSession(rs);
				}
				else {
					int dups = PrearcDatabase.numDuplicateSessions(s.getFolderName(), s.getTimestamp(), s.getProject());
					int suffix = 1;
					String suffixString = "";
					while (dups == 1) {
						suffixString = "_" + suffix;
						dups = PrearcDatabase.numDuplicateSessions(s.getFolderName() + suffixString, s.getTimestamp(), s.getProject());
						if (dups > 1) {
							throw new SessionException("Database is in a bad state, " + dups + "sessions (name : " + s.getFolderName() + " timestamp: " + s.getTimestamp() + " project : " + s.getProject());
						}
						suffix++;
					}
					
					s.setFolderName(s.getFolderName() + suffixString);
					s.setName(s.getName() + suffixString);
					s.setUrl(PrearcUtils.makeUri("/" + "prearchive/projects/" + s.getProject(), s.getTimestamp(), s.getFolderName()));
					
					PreparedStatement statement = this.pdb.getPreparedStatement(null,PrearcDatabase.insertSql());
					for (int i = 0; i < DatabaseSession.values().length; i++) {
						DatabaseSession.values()[i].setInsertStatement(statement, s);
					}
					statement.executeUpdate();
					SessionData tmp = PrearcDatabase.getSession(s.getFolderName(), s.getTimestamp(), s.getProject());
					return tmp;
				}
			}
		}.run();
	}

	/**
	 * Delete all the rows in the prearchive table.
	 * @throws SQLException
	 */
	private static void deleteRows() throws Exception, SQLException {
		try {
			new SessionOp<Void>(){
				public Void op() throws SQLException, Exception {
					PoolDBUtils.ExecuteNonSelectQuery("DROP TABLE IF EXISTS " + PrearcDatabase.tableWithSchema, null, null);
					PoolDBUtils.ExecuteNonSelectQuery(DatabaseSession.createTableSql(), null, null);
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
				public String op() throws SQLException, Exception {
					ResultSet r = this.pdb.executeQuery(null, "SELECT * FROM " + PrearcDatabase.tableWithSchema, null);
					return PrearcDatabase.showRows(r);
				}
			}.run();
		} catch (SessionException e) {}
		  catch (SQLException e) {
			  System.out.println(e.getMessage());
		  }
		  catch (Exception e) {}
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
		s.append("CREATE TABLE " + PrearcDatabase.tableWithSchema + "(");
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
	public static ArrayList<ArrayList<Object>> buildRows (final String[] projects) throws Exception, SQLException, SessionException {
		return new SessionOp<ArrayList<ArrayList<Object>>>(){
			public ArrayList<ArrayList<Object>> op() throws SQLException, SessionException, Exception {
				ArrayList<ArrayList<Object>> ao = new ArrayList<ArrayList<Object>>();
				if (projects.length > 0) {
					ResultSet rs = this.pdb.executeQuery(null, DatabaseSession.PROJECT.allMatchesSql(projects), null);
					ao=convertRStoList(rs);	
				}
				return ao;
			}
		}.run();
	}
	
	public static ArrayList<SessionData> getSessionsInProject (final String project) throws Exception, SQLException, SessionException {
		return new SessionOp<ArrayList<SessionData>>() {
			public ArrayList<SessionData> op() throws SQLException, SessionException, Exception {
				ArrayList<SessionData> ao = new ArrayList<SessionData>();
				String [] sdr = {project};
				ResultSet rs = this.pdb.executeQuery(null, DatabaseSession.PROJECT.allMatchesSql(sdr), null);
				while (rs.next()) {
					ao.add(DatabaseSession.fillSession(rs));
				}
				return ao;
			}
		}.run();
	}
	
	/**
	 * Build a list of all sessions in the prearchive.
	 * @return
	 * @throws Exception
	 * @throws SQLException
	 * @throws SessionException
	 */
	public static ArrayList<ArrayList<Object>> buildRows () throws Exception, SQLException, SessionException {
		return new SessionOp<ArrayList<ArrayList<Object>>>(){
			public ArrayList<ArrayList<Object>> op() throws SQLException, SessionException, Exception {
				ArrayList<ArrayList<Object>> ao = new ArrayList<ArrayList<Object>>();
				ResultSet rs = this.pdb.executeQuery(null, DatabaseSession.allMatchesSql(), null);
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
	public static ArrayList<ArrayList<Object>> buildRows (final Collection<SessionDataTriple> ss) throws Exception, SQLException, SessionException {
		return new SessionOp<ArrayList<ArrayList<Object>>>(){
			public ArrayList<ArrayList<Object>> op() throws SQLException, SessionException, Exception {
				ArrayList<ArrayList<Object>> ao = new ArrayList<ArrayList<Object>>();
				for(final SessionDataTriple s: ss){
					ResultSet rs = this.pdb.executeQuery(null, DatabaseSession.findSessionSql(s.getFolderName(), s.getTimestamp(), s.getProject()), null);
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
		ResultSet rs = PrearcDatabase.conn.createStatement().executeQuery("SHOW COLUMNS FROM " + PrearcDatabase.tableWithSchema);
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
		// Connection conn;
		PoolDBUtils pdb;
		public void createConnection() throws SQLException {
			// this.conn = DriverManager.getConnection("jdbc:h2:" + PrearcDatabase.prearcPath + dbName, "sa", "");
			this.pdb = new PoolDBUtils();
		}
		public void closeConnection() throws SQLException {
			this.pdb.closeConnection();
		}
		public void rollbackConnection () throws SQLException {
			// this.conn.rollback();
		}
		public abstract T op() throws SQLException, SessionException, Exception;
		public T run () throws SQLException, SessionException, Exception {
			this.createConnection();
			Object o = null;
			try {
				o = this.op();
			}
			catch (Exception e) {
				System.out.println(e.getMessage());
				throw e;
			}
			finally {
				closeConnection();
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
	private static void checkSession (String sess, String timestamp, String proj) throws Exception, SQLException, SessionException {
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
	
	private static void checkUniqueRow (String sess, String timestamp, String proj) throws Exception, SQLException, SessionException {
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
	
    private static <T extends Object> T withSession (String sess, String timestamp, String proj, SessionOp<T> op) throws Exception, SQLException, SessionException {
		PrearcDatabase.checkSession(sess,timestamp,proj);
		return op.run();
	}
    
    private static <T extends Object> T modifySession (final String sess, final String timestamp, final String proj, SessionOp<T> op) throws Exception, SQLException, SessionException {
    	withSession(sess,timestamp,proj,new SessionOp<java.lang.Void>() {
    		public Void op() throws SQLException, Exception {
    			PoolDBUtils.ExecuteNonSelectQuery(DatabaseSession.LASTMOD.updateSessionSql(sess, timestamp, proj, Calendar.getInstance().getTime()), null, null);
				return null;
			}
    	});
    	return op.run();
    }
}
