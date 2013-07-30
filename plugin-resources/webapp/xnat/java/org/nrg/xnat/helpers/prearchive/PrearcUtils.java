/*
 * org.nrg.xnat.helpers.prearchive.PrearcUtils
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 8:47 PM
 */
package org.nrg.xnat.helpers.prearchive;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang.StringUtils;
import org.nrg.xdat.om.ArcProject;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.SecurityManager;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.InvalidPermissionException;
import org.nrg.xnat.helpers.prearchive.PrearcTableBuilder.Session;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.UriParserUtils;
import org.nrg.xnat.restlet.util.RequestUtil;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class PrearcUtils {
	private final static Logger logger = LoggerFactory.getLogger(PrearcUtils.class);
	public static final String COMMON = "Unassigned";

	public static final String ROLE_SITE_ADMIN = "Administrator";
	public static final String PROJECT_SECURITY_TASK = "xnat:mrSessionData/project";

	public static final String APPEND = "append";

	public static final String DELETE = "delete";

	public static final String PREARC_TIMESTAMP = "PREARC_TIMESTAMP";

	public static final String PREARC_SESSION_FOLDER = "PREARC_SESSION_FOLDER";

	public enum PrearcStatus {
		RECEIVING,
        QUEUED_BUILDING,
		BUILDING,
		READY, 
        QUEUED_ARCHIVING,
		ARCHIVING,
		CONFLICT,
		ERROR, 
		DELETING,
		MOVING,
		_RECEIVING,_QUEUED_BUILDING,_BUILDING,_QUEUED_ARCHIVING,_ARCHIVING,_DELETING,_MOVING,_CONFLICT;

		public static boolean potentiallyReady(PrearcStatus status) {
			return (status==null || status.equals(READY));			    
		}
	};

	private static Logger logger() { return LoggerFactory.getLogger(PrearcUtils.class); }
	
	public static final Map<PrearcStatus, PrearcStatus> inProcessStatusMap = createInProcessMap();
	
	public static Map<PrearcStatus, PrearcStatus> createInProcessMap () {
		Map<PrearcStatus,PrearcStatus> map = new HashMap<PrearcStatus,PrearcStatus>();
		for (PrearcStatus s : PrearcStatus.values()){
			if (s != PrearcStatus.READY && s != PrearcStatus.ERROR && s.toString().charAt(0) != '_') {
				map.put(s, PrearcStatus.valueOf("_" + s.name()));
			}
		}
		return map;
	}
	
	public static ArrayList<String> getProjects (final XDATUser user, String requestedProject) throws Exception {
		ArrayList<String> projects = new ArrayList<String>();
		if(requestedProject!=null){
			if(requestedProject.contains(",")){
				String [] _tmp = StringUtils.split(requestedProject,',');
				for (int i = 0; i < _tmp.length; i++) {
				    projects.add(cleanProject(_tmp[i]));
				}
			}else{
				projects.add(cleanProject(requestedProject));
			}
		}else{
			for (final List<String> row : user.getQueryResults("xnat:projectData/ID", "xnat:projectData")) {
				final String id = row.get(0);
				if (projects.contains(id))
					continue;
				try {
					if (canModify(user,id)) {
						projects.add(id);
					}
				} catch (Exception e) {
					logger().error("Exception caught testing prearchive access", e);
				}
			}
			// if the user is an admin also add unassigned projects
			if (user.checkRole(ROLE_SITE_ADMIN)) {
				projects.add(null);
			}
		}
		return projects;
	}
	
	private static String cleanProject(final String p){
		if(COMMON.equals(p)){
			return null;
		}else{
			return p;
		}
	}
	
	public static boolean canModify(final XDATUser user, final String p) throws Exception{
		if(user.checkRole(PrearcUtils.ROLE_SITE_ADMIN)){
			return true;
		}else if(p==null){
			return false;
		}else{
			return user.canAction("xnat:mrSessionData/project", p, SecurityManager.CREATE);
		}
	}

	/**
	 * Retrieves the File reference to the prearchive root directory for the
	 * named project.
	 * 
	 * 4/30/12- removed requirement that user object be not null.  null users are allowed here for administrative code that happens outside the permissions structure (like logging).
	 * 4/30/12- refactored to prevent unnecessary database queries
	 *
	 * @param user
	 * @param project
	 *            project abbreviation or alias
	 * @return prearchive root directory
	 * @throws ResourceException
	 *             if the named project does not exist, or if the user does not
	 *             have create permission for it, or if the prearchive directory
	 *             does not exist.
	 */
	public static File getPrearcDir(final XDATUser user, final String project, final boolean allowUnassigned) throws IOException,InvalidPermissionException,Exception {
		String prearcPath;
		if(project==null || project.equals(COMMON)){
			if (allowUnassigned || user==null || user.checkRole(ROLE_SITE_ADMIN)) {
				prearcPath=ArcSpecManager.GetInstance(false).getGlobalPrearchivePath();
			}else{
				throw new InvalidPermissionException("user " + user.getUsername() + " does not have permission to access the Unassigned directory ");
			}
		}else{
			//Refactored to remove unnecessary database hits.  It only needs to hit the xnat_projectdata table if the query is using a project alias rather than a project id.  TO
			ArcProject p=ArcSpecManager.GetInstance().getProjectArc(project);
			if(p!=null){
				if (user!=null && !user.canAction(PROJECT_SECURITY_TASK, project, SecurityManager.CREATE)) {
					throw new InvalidPermissionException("user " + user.getUsername() + " does not have create permissions for project " + project);
				}
				prearcPath=ArcSpecManager.GetInstance().getPrearchivePathForProject(project);
			}else{
				//check to see if it used a project alias
				XnatProjectdata proj=XnatProjectdata.getProjectByIDorAlias(project, user, false);
				if(proj!=null){
					if (user!=null && !user.canAction(PROJECT_SECURITY_TASK, project, SecurityManager.CREATE)) {
						throw new InvalidPermissionException("user " + user.getUsername() + " does not have create permissions for project " + project);
					}
					prearcPath=proj.getPrearchivePath();
				}else{
					throw new IOException("No project named " + project);
				}
			}

			if (null == prearcPath) {
				final String message = "Unable to retrieve prearchive path for project " + project;
				logger().error(message);
				throw new Exception(message);
			}
		}
		final File prearc = new File(prearcPath);
		if (prearc.exists() && !prearc.isDirectory()) {
			final String message = "Prearchive directory is invalid for project " + project;
			logger().error(message);
			throw new Exception(message);
		}
		return prearc;
	}
	
	/**
	 * Checks that the user has permissions on the project. If getPrearcDir goes through without
	 * exceptions the user is valid. 
	 * 
	 * @param user
	 * @param project If the project is null, it is the unassigned project
	 *            project abbreviation or alias
	 * @return true if the user has permissions to access the project, false otherwise 
	 * @throws Exception 
	 * @throws IOException 
	 */
	public static boolean validUser (final XDATUser user, final String project, final boolean allowUnassigned) throws IOException, Exception {
		boolean valid = true;
		try {
			if (null == project) {
				PrearcUtils.getPrearcDir(user,PrearcUtils.COMMON,allowUnassigned); 
			}
			else {
				PrearcUtils.getPrearcDir(user,project,allowUnassigned);	 	
			}
		}
		catch (InvalidPermissionException e) {
			valid = false;
		}
		return valid;
	}

	/**
	 * A list of all projects in the prearchive.
	 *  
	 * @return a list of project names
	 */
	public static String[] allPrearchiveProjects () {
		File d = new File(ArcSpecManager.GetInstance(false).getGlobalPrearchivePath());
		return d.list( DirectoryFileFilter.INSTANCE );
	}

	private static final Pattern TSDIR_SECONDS_PATTERN = Pattern.compile("[0-9]{8}_[0-9]{6}");
	private static final String TSDIR_SECONDS_FORMAT = "yyyyMMdd_HHmmss";
	
	private static final Pattern TSDIR_MILLISECONDS_PATTERN = Pattern.compile("[0-9]{8}_[0-9]{9}");
	private static final String TSDIR_MILLISECONDS_FORMAT = "yyyyMMdd_HHmmssSSS"; 

	public  static final FileFilter isTimestampDirectory = new FileFilter() {
		public boolean accept(final File f) {
			return f.isDirectory() && (TSDIR_SECONDS_PATTERN.matcher(f.getName()).matches() || TSDIR_MILLISECONDS_PATTERN.matcher(f.getName()).matches()); 
		}
	};

	public static final Date parseTimestampDirectory(final String stamp) throws ParseException{
		final DateFormat format;
		if(stamp.length()==18){
			format = new SimpleDateFormat(TSDIR_MILLISECONDS_FORMAT);
		}else{
			format = new SimpleDateFormat(TSDIR_SECONDS_FORMAT);
		}

		return format.parse(stamp);
	}
	
	public  static final FileFilter isDirectory = new FileFilter() {
		public boolean accept(final File f) {
			return f.isDirectory();
		}
	};

	/**
	 * Creates a formatted timestamp using the {@link #TSDIR_MILLISECONDS_FORMAT} specification
     * and the U.S. locale.
	 * @return The formatted timestamp
	 */
	public static String makeTimestamp() {
	    final SimpleDateFormat formatter = new SimpleDateFormat(TSDIR_MILLISECONDS_FORMAT, Locale.US);
	    return formatter.format(new Date());
	}
	

	/**
	 * Checks for obvious problems with a session XML: existence, permissions.
	 * @param sessionXML The XML defining the session
	 * @return The {@link PrearcStatus} for the session.
	 */
	public static PrearcStatus checkSessionStatus(final File sessionXML) {
		if (!sessionXML.exists()) {
			return PrearcStatus.RECEIVING;
		}
		if (!sessionXML.isFile()) {
			logger().error("{} exists, but is not a file. ", sessionXML);
			return PrearcStatus.ERROR;
		}
		if (!sessionXML.canRead()) {
			logger().error("cannot read {}. " + sessionXML);
			return PrearcStatus.ERROR;
		}
		if (sessionXML.length() == 0) {
			logger().error("{} is empty. " + sessionXML);
			return PrearcStatus.ERROR;
		}
		return null;
	}
	
	public static java.util.Date timestamp2Date (java.sql.Timestamp t) {
		return new java.util.Date(t.getTime());
	}
	
	/**
	 * Create a blank session that will be used to populate a row in the prearchive table that will
	 * be filled later.
	 * 
	 * No attempt is made to create the necessary folder structure in the prearchive directory on the
	 * filesystem. 
	 * 
	 * The essential fields are set:
	 * - folderName
	 * - project
	 * - url
	 * - tag (the Study Instance UID)
	 * @param project
	 * @param sessionLabel
	 * @param tag
	 * @return
	 */
	public static SessionData blankSession (String project, String sessionLabel, String tag) throws IOException {
		if (sessionLabel == null || tag == null) {
			throw new IOException("Cannot create a SessionData object with a session label or study instance uid");
		}
		
		final File root;
        if (null == project) {
            root = new File(ArcSpecManager.GetInstance().getGlobalPrearchivePath());
        } else {
            //root = new File(project.getPrearchivePath());
        	root = new File (ArcSpecManager.GetInstance().getGlobalPrearchivePath() + "/" + project);
        }
        // doesn't currently exist only used to get pathname to create the URL
        final File tsdir;
        tsdir = new File(root, PrearcUtils.makeTimestamp());
        
        SessionData sess = new SessionData();
        sess.setFolderName(sessionLabel);
        sess.setName(sessionLabel);
        sess.setTimestamp(tsdir.getName());
        sess.setProject(project);
        sess.setUrl((new File(tsdir,sessionLabel)).getAbsolutePath());
        sess.setTag(tag);
        return sess;
	}
	
	public static void deleteProject (String project) throws SQLException, SessionException, Exception {
		ArrayList<SessionData> ss = PrearcDatabase.getSessionsInProject(project);
		Iterator<SessionData> i = ss.iterator();
		while (i.hasNext()) {
			SessionData s = i.next();
			PrearcDatabase.deleteSession(s.getFolderName(), s.getTimestamp(), s.getProject());
		}
	}
	
	public static final File getPrearcSessionDir(final XDATUser user, final String project, final String timestamp,final String session, final boolean allowUnassigned)
	throws IOException, InvalidPermissionException, Exception{
		if(user==null || timestamp==null || session==null){
			throw new IllegalArgumentException(String.format("Invalid prearchive session: user %s; timestamp %s; session %s",
			        user, timestamp, session));
		}
		return new File(new File(getPrearcDir(user, project,allowUnassigned),timestamp),session);
	}

	public static final FileFilter isSessionGeneratedFileFilter = new FileFilter() {
		private final Pattern conversionLogPattern = Pattern.compile("(\\w*)toxnat\\.log");
		private final Pattern scanCatalogPattern = Pattern.compile("scan_(\\d*)_catalog.xml");
		public boolean accept(final File f) {
			return scanCatalogPattern.matcher(f.getName()).matches()
			|| conversionLogPattern.matcher(f.getName()).matches();
		}
	};	
	
	public static void resetStatus(final XDATUser user,final String project, final String timestamp, final String session,final boolean allowUnassigned) throws IOException, InvalidPermissionException, Exception {
		SessionData deleted = null;
        try {
           deleted = PrearcDatabase.getSession(session,timestamp,project);
        PrearcDatabase.unsafeSetStatus(session, timestamp, project, PrearcStatus._DELETING);
        PrearcDatabase.deleteCacheRow(session,timestamp,project);
        } catch (SessionException e) {
        }

        addSession(user,project,timestamp,session,allowUnassigned);
        PrearcDatabase.setAutoArchive(session,timestamp,project,deleted.getAutoArchive());
        PrearcDatabase.setPreventAnon(session,timestamp,project,deleted.getPreventAnon());
        PrearcDatabase.setSource(session,timestamp,project,deleted.getSource());
        PrearcDatabase.setPreventAutoCommit(session,timestamp,project,deleted.getPreventAutoCommit());
    }
	
	public static void resetStatus(final XDATUser user,final String project, final String timestamp, final String session,final String uID, final boolean allowUnassigned) throws IOException, InvalidPermissionException, Exception {
		SessionData deleted = null;
		try {
			deleted = PrearcDatabase.getSession(session,timestamp,project);
			PrearcDatabase.unsafeSetStatus(session, timestamp, project, PrearcStatus._DELETING);
			PrearcDatabase.deleteCacheRow(session,timestamp,project);
		} catch (SessionException e) {
		}

		addSession(user,project,timestamp,session,uID, allowUnassigned);
		PrearcDatabase.setAutoArchive(session,timestamp,project,deleted.getAutoArchive());
        PrearcDatabase.setPreventAnon(session,timestamp,project,deleted.getPreventAnon());
        PrearcDatabase.setSource(session,timestamp,project,deleted.getSource());
        PrearcDatabase.setPreventAutoCommit(session,timestamp,project,deleted.getPreventAutoCommit());
    }
	
	public static void addSession(final XDATUser user,final String project, final String timestamp, final String session,final boolean allowUnassigned) throws IOException, InvalidPermissionException, Exception {
		addSession(user,project,timestamp,session,null,allowUnassigned);
	}
	
	public static void addSession(final XDATUser user,final String project, final String timestamp, final String session, final String uID,final boolean allowUnassigned) throws IOException, InvalidPermissionException, Exception {
		final Session s=PrearcTableBuilder.buildSessionObject(PrearcUtils.getPrearcSessionDir(user, project, timestamp, session,allowUnassigned), timestamp,project);
		final SessionData sd=s.getSessionData(PrearcDatabase.projectPath(project));
		if(s.getSessionXML()!=null){
			sd.setUrl((new File(s.getSessionXML().getParentFile(),s.getFolderName()).getAbsolutePath()));
		}
		if(StringUtils.isNotEmpty(uID))sd.setTag(uID);
		PrearcDatabase.addSession(sd);
	}
	
	public static String makeUri (final String urlBase, final String timestamp, final String folderName) {
		return StringUtils.join(new String[]{urlBase,"/".intern(),timestamp,"/".intern(),folderName});
	}

	public static Map<String,Object> parseURI(final String uri) throws MalformedURLException{
		return UriParserUtils.parseURI(uri).getProps();
	}

	public static String buildURI(final String project, final String timestamp, final String folderName){
		return StringUtils.join(new String[]{"/prearchive/projects/",(project==null)?PrearcUtils.COMMON:project,"/",timestamp,"/",folderName});
	}
	
	public static XFTTable convertArrayLtoTable(ArrayList<ArrayList<Object>> rows){
		XFTTable table = new XFTTable();
		table.initTable(PrearcDatabase.getCols());
		Iterator<ArrayList<Object>> i = rows.iterator();
		while(i.hasNext()) {
			table.insertRow(i.next().toArray());
		}
		return table;
	}

	public static String identifyProject(final Map<String,Object> params) throws MalformedURLException{
		if(params.containsKey(URIManager.PROJECT_ID)){
			return (String)params.get(URIManager.PROJECT_ID);
		}else if(params.containsKey(RequestUtil.DEST)){
			return (String)(parseURI((String)params.get(RequestUtil.DEST))).get(URIManager.PROJECT_ID);
		}
		return null;
	}

	public static final String TEMP_UNPACK = "temp-unpack";
	
	public static boolean isUnassigned(final SessionData sd){
		if(StringUtils.isEmpty(sd.getProject()) || sd.getProject().equals(COMMON)){
			return true;
		}else{
			return false;
		}
	}
	
	/*******************
	 * The prearchive logging code begins here.
	 * 
	 * In the future, we might want to move this to a database table.  However, the current prearchive table doesn't have a primary key column (really?).  
	 * So, there would be no way to reliably join from the logs table to the prearchive table.  Also, this would make more sense to do as part of a image session logging framework 
	 * which would capture a lot more than just preachive logs, but requires more requirements gathering.
	 * 
	 * As such, this is more of a stub implementation, that should probably change when the above problems are dealt with.  It will facilitate the current requirement, which 
	 * is just that we can show the last exception via REST.
	 */
	
	private static File getLogDir(final String project, final String timestamp, final String session) throws IOException, InvalidPermissionException, Exception{
		if(timestamp==null || session==null){
			throw new IllegalArgumentException(String.format("Invalid prearchive session: timestamp %s; session %s",
			        timestamp, session));
		}
		return new File(new File(new File(getPrearcDir(null, project,true),timestamp),session),"logs");
	}
	
	/**
	 * Logs a message for a particular prearchive session.  The log entry will be placed in a log file named with the current timestamp in a logs subdirectory.
	 * 
	 * @param data
	 * @param message
	 */
	public static void log(final SessionData data,final Throwable message){
		PrearcUtils.log(data.getProject(),data.getTimestamp(),data.getName(),message);
	}
	
	/**
	 * Logs a message for a particular prearchive session.  The log entry will be placed in a log file named with the current timestamp in a logs subdirectory.
	 * 
	 * @param project
	 * @param timestamp
	 * @param session
	 * @param message
	 */
	public static void log(final String project, final String timestamp, final String session, final Throwable message) {
		try {
			File logs=getLogDir(project,timestamp,session);
			if(!logs.exists()){
				logs.mkdirs();
			}
			FileUtils.writeStringToFile(new File(logs,Calendar.getInstance().getTimeInMillis()+".log"), message.getMessage());
		} catch (IOException e) {
			logger.error("",e);
		} catch (InvalidPermissionException e) {
			logger.error("",e);
		} catch (Exception e) {
			logger.error("",e);
		}
	}

    /**
     * Logs a message for a particular prearchive session.  The log entry will be placed in a log file named with the current timestamp in a logs subdirectory.
     *
     * @param path
     * @param message
     */
    public static void log(final File path, final Throwable message) {
        try {
            File logs=new File(path, "logs");
            if(!logs.exists()){
                logs.mkdirs();
            }
            FileUtils.writeStringToFile(new File(logs,Calendar.getInstance().getTimeInMillis()+".log"), message.getMessage());
        } catch (IOException e) {
            logger.error("",e);
        } catch (Exception e) {
            logger.error("",e);
        }
    }
	
	/**
	 * Get all of the log IDs for this prearchived session.  Returns an empty list when none are present.
	 * 
	 * @param project
	 * @param timestamp
	 * @param session
	 * @return
	 */
	public static Collection<String> getLogs(final String project, final String timestamp, final String session) {
		final Collection<String> logs=Lists.newArrayList();
		try {
			final File logDir=getLogDir(project,timestamp,session);
			if(logDir.exists()){
				final String[] files=logDir.list();
				if(files!=null){
					for(String f:files){
						logs.add(f.substring(0,f.indexOf(".log")));//strip off the .log so it would be seamless to not use physical log files here.
					}
				}
			}
		} catch (IOException e) {
			logger.error("",e);
			return null;
		} catch (InvalidPermissionException e) {
			logger.error("",e);
			return null;
		} catch (Exception e) {
			logger.error("",e);
			return null;
		}
		return logs;
	}
	
	/**
	 * Return the log entry for the specified ID (timestamp).  Returns null when it isn't found.
	 * 
	 * @param project
	 * @param timestamp
	 * @param session
	 * @param logId
	 * @return
	 */
	public static String getLog(final String project, final String timestamp, final String session, final String logId) {
		try {
			final File logDir=getLogDir(project,timestamp,session);
			if(logDir.exists()){
				final File log=new File(logDir,logId+".log");//the .log is hidden from log users to conceal implementation details 
				if(log.exists()){
					return FileUtils.readFileToString(log);
				}
			}
		} catch (IOException e) {
			logger.error("",e);
			return null;
		} catch (InvalidPermissionException e) {
			logger.error("",e);
			return null;
		} catch (Exception e) {
			logger.error("",e);
			return null;
		}
		return null;
	}
	
	/**
	 * Return the last log entry for this prearchived session.  When none are present, null is returned.
	 * 
	 * @param project
	 * @param timestamp
	 * @param session
	 * @return
	 */
	public static String getLastLog(final String project, final String timestamp, final String session) {
		try {
			final File logDir=getLogDir(project,timestamp,session);
			
			if(logDir.exists()){
				File lastFile=null;
				for(File f: logDir.listFiles()){
					if(lastFile==null){
						lastFile=f;
					}else{
						if(f.lastModified()>lastFile.lastModified()){
							lastFile=f;
						}
					}
				}
				if(lastFile!=null){
					return FileUtils.readFileToString(lastFile);
				}else{
					return null;
				}
			}
		} catch (IOException e) {
			logger.error("",e);
			return null;
		} catch (InvalidPermissionException e) {
			logger.error("",e);
			return null;
		} catch (Exception e) {
			logger.error("",e);
			return null;
		}
		return null;
	}
	
	
}
