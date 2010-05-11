/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.xnat.archive;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.concurrent.Callable;

import org.nrg.StatusListener;
import org.nrg.StatusMessage;
import org.nrg.StatusMessage.Status;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xnat.turbine.modules.actions.LoadImageData;
import org.nrg.xnat.turbine.modules.actions.StoreImageSession;
import org.xml.sax.SAXException;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public final class PrearcSessionArchiver implements Callable<URL> {
	private final static String[] SCANS_DIR_NAMES = {"SCANS", "RAW"};
	
	private final Collection<StatusListener> listeners = new LinkedHashSet<StatusListener>();
	private final Object statusObj;
	private final XnatImagesessiondata session;
	private final XDATUser user;
	private final String project;
	
	public PrearcSessionArchiver(final XnatImagesessiondata session,
			final XDATUser user, final String project) {
		this.session = session;
		this.user = user;
		this.project = project;
		this.statusObj = session.getPrearchivePath();
	}
	
	public PrearcSessionArchiver(final File sessionDir,
			final XDATUser user, final String project)
	throws IOException,SAXException {
		this(loadSession(sessionDir, user, project), user, project);
	}
	
	private static XnatImagesessiondata loadSession(final File sessionDir,
			final XDATUser user, final String project)
	throws IOException,SAXException {
		final LoadImageData loader = new LoadImageData();
		final File sessionXML = new File(sessionDir.getPath() + ".xml");
		return loader.getSession(user, sessionXML, project, false);
	}
	
	
	public boolean addStatusListener(final StatusListener listener) {
		return listeners.add(listener);
	}
	
	public boolean removeStatusListener(final StatusListener listener) {
		return listeners.remove(listener);
	}
	
	public void clearStatusListeners() {
		listeners.clear();
	}
	
	private void report(final Status status, final String message) {
		for (final StatusListener listener : listeners) {
			listener.notify(new StatusMessage(statusObj, status, message));
		}
	}
	
	private void processing(final String message) {
		report(Status.PROCESSING, message);
	}
	
	private void warning(final String message) {
		report(Status.WARNING, message);
	}
	
	private void failed(final String message) {
		report(Status.FAILED, message);
	}
	
	private void completed(final String message) {
		report(Status.COMPLETED, message);
	}
	/*
	 * (non-Javadoc)
	 * @see java.util.concurrent.Callable#call()
	 */
	public URL call() throws Exception {
		/*
		 * Ensure that the subject label and ID are set (by setting them, if necessary)
		 */
		processing("looking for subject");
		XnatSubjectdata subject = session.getSubjectData();
		// TODO: check for REST-specified subject label
		final String subjectID = session.getSubjectId();
		if (null == subject && LoadImageData.hasValue(subjectID)) {
			final String cleaned = XnatSubjectdata.cleanValue(subjectID);
			if (!cleaned.equals(subjectID)) {
				session.setSubjectId(cleaned);
				subject = session.getSubjectData();
			}
		}

		if (null == subject) {
			subject = new XnatSubjectdata((UserI)user);
			subject.setProject(project);
			if (LoadImageData.hasValue(subjectID)) {
				subject.setLabel(XnatSubjectdata.cleanValue(subjectID));
			}
			try {
				subject.setId(XnatSubjectdata.CreateNewID());
				subject.save(user, false, false);
				processing("created new subject " + subjectID);

				session.setSubjectId(subject.getId());
			} catch (Exception e) {
				failed("unable to build new subject: " + e.getMessage());
				throw e;
			}
		} else {
			processing("matches existing subject " + subjectID);
		}

		/*
		 * Determine a session label
		 */
		// TODO: check for REST-specified session label
		if (!LoadImageData.hasValue(session.getLabel())) {
			if (LoadImageData.hasValue(session.getDcmpatientid())) {
				session.setLabel(session.getDcmpatientid());
			}
		}
		if (!LoadImageData.hasValue(session.getLabel())) {
			failed("unable to deduce session label");
			throw new IllegalArgumentException("unable to deduce session label");
		}

		/*
		 * Don't overwrite an existing session.
		 */
		final StoreImageSession store = new StoreImageSession();	// TODO: remove StoreImageSession; see below
		final String arcSessionPath;
		try {
			arcSessionPath = store.getArcSessionPath(session);
		} catch (Exception e) {
			failed("Unable to determine archive path for session");
			throw e;
		}
		final File archivedSessionDir = new File(arcSessionPath);
		if (archivedSessionDir.exists()) {
			for (final String scansDirName : SCANS_DIR_NAMES) {
				final File scansDir = new File(archivedSessionDir, scansDirName);
				if (scansDir.exists() && FileUtils.HasFiles(scansDir)) {
					failed("Project " + project + " already contains a session named " + session.getLabel());
					throw new IllegalStateException("Session " + session.getLabel() + " already existing in project " + project);
				}
			}
		}

		store.template = session;
		processing("archiving session");
		
		// TODO: StoreImageSession is really a Turbine thing. We need a common foundation
		// to do the transfer without requiring RunData and Context. Once we have that,
		// much of the above code can likely be removed. (See also
		// org.nrg.xnat.turbine.modules.actions.ImageUpload)
		throw new UnsupportedOperationException();
	}
	
	
	public void dispose() {
		listeners.clear();
	}
}
