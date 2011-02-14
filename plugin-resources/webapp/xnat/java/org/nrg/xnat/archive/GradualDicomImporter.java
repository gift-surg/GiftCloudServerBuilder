package org.nrg.xnat.archive;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.TransferSyntax;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;
import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.dcm.Decompress;
import org.nrg.dcm.DicomFileNamer;
import org.nrg.dcm.xnat.SOPHashDicomFileNamer;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.AbstractDicomObjectIdentifier;
import org.nrg.xnat.DicomObjectIdentifier;
import org.nrg.xnat.Files;
import org.nrg.xnat.Labels;
import org.nrg.xnat.helpers.prearchive.PrearcDatabase;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.nrg.xnat.helpers.prearchive.SessionData;
import org.nrg.xnat.helpers.prearchive.SessionException;
import org.nrg.xnat.helpers.uri.UriParserUtils;
import org.nrg.xnat.restlet.actions.importer.ImporterHandlerA;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.restlet.data.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Strings;

/**
 * @author Tim Olsen <olsent@mir.wustl.edu>
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public class GradualDicomImporter extends ImporterHandlerA {
    private static final String DEFAULT_TRANSFER_SYNTAX = TransferSyntax.ImplicitVRLittleEndian.uid();
    private static final DicomFileNamer namer = new SOPHashDicomFileNamer();

    private final Logger logger = LoggerFactory.getLogger(GradualDicomImporter.class);
    private final DicomObject o;
    private final String name;
    private final XDATUser user;
    final Map<String,Object> params;

    public GradualDicomImporter(Object listenerControl, XDATUser u,	FileWriterWrapperI fw, Map<String, Object> params)
    throws IOException,ClientException {
        super(listenerControl, u, fw, params);
        this.user = u;
        this.o = read(fw.getInputStream());
        this.params = params;
        this.name = null == fw.getName() ? namer.makeFileName(o) : fw.getName();
    }

    public GradualDicomImporter(Object listenerControl, XDATUser u, DicomObject o, Map<String,Object> params) {
        super(listenerControl, u, null, params);
        this.user = u;
        this.o = o;
        this.name = namer.makeFileName(o);
        this.params = params;
    }

    private static DicomObject read(final InputStream in) throws ClientException {
        final BufferedInputStream bis = new BufferedInputStream(in);
        IOException ioexception = null;
        try {
            final DicomInputStream dis = new DicomInputStream(bis);
            try {
                return dis.readDicomObject();
            } catch (IOException e) {
                throw ioexception = e;
            } finally {
                try {
                    dis.close();
                } catch (IOException e) {
                    throw ioexception = null == ioexception ? e : ioexception;
                }
            }
        } catch (IOException e) {
            if (null == ioexception) {
                ioexception = e;
            }
            throw new ClientException(Status.CLIENT_ERROR_BAD_REQUEST, "unable to parse DICOM object", ioexception);
        } finally {
            try {
                bis.close();
            } catch (IOException e) {
                if (null == ioexception) {
                    throw new ClientException(Status.CLIENT_ERROR_BAD_REQUEST, "unable to close DICOM object input", e);
                } // otherwise, we're already throwing a ClientException.
            }
        }
    }

    private static Logger slog() { return LoggerFactory.getLogger(GradualDicomImporter.class); }

    private static void write(final DicomObject fmi, final DicomObject dataset, final File f)
    throws IOException {
        IOException ioexception = null;
        final FileOutputStream fos = new FileOutputStream(f);
        try {
            final DicomOutputStream dos = new DicomOutputStream(new BufferedOutputStream(fos));
            try {
                final String tsuid = fmi.getString(Tag.TransferSyntaxUID, DEFAULT_TRANSFER_SYNTAX);
                try {
                    if (Decompress.needsDecompress(tsuid)) {
                        try {
                            final ByteArrayInputStream bis = new ByteArrayInputStream(Decompress.dicomObject2Bytes(dataset));
                            final DicomObject d = Decompress.decompress_image(bis, tsuid);
                            final String dtsdui = Decompress.getTsuid(d);
                            try {
                                fmi.putString(Tag.TransferSyntaxUID, VR.UI, dtsdui);
                                dos.writeFileMetaInformation(fmi);
                                dos.writeDataset(dataset, dtsdui);
                            } catch (Throwable t) {
                                if (t instanceof IOException) {
                                    ioexception = (IOException)t;
                                }
                                try {
                                    dos.close();
                                } catch (IOException e) {
                                    throw ioexception = null == ioexception ? e : ioexception;
                                }
                            }
                        } catch (Throwable t) {
                            slog().error("Decompression failed; storing in original format " + tsuid, t);
                            dos.writeFileMetaInformation(fmi);
                            dos.writeDataset(dataset, tsuid);
                        }
                    } else {
                        dos.writeFileMetaInformation(fmi);
                        dos.writeDataset(dataset, tsuid);
                    }
                } catch (NoClassDefFoundError t) {
                    slog().error("Unable to check compression status; storing in original format " + tsuid, t);
                    dos.writeFileMetaInformation(fmi);
                    dos.writeDataset(dataset, tsuid);
                }
            } catch (IOException e) {
                throw ioexception = null == ioexception ? e : ioexception;
            } finally {
                try {
                    dos.close();
                } catch (IOException e) {
                    throw null == ioexception ? e : ioexception;
                }
            }
        } catch (IOException e) {
            throw ioexception = e;
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                throw null == ioexception ? e : ioexception;
            }
        }
    }

    private boolean canCreateIn(final XnatProjectdata p) {
        try {
            return PrearcUtils.canModify(user, p.getId());
        } catch (Exception e) {
            logger.error("Unable to check permissions for " + user + " in " + p, e);
            return false;
        }
    }

    public XnatProjectdata getProject(final Object alias, final XnatProjectdata defaultProject) {
        if (null != alias) {
            final XnatProjectdata p = XnatProjectdata.getXnatProjectdatasById(alias, user, false);
            if (null != p && canCreateIn(p)) {
                return p;
            } else {
                for (final XnatProjectdata pa :
                    XnatProjectdata.getXnatProjectdatasByField("xnat:projectData/aliases/alias/alias",
                            alias, user, false)) {
                    if (canCreateIn(pa)) {
                        return pa;
                    }
                }
            }
        }
        return defaultProject;
    }

    private String buildURI(final XnatProjectdata project, final File timestampDir, final File sessionDir) {
        final StringBuilder sb = new StringBuilder("/prearchive/projects/");
        sb.append(null == project ? PrearcUtils.COMMON : project.getId());
        sb.append("/").append(timestampDir.getName());
        sb.append("/").append(sessionDir.getName());
        return sb.toString();
    }

    @Override
    public List<String> call() throws ClientException, ServerException {
        final DicomObjectIdentifier<XnatProjectdata> id =
            new AbstractDicomObjectIdentifier<XnatProjectdata>() {
            public XnatProjectdata getProject(final String id) {
                return GradualDicomImporter.this.getProject(id, null);
            }
        };
        final XnatProjectdata project = getProject(params.get(UriParserUtils.PROJECT_ID), id.getProject(o));
        final String studyInstanceUID = o.getString(Tag.StudyInstanceUID);
        logger.trace("Looking for study {} in project {}", studyInstanceUID, project);

        SessionData sess = null;
        try {
            for (final SessionData s : PrearcDatabase.getSessionByUID(studyInstanceUID)) {
                if (Objects.equal(null == project ? null : project.getId(), s.getProject())
                        || (null == project && PrearcUtils.COMMON.equals(s.getProject()))) {
                    logger.trace("{}/{} identified to session {}",
                            new Object[] {project, studyInstanceUID, s.getUrl()});
                    sess = s;
                    break;
                }
            }
        } catch (SQLException e) {
            logger.error("unable to retrieve session by study UID", e);
            throw new ServerException(Status.SERVER_ERROR_INTERNAL, e);
        } catch (SessionException e) {
            logger.error("unable to retrieve session by study UID", e);
            throw new ServerException(Status.SERVER_ERROR_INTERNAL, e);
        }

        final File root;
        if (null == project) {
            root = new File(ArcSpecManager.GetInstance().getGlobalPrearchivePath());
        } else {
            root = new File(project.getPrearchivePath());
        }
        final File tsdir, sessdir;
        final String uri;
        if (null == sess) {
            logger.debug("No session found for {}/{}; creating a new one", project, studyInstanceUID);
            tsdir = PrearcUtils.makeTimestampDir(root);
            final String session = id.getSessionLabel(o);
            sessdir = new File(tsdir, session);
            uri = buildURI(project, tsdir, sessdir);
            sess = new SessionData();
            sess.setFolderName(session);
            sess.setName(session);
            sess.setProject(null == project ? null : project.getId());
            sess.setScan_date(o.getDate(Tag.StudyDate));
            sess.setStatus(PrearcUtils.PrearcStatus.RECEIVING);
            sess.setTag(studyInstanceUID);
            sess.setTimestamp(tsdir.getName());
            sess.setUrl(uri);
            try {
                PrearcDatabase.addSession(sess);
            } catch (SQLException e) {
                throw new ServerException(Status.SERVER_ERROR_INTERNAL, e);
            } catch (SessionException e) {
                throw new ServerException(Status.SERVER_ERROR_INTERNAL, e);
            }
        } else {
            tsdir = new File(root, sess.getTimestamp());
            sessdir = new File(tsdir, sess.getFolderName());
            uri = sess.getUrl();
            try {
                PrearcDatabase.setStatus(uri, PrearcUtils.PrearcStatus.RECEIVING);
            } catch (SQLException e) {
                logger.error("unable to update prearchive session status to RECEIVING", e);
            } catch (SessionException e) {
                logger.error("unable to update prearchive session status to RECEIVING", e);
            }
        }

        // Build the scan label
        final String seriesNum = o.getString(Tag.SeriesNumber);
        final String seriesUID = o.getString(Tag.SeriesInstanceUID);
        final String scan;
        if (Files.isValidFilename(seriesNum)) {
            scan = seriesNum;
        } else if (!Strings.isNullOrEmpty(seriesUID)) {
            scan = Labels.toLabelChars(seriesUID);
        } else {
            scan = null;
        }

        final DicomObject fmi;
        if (o.contains(Tag.TransferSyntaxUID)) {
            fmi = o.fileMetaInfo();
        } else {
            final String sopClassUID = o.getString(Tag.SOPClassUID);
            final String sopInstanceUID = o.getString(Tag.SOPInstanceUID);
            final String transferSyntaxUID = o.getString(Tag.TransferSyntaxUID, DEFAULT_TRANSFER_SYNTAX);
            fmi = new BasicDicomObject();
            fmi.initFileMetaInformation(sopClassUID, sopInstanceUID, transferSyntaxUID);
        }

        final File f = Files.getImageFile(sessdir, scan, null == name ? namer.makeFileName(o) : name);
        f.getParentFile().mkdirs();
        try {
            write(fmi, o, f);
        } catch (IOException e) {
            throw new ServerException(Status.SERVER_ERROR_INSUFFICIENT_STORAGE, e);
        }

        logger.trace("Stored object {}/{}/{} as {}",
                new Object[]{project, studyInstanceUID, o.getString(Tag.SOPInstanceUID), uri});
        return Collections.singletonList(uri.toString());
    }
}