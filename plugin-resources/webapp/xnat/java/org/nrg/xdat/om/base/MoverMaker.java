/*
 * org.nrg.xdat.om.base.MoverMaker
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xdat.om.base;

import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.om.*;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTItem;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.exception.*;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xft.utils.SaveItemHelper;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

public class MoverMaker {
	public static boolean check(ItemI i, XDATUser u)
			throws InvalidItemException, Exception {
		return u.canEdit(i);
	}

	public static void writeDB(MoveableI m, XnatProjectdata newProject,
			String newLabel, XDATUser u, EventMetaI c)
			throws InvalidItemException, XFTInitException,
			ElementNotFoundException, FieldNotFoundException,
			InvalidValueException, Exception {
		XFTItem item = XFTItem.NewItem(m.getXSIType(), u);
		MoveableI current = (MoveableI) BaseElement.GetGeneratedItem(item);
		current.setId(m.getId());
		current.setProject(newProject.getId());
		current.setLabel(newLabel);
		if (m instanceof XnatSubjectassessordata) {
			((XnatSubjectassessordata) current)
					.setSubjectId(((XnatSubjectassessordata) m).getSubjectId());
		} else if (m instanceof XnatImageassessordata) {
			((XnatImageassessordata) current)
					.setImagesessionId(((XnatImageassessordata) m)
							.getImagesessionId());
		}
		SaveItemHelper.authorizedSave(current.getItem(), u, false, false, c);
	}

	public static void setLocal(MoveableI m, XnatProjectdata newProject,
			String newLabel) {
		m.setProject(newProject.getId());
		m.setLabel(newLabel);
	}

	public static class Mover implements Callable<java.lang.Void> {
		final File newSessionDir;
		final String existingSessionDir;
		final String existingRootPath;
		final XDATUser u;
		XnatAbstractresource r = null;
		final EventMetaI c;

		public Mover(File newSessionDir, String existingSessionDir,
				String existingRootPath, XDATUser u, EventMetaI c) {
			this.newSessionDir = newSessionDir;
			this.existingSessionDir = existingSessionDir;
			this.existingRootPath = existingRootPath;
			this.u = u;
			this.c = c;
		}

		public void setResource(XnatAbstractresource r) {
			this.r = r;
		}

		@Override
		public Void call() throws Exception {
			r.moveTo(newSessionDir, existingSessionDir, existingRootPath, u, c);
			return null;
		}
	}

	public static Mover moveResource(XnatAbstractresourceI r,
			String current_label, MoveableI m, File newSessionDir,
			String existingRootPath, XDATUser u, EventMetaI c)
			throws IOException, Exception {
		String uri = null;
		if (r instanceof XnatResource) {
			uri = ((XnatResource) r).getUri();
		} else {
			uri = ((XnatResourceseries) r).getPath();
		}

		if (FileUtils.IsAbsolutePath(uri)) {
			int lastIndex = uri.lastIndexOf("/" + current_label + "/");
			if (lastIndex > -1) {
				lastIndex += 1 + current_label.length();
			}
			if (lastIndex == -1) {
				lastIndex = uri.lastIndexOf("/" + m.getId() + "/");
				if (lastIndex > -1) {
					lastIndex += 1 + m.getId().length();
				}
			}
			String existingSessionDir = null;
			if (lastIndex > -1) {
				// in session_dir
				existingSessionDir = uri.substring(0, lastIndex);
			} else {
				// outside session_dir
				// newSessionDir = new File(newSessionDir,subdirectoryName);
				// newSessionDir = new
				// File(newSessionDir,r.getXnatAbstractresourceId().toString());
				// int lastSlash=uri.lastIndexOf("/");
				// if(uri.lastIndexOf("\\")>lastSlash){
				// lastSlash=uri.lastIndexOf("\\");
				// }
				// existingSessionDir=uri.substring(0,lastSlash);
				// don't attempt to move sessions which are outside of the
				// Session Directory.
				throw new Exception("Non-standard file location for file(s):"
						+ uri);
			}
			return new Mover(newSessionDir, existingSessionDir,
					existingRootPath, u, c);
			// ((XnatAbstractresource)m).moveTo(newSessionDir,existingSessionDir,existingRootPath,u);
		} else {
			return new Mover(newSessionDir, null, existingRootPath, u, c);
			// ((XnatAbstractresource)m).moveTo(newSessionDir,null,existingRootPath,u);
		}
	}

	public static File createPrimaryBackupDirectory(String cacheBKDirName,
			String project, String folderName) {
		File f = org.nrg.xnat.utils.FileUtils.buildCachepath(project,
				cacheBKDirName, folderName);
		f.mkdirs();
		return f;
	}
}
