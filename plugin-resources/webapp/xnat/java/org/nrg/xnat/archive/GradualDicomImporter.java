/*
 * Copyright (c) 2011 Washington University
 */
package org.nrg.xnat.archive;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.TransferSyntax;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;
import org.dcm4che2.io.StopTagInputHandler;
import org.dcm4che2.util.TagUtils;
import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.dcm.Decompress;
import org.nrg.dcm.DicomFileNamer;
import org.nrg.dcm.Extractor;
import org.nrg.dcm.MatchedPatternExtractor;
import org.nrg.dcm.xnat.SOPHashDicomFileNamer;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.XFT;
import org.nrg.xnat.AbstractDicomObjectIdentifier;
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
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;

/**
 * @author Tim Olsen <olsent@mir.wustl.edu>
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public class GradualDicomImporter extends ImporterHandlerA {
    public static final String SENDER_AE_TITLE_PARAM = "Sender-AE-Title";
    public static final String SENDER_ID_PARAM = "Sender-ID";
    public static final String TSUID_PARAM = "Transfer-Syntax-UID";
    private static final String DEFAULT_TRANSFER_SYNTAX = TransferSyntax.ImplicitVRLittleEndian.uid();
    private static final String DICOM_IMPORTER_PROPS = "dicom-importer.properties";
    private static final String DICOM_PROJECT_RULES = "dicom-project.rules";
    private static final String NAMER_PROPERTY = "dicom.file.namer";
    private static final String NAMER_DEFAULT = SOPHashDicomFileNamer.class.getName();
    private static final String RENAME_PARAM = "rename";
    private static final long PROJECT_CACHE_EXPIRY_SECONDS = 120;
    private static final boolean canDecompress = Decompress.isSupported();
    private static final DicomFileNamer namer = getDicomFileNamer();

    private final Logger logger = LoggerFactory.getLogger(GradualDicomImporter.class);
    private final FileWriterWrapperI fw;
    private final XDATUser user;
    private final Map<String,Object> params;
    private TransferSyntax ts = null;
    private Cache projectCache = null;
    private final RuleBasedIdentifier projectIdentifier = new RuleBasedIdentifier(this);

    public GradualDicomImporter(Object listenerControl, XDATUser u,	FileWriterWrapperI fw, Map<String,Object> params)
    throws IOException,ClientException {
        super(listenerControl, u, fw, params);
        this.user = u;
        this.fw = fw;
        this.params = params;
        if (params.containsKey(TSUID_PARAM)) {
            ts = TransferSyntax.valueOf((String)params.get(TSUID_PARAM));
        }
    }

    private static final Pattern CUSTOM_RULE_PATTERN = Pattern.compile("\\((\\p{XDigit}{4})\\,(\\p{XDigit}{4})\\):(.+?)(?::(\\d+))?");

    private static DicomObject read(final InputStream in, final String name) throws ClientException {
        final BufferedInputStream bis = new BufferedInputStream(in);
        IOException ioexception = null;
        try {
            final DicomInputStream dis = new DicomInputStream(bis);
            try {
                final DicomObject o = dis.readDicomObject();
                if (Strings.isNullOrEmpty(o.getString(Tag.SOPClassUID))) {
                    throw new ClientException("object " + name + " contains no SOP Class UID");
                }
                if (Strings.isNullOrEmpty(o.getString(Tag.SOPInstanceUID))) {
                    throw new ClientException("object " + name + " contains no SOP Instance UID");
                }
                return o;
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

    private static void write(final DicomObject fmi, final DicomObject dataset,
            BufferedInputStream remainder, final File f, final String source)
    throws ClientException,IOException {
        IOException ioexception = null;
        final FileOutputStream fos = new FileOutputStream(f);
        final BufferedOutputStream bos = new BufferedOutputStream(fos);
        try {
            final DicomOutputStream dos = new DicomOutputStream(bos);
            try {
                final String tsuid = fmi.getString(Tag.TransferSyntaxUID, DEFAULT_TRANSFER_SYNTAX);
                try {
                    if (Decompress.needsDecompress(tsuid) && canDecompress) {
                        try {
                            // Read the rest of the object into memory so the pixel data can be decompressed.
                            final DicomInputStream dis = new DicomInputStream(remainder, tsuid);
                            try {
                                dis.readDicomObject(dataset, -1);
                            } catch (IOException e) {
                                ioexception = e;
                                throw new ClientException(Status.CLIENT_ERROR_BAD_REQUEST,
                                        "error parsing DICOM object", e);
                            }
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
                                } else {
                                    slog().error("Unable to write decompressed dataset", t);
                                }
                                try {
                                    dos.close();
                                } catch (IOException e) {
                                    throw ioexception = null == ioexception ? e : ioexception;
                                }
                            }
                        } catch (ClientException e) {
                            throw e;
                        } catch (Throwable t) {
                            slog().error("Decompression failed; storing in original format " + tsuid, t);
                            dos.writeFileMetaInformation(fmi);
                            dos.writeDataset(dataset, tsuid);
                            if (null != remainder) {
                                final long copied = ByteStreams.copy(remainder, bos);
                                slog().trace("copied {} additional bytes to {}", copied, f);
                            }
                        }
                    } else {
                        dos.writeFileMetaInformation(fmi);
                        dos.writeDataset(dataset, tsuid);
                        if (null != remainder) {
                            final long copied = ByteStreams.copy(remainder, bos);
                            slog().trace("copied {} additional bytes to {}", copied, f);
                        }
                    }
                } catch (NoClassDefFoundError t) {
                    slog().error("Unable to check compression status; storing in original format " + tsuid, t);
                    dos.writeFileMetaInformation(fmi);
                    dos.writeDataset(dataset, tsuid);
                    if (null != remainder) {
                        final long copied = ByteStreams.copy(remainder, bos);
                        slog().trace("copied {} additional bytes to {}", copied, f);
                    }
                }
            } catch (IOException e) {
                throw ioexception = null == ioexception ? e : ioexception;
            } finally {
                try {
                    dos.close();
                    LoggerFactory.getLogger("org.nrg.xnat.received").info("{}:{}", source, f);
                } catch (IOException e) {
                    throw null == ioexception ? e : ioexception;
                }
            }
        } catch (IOException e) {
            throw ioexception = e;
        } finally {
            try {
                bos.close();
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

    public XnatProjectdata getProject(final Object alias, final Callable<XnatProjectdata> defaultProject) {
        if (null != alias) {
            if (null == projectCache) {
                setCacheManager(CacheManager.getInstance());
            }
            final Element pe = projectCache.get(alias);
            if (null != pe) {
                return (XnatProjectdata)pe.getValue();
            } else {
                logger.debug("cache miss for project alias {}, trying database", alias);
                final XnatProjectdata p = XnatProjectdata.getXnatProjectdatasById(alias, user, false);
                if (null != p && canCreateIn(p)) {
                    projectCache.put(new Element(alias, p));
                    return p;
                } else {
                    for (final XnatProjectdata pa :
                        XnatProjectdata.getXnatProjectdatasByField("xnat:projectData/aliases/alias/alias",
                                alias, user, false)) {
                        if (canCreateIn(pa)) {
                            projectCache.put(new Element(alias, pa));
                            return pa;
                        }
                    }
                }
            }
        }
        // Couldn't find anything. Use the default project.
        try {
            final XnatProjectdata dp = null == defaultProject ? null : defaultProject.call();
            if (null != alias) {
                projectCache.put(new Element(alias, dp));
            }
            return dp;
        } catch (Throwable t) {
            logger.error("error in default project provider", t);
            return null;
        }
    }

    private File getSafeFile(File sessionDir, String scan, String name, DicomObject o, boolean forceRename) {
        final File safeFile = Files.getImageFile(sessionDir, scan, namer.makeFileName(o));
        if (forceRename) {
            return safeFile;
        }
        final String valname = Files.toFileNameChars(name);
        if (!Files.isValidFilename(valname)) {
            return safeFile;
        }
        final File reqFile = Files.getImageFile(sessionDir, scan, valname);
        if (reqFile.exists()) {
            try {
                Exception exception = null;
                final FileInputStream fin = new FileInputStream(reqFile);
                try {
                    final DicomObject o1 = read(fin, name);
                    if (Objects.equal(o.get(Tag.SOPInstanceUID), o1.get(Tag.SOPInstanceUID)) &&
                            Objects.equal(o.get(Tag.SOPClassUID), o1.get(Tag.SOPClassUID))) {
                        return reqFile;  // object are equivalent; ok to overwrite
                    } else {
                        return safeFile;
                    }
                } catch (ClientException e) {
                    throw exception = e;
                } finally {
                    try {
                        fin.close();
                    } catch (IOException e) {
                        throw null == exception ? e : exception;
                    }
                }
            } catch (Throwable t) {
                return safeFile;                    
            }
        } else {
            return reqFile;
        }
    }

    private static <K,V> String getString(final Map<K,V> m, final K k, final V defaultValue) {
        final V v = m.get(k);
        if (null == v) {
            return null == defaultValue ? null : defaultValue.toString();
        } else {
            return v.toString();
        }
    }

    @Override
    public List<String> call() throws ClientException, ServerException {
        final BufferedInputStream bis;
        final DicomInputStream dis;
        final String name = fw.getName();
        final DicomObject o;
        try {
            bis = new BufferedInputStream(fw.getInputStream());
            try {
                dis = null == ts ? new DicomInputStream(bis) : new DicomInputStream(bis, ts);
                final int lastTag = projectIdentifier.getTags().last() + 1;
                logger.trace("reading object into memory up to {}", TagUtils.toString(lastTag));
                dis.setHandler(new StopTagInputHandler(lastTag));
                o = dis.readDicomObject();
                try {
                    bis.reset();
                } catch (IOException e) {
                    logger.error("unable to reset DICOM data stream", e);
                }
                if (Strings.isNullOrEmpty(o.getString(Tag.SOPClassUID))) {
                    throw new ClientException("object " + name + " contains no SOP Class UID");
                }
                if (Strings.isNullOrEmpty(o.getString(Tag.SOPInstanceUID))) {
                    throw new ClientException("object " + name + " contains no SOP Instance UID");
                }
            } catch (Throwable t) {
                try {
                    bis.close();
                } catch (IOException e1) {
                    logger.error("unable to close input stream", t);
                }
                throw new ClientException(Status.CLIENT_ERROR_BAD_REQUEST, "unable to read DICOM object " + name, t);
            }
        } catch (ClientException e) {
            throw e;
        } catch (Throwable t) {
            throw new ClientException(Status.CLIENT_ERROR_BAD_REQUEST, "unable to read DICOM object " + name, t);
        }

        final XnatProjectdata project;
        try {
            // project identifier is expensive, so avoid if possible
            project = getProject(PrearcUtils.identifyProject(params),
                    new Callable<XnatProjectdata>() {
                public XnatProjectdata call() {
                    return projectIdentifier.getProject(o);
                }
            });
        } catch (MalformedURLException e1) {
            logger.error("unable to parse supplied destination flag", e1);
            throw new ClientException(Status.CLIENT_ERROR_BAD_REQUEST, e1);
        }
        final String studyInstanceUID = o.getString(Tag.StudyInstanceUID);
        logger.trace("Looking for study {} in project {}", studyInstanceUID,
                null == project ? null : project.getId());

        // Fill a SessionData object in case it is the first upload
        SessionData sess = null;
        final File root;
        final String project_id;
        if (null == project) {
            root = new File(ArcSpecManager.GetInstance().getGlobalPrearchivePath());
            project_id=null;
        } else {
            root = new File(project.getPrearchivePath());
            project_id = project.getId();
        }
        final File tsdir, sessdir;

        tsdir = new File(root, PrearcUtils.makeTimestamp());
        final String session;
        if (params.containsKey(UriParserUtils.EXPT_LABEL)) {
            session = (String)params.get(UriParserUtils.EXPT_LABEL);
            logger.trace("using provided experiment label {}", params.get(UriParserUtils.EXPT_LABEL));
        } else {
            session = projectIdentifier.getSessionLabel(o);
        }
        sess = new SessionData();
        sess.setFolderName(session);
        sess.setName(session);
        sess.setProject(project_id);
        sess.setScan_date(o.getDate(Tag.StudyDate));
        sess.setTag(studyInstanceUID);
        sess.setTimestamp(tsdir.getName());
        sess.setStatus(PrearcUtils.PrearcStatus.RECEIVING);
        sess.setLastBuiltDate(Calendar.getInstance().getTime());

        sess.setUrl((new File(tsdir,session)).getAbsolutePath());

        // query the cache for an existing session that has this Study Instance UID and project name,
        // if found the SessionData object we just created is over-ridden with the values from the cache
        try {
            sess = PrearcDatabase.getOrCreateSession(sess.getProject(), sess.getTag(), sess, tsdir, shouldAutoArchive(o));
            PrearcDatabase.setLastModifiedTime(sess.getName(), sess.getTimestamp(), sess.getProject());        
        } catch (SQLException e) {
            throw new ServerException(Status.SERVER_ERROR_INTERNAL, e);
        } catch (SessionException e) {
            throw new ServerException(Status.SERVER_ERROR_INTERNAL, e);
        } catch (Exception e) {
            throw new ServerException(Status.SERVER_ERROR_INTERNAL, e);
        }

        sessdir = new File(new File(root, sess.getTimestamp()),sess.getFolderName());

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

        final String source = getString(params, SENDER_ID_PARAM, user.getLogin());

        try {
            final DicomObject fmi;
            if (o.contains(Tag.TransferSyntaxUID)) {
                fmi = o.fileMetaInfo();
            } else {
                final String sopClassUID = o.getString(Tag.SOPClassUID);
                final String sopInstanceUID = o.getString(Tag.SOPInstanceUID);
                final String transferSyntaxUID;
                if (null == ts) {
                    transferSyntaxUID = o.getString(Tag.TransferSyntaxUID, DEFAULT_TRANSFER_SYNTAX);
                } else {
                    transferSyntaxUID = ts.uid();
                }
                fmi = new BasicDicomObject();
                fmi.initFileMetaInformation(sopClassUID, sopInstanceUID, transferSyntaxUID);
                if (params.containsKey(SENDER_AE_TITLE_PARAM)) {
                    fmi.putString(Tag.SourceApplicationEntityTitle, VR.AE, (String)params.get(SENDER_AE_TITLE_PARAM));
                }
            }

            final File f = getSafeFile(sessdir, scan, name, o, Boolean.valueOf((String)params.get(RENAME_PARAM)));
            f.getParentFile().mkdirs();
            try {
                write(fmi, o, bis, f, source);
            } catch (IOException e) {
                throw new ServerException(Status.SERVER_ERROR_INSUFFICIENT_STORAGE, e);
            }
        } finally {
            if (null != dis) try {
                dis.close();
            } catch (IOException e) {
                logger.error("closing DicomInputStream failed", e);
            }
        }

        logger.trace("Stored object {}/{}/{} as {} for {}",
                new Object[]{project, studyInstanceUID, o.getString(Tag.SOPInstanceUID), sess.getUrl(), source});
        return Collections.singletonList(sess.getExternalUrl());
    }

    public void setCacheManager(final CacheManager cacheManager) {
        final String cacheName = user.getLogin() + "-projects";
        synchronized (cacheManager) {
            if (!cacheManager.cacheExists(cacheName)) {
                final CacheConfiguration config = new CacheConfiguration(cacheName, 0)
                .copyOnRead(false).copyOnWrite(false)
                .eternal(false)
                .overflowToDisk(false)
                .timeToLiveSeconds(PROJECT_CACHE_EXPIRY_SECONDS);
                final Cache cache = new Cache(config);
                cacheManager.addCache(cache);
                projectCache = cache;
            } else {
                projectCache = cacheManager.getCache(cacheName);
            }
        }
    }

    private static final Pattern aaPattern = Pattern.compile("\\A(?:.*\\W)?AA:([a-zA-Z]+)(?:\\W.*)?\\Z");

    /**
     * Looks for AA:true|false in the given DICOM object. The AA: portion is case-sensitive,
     * but the true/false is case-insensitive. Patient Comments is searched first, then
     * Study Comments.
     * @param o
     * @return true if AA:true is found, false if AA:false is found, null otherwise.
     */
    private static Boolean shouldAutoArchive(final DicomObject o) {
        for (final int tag : new int[]{Tag.PatientComments, Tag.StudyComments}) {
            final String s = o.getString(tag);
            if (null != s) {
                final Matcher m = aaPattern.matcher(s);
                if (m.matches()) {
                    final String arg = m.group(1);
                    if ("true".equalsIgnoreCase(arg)) {
                        return true;
                    } else if ("false".equalsIgnoreCase(arg)) {
                        return false;
                    }
                }
            }
        }
        return null;
    }

    private static final class RuleBasedIdentifier
    extends AbstractDicomObjectIdentifier<XnatProjectdata> {
        private static final Extractor[] exts;
        static {
            final List<Extractor> es = Lists.newArrayList();
            final File config = new File(XFT.GetConfDir(), DICOM_PROJECT_RULES);
            IOException ioexception = null;
            if (config.isFile()) {
                try {
                    final BufferedReader reader = new BufferedReader(new FileReader(config));
                    try {
                        String line;
                        while (null != (line = reader.readLine())) {
                            final Extractor extractor = parseRule(line);
                            if (null != extractor) {
                                es.add(extractor);
                            }
                        }
                    } catch (IOException e) {
                        throw ioexception = e;
                    } finally {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            throw ioexception = null == ioexception ? e : ioexception;
                        }
                    }
                } catch (Throwable t) {
                    slog().error("Unable to load project identification rules from " + DICOM_PROJECT_RULES, t);
                }
            } else {
                slog().debug("custom project rules spec {} not found", DICOM_PROJECT_RULES);                
            }
            exts = es.toArray(new Extractor[0]);
        }

        private final GradualDicomImporter importer;

        RuleBasedIdentifier(final GradualDicomImporter importer) {
            this.importer = importer;
            addProjectExtractors(exts);
        }

        private static final Extractor parseRule(final String rule) {
            final Matcher matcher = CUSTOM_RULE_PATTERN.matcher(rule);
            if (matcher.matches()) {
                final StringBuilder tagsb = new StringBuilder("0x");
                tagsb.append(matcher.group(1)).append(matcher.group(2));
                final int tag = Integer.decode(tagsb.toString());
                final String regexp = matcher.group(3);
                final String groupIdx = matcher.group(4);
                final int group = null == groupIdx ? 1 : Integer.parseInt(groupIdx);
                return new MatchedPatternExtractor(tag, Pattern.compile(regexp), group);
            } else {
                return null;
            }
        }

        public XnatProjectdata getProject(final String id) {
            return importer.getProject(id, null);
        }
    }

    private static DicomFileNamer getDicomFileNamer() {
        try {
            final Properties properties = getProperties();
            final String namerClass = properties.getProperty(NAMER_PROPERTY, NAMER_DEFAULT);
            return Class.forName(namerClass).asSubclass(DicomFileNamer.class).newInstance();
        } catch (Throwable t) {
            slog().warn("unable to load custom DICOM file namer", t);
            return new SOPHashDicomFileNamer();
        }
    }

    private static Properties getProperties() {
        final File propsfile = new File(XFT.GetConfDir(), DICOM_IMPORTER_PROPS);
        final Properties properties = new Properties();
        try {
            properties.load(new FileReader(propsfile));
        } catch (IOException e) {
            slog().debug("no DICOM SCP properties file " + propsfile + " found", e);
        }
        return properties;
    }
}
