/*
 * org.nrg.xnat.restlet.resources.ScanDIRResource
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/17/13 4:45 PM
 */
package org.nrg.xnat.restlet.resources;

import org.apache.commons.io.FileUtils;
import org.nrg.action.ActionException;
import org.nrg.dcm.DicomDir;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xnat.restlet.files.utils.RestFileUtils;
import org.nrg.xnat.restlet.representations.ZipRepresentation;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class ScanDIRResource extends ScanResource {
	final Logger logger = LoggerFactory.getLogger(ScanDIRResource.class);

	public ScanDIRResource(Context context, Request request, Response response) {
		super(context, request, response);

		this.getVariants().clear();
		this.getVariants().add(new Variant(MediaType.APPLICATION_ZIP));
	}

	@Override
	public boolean allowPut() {
		return false;
	}

	@Override
	public Representation getRepresentation(Variant variant) {
		final List<XnatImagescandata> scans;
		if (null != scan) {
			scans = Collections.singletonList(scan);
		} else if (null == this.session) {
			this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,
					"Unable to find the specified session.");
			return null;
		} else if (!Strings.isNullOrEmpty(scanID)) {
			scanID = URLDecoder.decode(scanID);
			scans = XnatImagescandata.getScansByIdORType(scanID, session, user,
					completeDocument);
			if (scans.isEmpty()) {
				this.getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND,
						"Unable to find the specified scan(s).");
				return null;
			}
		} else {
			// TODO: use all scans for the given session?
			this.getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST,
					"No scan specified");
			return null;
		}
		assert !scans.isEmpty();

		ZipRepresentation rep;
		try {
			// prepare Maps for use in cleaning file paths and relativing them.
			final Map<String, String> session_mapping = new Hashtable<String, String>();
			session_mapping.put(session.getId(),
					session.getArchiveDirectoryName());
			session_mapping.put(session.getArchiveDirectoryName(),
					session.getArchiveDirectoryName());

			final ArrayList<String> session_ids = new ArrayList<String>();
			session_ids.add(session.getArchiveDirectoryName());

			Map<String, String> valuesToReplace = RestFileUtils.getReMaps(
					scans, null);
			try {
				rep = new ZipRepresentation(MediaType.APPLICATION_ZIP,
						session_ids, identifyCompression(null));
			} catch (ActionException e) {
				logger.error("", e);
				this.setResponseStatus(e);
				return null;
			}

			// this is the expected path to the SESSION_DIR
			final String rootPath = session.getArchivePath();

			// create a directory in the temporary directory to hold our files
			File _tmp_working_dir = File.createTempFile("dicom_", "", new File(
					System.getProperty("java.io.tmpdir")));
			String name = _tmp_working_dir.getAbsolutePath();
			_tmp_working_dir.delete();
			final File tmp_working_dir = new File(name);

			tmp_working_dir.mkdirs();

			// make the DICOMDIR file inside the working temp directory
			final File dicomDIRFile = new File(tmp_working_dir, "DICOMDIR");
			dicomDIRFile.createNewFile();

			// populate the DICOMDIR
			final DicomDir dicomdir = new DicomDir(dicomDIRFile);
			try {
				dicomdir.create();
				// iterate through scans and only include DICOM files.
				for (final XnatImagescandata scan : scans) {
					for (final XnatAbstractresourceI res : scan.getFile()) {
						if (((XnatAbstractresource) res).getFormat() != null
								&& ((XnatAbstractresource) res).getFormat()
										.equals("DICOM")) {
							for (final File f : ((XnatAbstractresource) res)
									.getCorrespondingFiles(rootPath)) {
								final String uri = f.getAbsolutePath();
								final String relative = RestFileUtils
										.buildRelativePath(
												uri,
												session_mapping,
												valuesToReplace,
												res.getXnatAbstractresourceId(),
												res.getLabel());
								// create a matching directory structure in the
								// working temp directory
								File tmp_dicom_dir = new File(tmp_working_dir,
										relative).getParentFile();
								if (tmp_dicom_dir != null) {
									tmp_dicom_dir.mkdirs();
								}
								if (f != null && f.exists()) {
									rep.addEntry(relative, f);
									// copy file to the directory structure in
									// the working temp directory.
									FileUtils.copyFileToDirectory(f,
											tmp_dicom_dir);
									File tmp_dicom_file = new File(
											tmp_dicom_dir, f.getName());
									dicomdir.addFile(tmp_dicom_dir);
									// delete the file now to avoid buildup.
									tmp_dicom_file.delete();
								}
							}
						}
					}
				}

				rep.addEntry("DICOMDIR", dicomDIRFile);
				rep.deleteDirectoryAfterWrite(tmp_working_dir);
				this.setContentDisposition(rep.getDownloadName());
				return rep;
			} finally {
				dicomdir.close();
			}
		} catch (Throwable e) {
			logger.error("", e);
			this.getResponse().setStatus(Status.SERVER_ERROR_INTERNAL,
					e.getMessage());
			return null;
		}
	}
}
