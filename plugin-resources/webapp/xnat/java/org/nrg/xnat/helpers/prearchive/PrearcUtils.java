package org.nrg.xnat.helpers.prearchive;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.base.auto.AutoXnatProjectdata;
import org.nrg.xdat.security.SecurityManager;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.exception.InvalidPermissionException;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.restlet.resource.ResourceException;

public class PrearcUtils {
	static Logger logger = Logger.getLogger(PrearcUtils.class);

	public static final String COMMON = "Unassigned";

	public static final String ROLE_SITE_ADMIN = "Administrator";
	public static final String PROJECT_SECURITY_TASK = "xnat:mrSessionData/project";
	//replicated from XNATApplication.java. Please keep the two in sync.
	public static final String sessionUriTemplate = "prearchive/projects/{PROJECT_ID}/{SESSION_TIMESTAMP}/{SESSION_LABEL}";
	public static final String projectUriTemplate = "prearchive/projects/{PROJECT_ID}";

	public static final String APPEND = "append";

	public static final String DELETE = "delete";

	public static final String PREARC_TIMESTAMP = "PREARC_TIMESTAMP";

	public static final String PREARC_SESSION_FOLDER = "PREARC_SESSION_FOLDER";

	public enum PrearcStatus {
		RECEIVING,
		BUILDING,
		READY, 
		ARCHIVING,
		ERROR, 
		DELETING,
		MOVING,
		_RECEIVING,_BUILDING,_ARCHIVING,_DELETING,_MOVING
	};

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
				    projects.add(_tmp[i]);
				}
			}else{
				projects.add(requestedProject);
			}
		}else{
			for (final List<String> row : user.getQueryResults("xnat:projectData/ID", "xnat:projectData")) {
				final String id = row.get(0);
				if (projects.contains(id))
					continue;
				try {
					if (user.canAction("xnat:mrSessionData/project", id, SecurityManager.CREATE)) {
						projects.add(id);
					}
				} catch (Exception e) {
					logger.error("Exception caught testing prearchive access", e);
				}
			}
			// if the user is an admin also add unassigned projects
			if (user.checkRole(ROLE_SITE_ADMIN)) {
				projects.add(null);
			}
		}
		return projects;
	}
	/**
	 * Retrieves the File reference to the prearchive root directory for the
	 * named project.
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
	public static File getPrearcDir(final XDATUser user, final String project) throws IOException,InvalidPermissionException,Exception {
		if (null == user) {
			throw new InvalidPermissionException("null user object");
		}
		String prearcPath;
		if(project.equals(COMMON)){
			if (user.checkRole(ROLE_SITE_ADMIN)) {
				prearcPath=ArcSpecManager.GetInstance().getGlobalPrearchivePath();
			}else{
				throw new InvalidPermissionException("user " + user.getUsername() + " does not have permission to access the Unassigned directory ");
			}
		}else{
			XnatProjectdata projectData = AutoXnatProjectdata.getXnatProjectdatasById(project, user, false);
			if (null == projectData) {
				final List<XnatProjectdata> matches = AutoXnatProjectdata.getXnatProjectdatasByField("xnat:projectData/aliases/alias/alias", project, user, false);
				if (!matches.isEmpty()) {
					projectData = matches.get(0);
				}
			}
			if (null == projectData) {
				throw new IOException("No project named " + project);
			}
			try {
				if (user.canAction(PROJECT_SECURITY_TASK, projectData.getId(), SecurityManager.CREATE)) {
					prearcPath = projectData.getPrearchivePath();
				} else {
					throw new InvalidPermissionException("user " + user.getUsername() + " does not have create permissions for project " + projectData.getId());
				}
			} catch (final Exception e) {
				logger.error("Unable to check security for " + user.getUsername() + " on " + projectData.getId(), e);
				throw new Exception(e.getMessage());
			}

			if (null == prearcPath) {
				final String message = "Unable to retrieve prearchive path for project " + projectData.getId();
				logger.error(message);
				throw new Exception(message);
			}
		}
		final File prearc = new File(prearcPath);
		if (prearc.exists() && !prearc.isDirectory()) {
			final String message = "Prearchive directory is invalid for project " + project;
			logger.error(message);
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
	public static boolean validUser (final XDATUser user, final String project) throws IOException, Exception {
		boolean valid = true;
		try {
			if (null == project) {
				PrearcUtils.getPrearcDir(user,PrearcUtils.COMMON); 
			}
			else {
				PrearcUtils.getPrearcDir(user,project);	 	
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
		File d = new File(ArcSpecManager.GetInstance().getGlobalPrearchivePath());
		return d.list( DirectoryFileFilter.INSTANCE );
	}

	public  static final Pattern timestampPattern = Pattern.compile("[0-9]{8}_[0-9]{6}");

	public  static final FileFilter isTimestampDirectory = new FileFilter() {
		public boolean accept(final File f) {
			return f.isDirectory() && timestampPattern.matcher(f.getName()).matches();
		}
	};

	public  static final FileFilter isDirectory = new FileFilter() {
		public boolean accept(final File f) {
			return f.isDirectory();
		}
	};
	

	/**
	 * Checks for obvious problems with a session XML: existence, lock, permissions.
	 * @param sessionDir Directory holding the session
	 * @return
	 */
	public static PrearcStatus checkSessionStatus(final File sessionXML) {
		if (!sessionXML.exists()) {
			return PrearcStatus.RECEIVING;
		}
		if (!sessionXML.isFile()) {
			logger.error(sessionXML.toString() + " exists, but is not a file");
			return PrearcStatus.ERROR;
		}
		final FileOutputStream fos;
		try {
			fos = new FileOutputStream(sessionXML, true);
		} catch (FileNotFoundException e) {
			logger.error("File not found (but passed .exists()!)", e);
			return PrearcStatus.ERROR;
		}
		try {
//			final FileLock lock = fos.getChannel().tryLock();
//			if (null == lock)
//				return PrearcStatus.BUILDING;
		} catch (Exception e) {
			logger.error("Unable to check lock on session " + sessionXML, e);
			return PrearcStatus.ERROR;
		} finally {
			try { fos.close(); } catch (IOException ignore) {}
		}

		if (!sessionXML.canRead()) {
			System.out.println("able to obtain lock, but cannot read " + sessionXML);
			logger.error("able to obtain lock, but cannot read " + sessionXML);
			return PrearcStatus.ERROR;
		}

		return null;
	}
	public static java.util.Date timestamp2Date (java.sql.Timestamp t) {
		return new java.util.Date(t.getTime());
}

	public static final FileFilter isSessionGeneratedFileFilter = new FileFilter() {
		private final Pattern conversionLogPattern = Pattern.compile("(\\w*)toxnat\\.log");
		private final Pattern scanCatalogPattern = Pattern.compile("scan_(\\d*)_catalog.xml");
		public boolean accept(final File f) {
			return scanCatalogPattern.matcher(f.getName()).matches()
			|| conversionLogPattern.matcher(f.getName()).matches();
		}
	};
}
