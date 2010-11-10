package org.nrg.xnat.helpers.prearchive;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.List;
import java.util.regex.Pattern;

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

	private static final String ROLE_SITE_ADMIN = "Administrator";
	private static final String PROJECT_SECURITY_TASK = "xnat:mrSessionData/project";


	public enum PrearcStatus {
		RECEIVING, BUILDING, READY, ARCHIVING, ERROR
	};
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
			final FileLock lock = fos.getChannel().tryLock();
			if (null == lock)
				return PrearcStatus.BUILDING;
		} catch (IOException e) {
			logger.error("Unable to check lock on session " + sessionXML, e);
			return PrearcStatus.ERROR;
		} finally {
			try { fos.close(); } catch (IOException ignore) {}
		}

		if (!sessionXML.canRead()) {
			logger.error("able to obtain lock, but cannot read " + sessionXML);
			return PrearcStatus.ERROR;
		}

		return null;
	}
}
