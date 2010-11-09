/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.xnat.restlet.resources;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.SecurityManager;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.restlet.util.XNATRestConstants;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.DomRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public final class PrearcSessionListResource extends Resource {
	private static final String XSDATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	private static final String PROJECT_SECURITY_TASK = "xnat:mrSessionData/project";
	private static final String USER_ATTR = "user";
	private static final String PROJECT_ATTR = "PROJECT_ID";
	private final Logger logger = LoggerFactory.getLogger(PrearcSessionListResource.class);

	private final XDATUser user;
	private final String requestedProject;
	private final Reference prearcRef;

	/**
	 * @param context
	 * @param request
	 * @param response
	 */
	public PrearcSessionListResource(final Context context, final Request request,
			final Response response) {
		super(context, request, response);

		// User comes either from traditional session or via XnatSecureGuard
		user = (XDATUser) getRequest().getAttributes().get(USER_ATTR);

		// Project is explicit in the request
		requestedProject = (String)request.getAttributes().get(PROJECT_ATTR);

		prearcRef = request.getResourceRef();
		
		//		getVariants().add(new Variant(MediaType.APPLICATION_JSON));
		//		getVariants().add(new Variant(MediaType.TEXT_HTML));
		getVariants().add(new Variant(MediaType.TEXT_XML));
	}

	private class Session implements Comparable<Session> {
		private final String name;
		private final Date uploadDate;
		private final Date lastBuiltDate;
		private final File sessionXML;
		
		Session(final File sessdir) {
			name = sessdir.getName();
			
			sessionXML = new File(sessdir.getPath() + ".xml");
			if (sessionXML.exists()) {
				lastBuiltDate = new Date(sessionXML.lastModified());
			} else {
				lastBuiltDate = new Date();
			}
			
			final DateFormat format = new SimpleDateFormat(XNATRestConstants.PREARCHIVE_TIMESTAMP);
			Date t_uploadDate;
			try {
				t_uploadDate = format.parse(sessdir.getParentFile().getName());
			} catch (ParseException e) {
				logger.error("Unable to parse upload date from session parent "
						+ sessdir.getParentFile(), e);
				t_uploadDate = null;
			}
			uploadDate = t_uploadDate;
		}
		
		public Date getLastBuiltDate() { return lastBuiltDate; }
		
		public String getName() { return name; }
		
		public Date getUploadDate() { return uploadDate; }
				
		public Element createElement(final Document d, final String urlBase) {
			final DateFormat xsDateTime = new SimpleDateFormat(XSDATETIME_FORMAT);
			final Element se = d.createElement("session");
			se.setAttribute("name", getName());
			se.setAttribute("upload", xsDateTime.format(getUploadDate()));
			se.setAttribute("build", xsDateTime.format(getLastBuiltDate()));
			final StringBuilder url = new StringBuilder(urlBase);
			url.append("/sessions/");
			final DateFormat ts = new SimpleDateFormat(XNATRestConstants.PREARCHIVE_TIMESTAMP);
			url.append(ts.format(getUploadDate()));
			url.append("/").append(getName());
			se.appendChild(d.createTextNode(url.toString()));
			return se;
		}
		
		/*
		 * (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(final Session other) {
			return getLastBuiltDate().compareTo(other.getLastBuiltDate());
		}
	}
	
	/**
	 * Retrieves the File reference to the prearchive root directory
	 * for the named project.
	 * @param user
	 * @param project project abbreviation or alias
	 * @return prearchive root directory
	 * @throws ResourceException if the named project does not exist, or if
	 * the user does not have create permission for it, or if the prearchive
	 * directory does not exist.
	 */
	static File getPrearcDir(final XDATUser user, final String project)
	throws ResourceException {
		if (null == user) {
			throw new ResourceException(Status.CLIENT_ERROR_UNAUTHORIZED);
		}

		XnatProjectdata projectData = XnatProjectdata.getXnatProjectdatasById(project,
				user, false);
		if (null == projectData) {
			final List<XnatProjectdata> matches =
				XnatProjectdata.getXnatProjectdatasByField("xnat:projectData/aliases/alias/alias",
						project, user, false);
			if (!matches.isEmpty()) {
				projectData = matches.get(0);
			}
		}
		if (null == projectData) {
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND,
					"No project named " + project);
		}

		String prearcPath;
		try {
			if (user.canAction(PROJECT_SECURITY_TASK, projectData.getId(), SecurityManager.CREATE)) {
				prearcPath = projectData.getPrearchivePath();
			} else {
				throw new ResourceException(Status.CLIENT_ERROR_FORBIDDEN,
						"user " + user.getUsername()
						+ " does not have create permissions for project "
						+ projectData.getId());
			}
		} catch (Exception e) {
			LoggerFactory.getLogger(PrearcSessionListResource.class)
			.error("Unable to check security for " + user.getUsername()
					+ " on " + projectData.getId(), e);
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL,
					e.getMessage());
		}

		if (null == prearcPath) {
			final String message = "Unable to retrieve prearchive path for project "
				+ projectData.getId();
			LoggerFactory.getLogger(PrearcSessionListResource.class).error(message);
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, message);
		}
		
		final File prearc = new File(prearcPath);
		if (!prearc.isDirectory()) {
			final String message = "Prearchive directory is invalid for project "
				+ projectData.getId();
			LoggerFactory.getLogger(PrearcSessionListResource.class).error(message);
			throw new ResourceException(Status.SERVER_ERROR_INTERNAL, message);
		}
		
		return prearc;
	}
	
	
	private static final Pattern timestampPattern = Pattern.compile("[0-9]{8}_[0-9]{6}");

	private static final FileFilter isTimestampDirectory = new FileFilter() {
		public boolean accept(final File f) {
			return f.isDirectory() && timestampPattern.matcher(f.getName()).matches();
		}
	};

	private static final FileFilter isDirectory = new FileFilter() {
		public boolean accept(final File f) {
			return f.isDirectory();
		}
	};
	
	private SortedMap<Date,Collection<Session>> getPrearcSessions(final File prearcDir) {
		final SortedMap<Date,Collection<Session>> sessions = new TreeMap<Date,Collection<Session>>();
		for (final File tsdir : prearcDir.listFiles(isTimestampDirectory)) {
			for (final File sessdir : tsdir.listFiles(isDirectory)) {
				final Session session = new Session(sessdir);
				final Date builtDate = session.getLastBuiltDate();
				if (!sessions.containsKey(builtDate)) {
					sessions.put(builtDate, new ArrayList<Session>(1));
				}
				sessions.get(builtDate).add(session);
			}
		}
		return sessions;
	}

	/*
	 * (non-Javadoc)
	 * @see org.restlet.resource.Resource#represent(org.restlet.resource.Variant)
	 */
	@Override
	public Representation represent(final Variant variant) throws ResourceException {
		if (MediaType.TEXT_XML.equals(variant.getMediaType())) {
			try {
				final DomRepresentation r = new DomRepresentation(MediaType.TEXT_XML);
				final Document d = r.getDocument();
				final Element root = d.createElement("PrearcSessions");
				root.setAttribute("project", requestedProject);
				d.appendChild(root);
				for (final Collection<Session> ss :
					getPrearcSessions(getPrearcDir(user, requestedProject)).values()) {
					for (final Session s : ss) {
						root.appendChild(s.createElement(d, prearcRef.getBaseRef().toString()));
					}
				}
				return r;
			} catch (IOException e) {
				logger.error("Unable to build prearchive session list representation", e);
				return null;
			}
		} else {
			logger.error("Requested representation for unsupported variant " + variant);
			return null;
		}
	}
}
