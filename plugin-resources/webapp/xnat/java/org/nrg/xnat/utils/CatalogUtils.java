/**
 * Copyright 2010 Washington University
 */
package org.nrg.xnat.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipOutputStream;

import com.twmacinta.util.MD5;
import org.apache.log4j.Logger;
import org.nrg.config.entities.Configuration;
import org.nrg.config.exceptions.ConfigServiceException;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.bean.CatCatalogBean;
import org.nrg.xdat.bean.CatCatalogMetafieldBean;
import org.nrg.xdat.bean.CatEntryBean;
import org.nrg.xdat.bean.CatEntryMetafieldBean;
import org.nrg.xdat.bean.CatEntryTagBean;
import org.nrg.xdat.bean.base.BaseElement;
import org.nrg.xdat.bean.reader.XDATXMLReader;
import org.nrg.xdat.model.CatCatalogI;
import org.nrg.xdat.model.CatCatalogMetafieldI;
import org.nrg.xdat.model.CatDcmentryI;
import org.nrg.xdat.model.CatEntryI;
import org.nrg.xdat.model.CatEntryMetafieldI;
import org.nrg.xdat.model.CatEntryTagI;
import org.nrg.xdat.model.XnatResourcecatalogI;
import org.nrg.xdat.om.XnatAbstractresource;
import org.nrg.xdat.om.XnatAbstractresourceTag;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatResource;
import org.nrg.xdat.om.XnatResourcecatalog;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xft.utils.zip.TarUtils;
import org.nrg.xft.utils.zip.ZipI;
import org.nrg.xft.utils.zip.ZipUtils;
import org.nrg.xnat.helpers.resource.XnatResourceInfo;
import org.nrg.xnat.presentation.ChangeSummaryBuilderA;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.xml.sax.SAXException;

/**
 * @author timo
 */
@SuppressWarnings("deprecation")
public class CatalogUtils {

    public static boolean DEFAULT_CHECKSUM = true;
    public final static String[] FILE_HEADERS = {"Name", "Size", "URI", "collection", "file_tags", "file_format", "file_content", "cat_ID"};
    public final static String[] FILE_HEADERS_W_FILE = {"Name", "Size", "URI", "collection", "file_tags", "file_format", "file_content", "cat_ID", "file"};

    public static boolean getChecksumConfiguration(final XnatProjectdata project) throws ConfigServiceException {
        Callable<Long> getProjectId = new Callable<Long>() {
            public Long call() {
                return (long) (Integer) project.getItem().getProps().get("projectdata_info");
            }
        };

        Configuration configuration = XDAT.getConfigService().getConfig("checksums", "checksums", getProjectId);

        if (configuration != null) {
            String checksumProperty = XDAT.getSiteConfigurationProperty("checksums");
            if (!org.apache.commons.lang.StringUtils.isBlank(checksumProperty)) {
                return Boolean.parseBoolean(checksumProperty);
            }
        }

        return getChecksumConfiguration();
    }

    public static boolean getChecksumConfiguration() throws ConfigServiceException {
        String checksumProperty = XDAT.getSiteConfigurationProperty("checksums");
        if (!org.apache.commons.lang.StringUtils.isBlank(checksumProperty)) {
            return Boolean.parseBoolean(checksumProperty);
        }
        return DEFAULT_CHECKSUM;
    }

    public static void calculateResourceChecksums(final CatCatalogI cat, final File f) {
        for (CatEntryI entry : cat.getEntries_entry()) {
            File file = new File(f.getParent(), entry.getUri());
            try {
                String checksum = MD5.asHex(MD5.getHash(file));
                entry.setDigest(checksum);
            } catch (IOException e) {
                //
            }
        }
    }

    public static List<Object[]> getEntryDetails(CatCatalogI cat, String parentPath, String uriPath, XnatResource _resource, boolean includeFile, final CatEntryFilterI filter, XnatProjectdata proj, String locator) {
        final ArrayList<Object[]> al = new ArrayList<Object[]>();
        for (final CatCatalogI subset : cat.getSets_entryset()) {
            al.addAll(getEntryDetails(subset, parentPath, uriPath, _resource, includeFile, filter, proj, locator));
        }

        final int ri = (includeFile) ? 9 : 8;

        //final int ri=(includeFile)?14:13;

        for (final CatEntryI entry : cat.getEntries_entry()) {
            if (filter == null || filter.accept(entry)) {
                final Object[] row = new Object[ri];
                final String entryPath = StringUtils.ReplaceStr(FileUtils.AppendRootPath(parentPath, entry.getUri()), "\\", "/");
                final File f = getFileOnLocalFileSystem(entryPath);
                row[0] = (f.getName());
                if (includeFile) {
                    row[1] = 0;
                } else {
                    row[1] = (f.length());
                }
                if (locator.equalsIgnoreCase("URI")) {
                    if (FileUtils.IsAbsolutePath(entry.getUri())) {
                        row[2] = uriPath + "/" + entry.getId();
                    } else {
                        row[2] = uriPath + "/" + entry.getUri();
                    }
                } else if (locator.equalsIgnoreCase("absolutePath")) {
                    row[2] = entryPath;
                } else if (locator.equalsIgnoreCase("projectPath")) {
                    row[2] = entryPath.substring(proj.getRootArchivePath().substring(0, proj.getRootArchivePath().lastIndexOf(proj.getId())).length());
                }
                row[3] = _resource.getLabel();
                row[4] = "";
                for (CatEntryMetafieldI meta : entry.getMetafields_metafield()) {
                    if (!row[4].equals("")) row[4] = row[4] + ",";
                    row[4] = row[4] + meta.getName() + "=" + meta.getMetafield();
                }
                for (CatEntryTagI tag : entry.getTags_tag()) {
                    if (!row[4].equals("")) row[4] = row[4] + ",";
                    row[4] = row[4] + tag.getTag();
                }
                row[5] = entry.getFormat();
                row[6] = entry.getContent();
                row[7] = _resource.getXnatAbstractresourceId();
                if (includeFile) row[8] = f;
                al.add(row);
            }
        }

        return al;
    }

    public interface CatEntryFilterI {
        public boolean accept(final CatEntryI entry);
    }

    public static CatEntryI getEntryByFilter(final CatCatalogI cat, final CatEntryFilterI filter) {
        CatEntryI e;
        for (CatCatalogI subset : cat.getSets_entryset()) {
            e = getEntryByFilter(subset, filter);
            if (e != null) return e;
        }

        for (CatEntryI entry : cat.getEntries_entry()) {
            try {
                if (filter.accept(entry)) {
                    return entry;
                }
            } catch (Exception e1) {
                logger.error(e1);
            }
        }

        return null;
    }

    public static Collection<CatEntryI> getEntriesByFilter(final CatCatalogI cat, final CatEntryFilterI filter) {
        List<CatEntryI> entries = new ArrayList<CatEntryI>();

        for (CatCatalogI subset : cat.getSets_entryset()) {
            entries.addAll(getEntriesByFilter(subset, filter));
        }

        for (CatEntryI entry : cat.getEntries_entry()) {
            try {
                if (filter == null || filter.accept(entry)) {
                    entries.add(entry);

                }
            } catch (Exception e1) {
                logger.error(e1);
            }
        }

        return entries;
    }

    public static CatCatalogI getCatalogByFilter(final CatCatalogI cat) {
        CatCatalogI e;
        for (CatCatalogI subset : cat.getSets_entryset()) {
            e = getCatalogByFilter(subset);
            if (e != null) return e;
        }

        return null;
    }

    public static List<File> getFiles(CatCatalogI cat, String parentPath) {
        List<File> al = new ArrayList<File>();
        for (CatCatalogI subset : cat.getSets_entryset()) {
            al.addAll(getFiles(subset, parentPath));
        }

        for (CatEntryI entry : cat.getEntries_entry()) {
            String entryPath = StringUtils.ReplaceStr(FileUtils.AppendRootPath(parentPath, entry.getUri()), "\\", "/");
            File f = getFileOnLocalFileSystem(entryPath);

            if (f != null)
                al.add(f);
        }

        return al;
    }

    public static File getFile(CatEntryI entry, String parentPath) {
        String entryPath = StringUtils.ReplaceStr(FileUtils.AppendRootPath(parentPath, entry.getUri()), "\\", "/");
        return getFileOnLocalFileSystem(entryPath);
    }

    public static Stats getFileStats(CatCatalogI cat, String parentPath) {
        return new Stats(cat, parentPath);
    }

    public static class Stats {
        public int count;
        public long size;

        public Stats(CatCatalogI cat, String parentPath) {
            count = 0;
            size = 0;
            for (final File f : getFiles(cat, parentPath)) {
                if (f != null && f.exists() && !f.getName().endsWith("catalog.xml")) {
                    count++;
                    size += f.length();
                }
            }
        }
    }

    public static Collection<CatEntryI> getEntriesByRegex(final CatCatalogI cat, String regex) {
        List<CatEntryI> entries = new ArrayList<CatEntryI>();
        for (CatCatalogI subset : cat.getSets_entryset()) {
            entries.addAll(getEntriesByRegex(subset, regex));
        }
        for (CatEntryI entry : cat.getEntries_entry()) {
            try {
                if (entry.getUri().matches(regex)) {
                    entries.add(entry);
                }
            } catch (Exception e1) {
                logger.error(e1);
            }
        }
        return entries;
    }

    public static CatEntryI getEntryByURI(CatCatalogI cat, String name) {
        CatEntryI e;
        for (CatCatalogI subset : cat.getSets_entryset()) {
            e = getEntryByURI(subset, name);
            if (e != null) return e;
        }

        for (CatEntryI entry : cat.getEntries_entry()) {
            try {
                String decoded = URLDecoder.decode(name);
                if (entry.getUri().equals(name) || entry.getUri().equals(decoded)) {
                    return entry;
                }
            } catch (Exception e1) {
                logger.error(e1);
            }
        }

        return null;
    }

    public static CatEntryI getEntryByName(CatCatalogI cat, String name) {
        CatEntryI e;
        for (CatCatalogI subset : cat.getSets_entryset()) {
            e = getEntryByName(subset, name);
            if (e != null) return e;
        }

        for (CatEntryI entry : cat.getEntries_entry()) {
            String decoded = URLDecoder.decode(name);
            if (entry.getName().equals(name) || entry.getName().equals(decoded)) {
                return entry;
            }
        }

        return null;
    }

    public static CatEntryI getEntryById(CatCatalogI cat, String name) {
        CatEntryI e;
        for (CatCatalogI subset : cat.getSets_entryset()) {
            e = getEntryById(subset, name);
            if (e != null) return e;
        }

        for (CatEntryI entry : cat.getEntries_entry()) {
            if (entry.getId().equals(name)) {
                return entry;
            }
        }

        return null;
    }

    public static CatDcmentryI getDCMEntryByUID(CatCatalogI cat, String uid) {
        CatDcmentryI e;
        for (CatCatalogI subset : cat.getSets_entryset()) {
            e = getDCMEntryByUID(subset, uid);
            if (e != null) return e;
        }

        for (CatEntryI entry : cat.getEntries_entry()) {
            if (entry instanceof CatDcmentryI && ((CatDcmentryI) entry).getUid().equals(uid)) {
                return (CatDcmentryI) entry;
            }
        }

        return null;
    }

    public static CatDcmentryI getDCMEntryByInstanceNumber(CatCatalogI cat, Integer num) {
        CatDcmentryI e;
        for (CatCatalogI subset : cat.getSets_entryset()) {
            e = getDCMEntryByInstanceNumber(subset, num);
            if (e != null) return e;
        }

        for (CatEntryI entry : cat.getEntries_entry()) {
            if (entry instanceof CatDcmentryI && ((CatDcmentryI) entry).getInstancenumber().equals(num)) {
                return (CatDcmentryI) entry;
            }
        }

        return null;
    }

    public static File getFileOnLocalFileSystem(String fullPath) {
        File f = new File(fullPath);
        if (!f.exists()) {
            if (!fullPath.endsWith(".gz")) {
                f = new File(fullPath + ".gz");
                if (!f.exists()) {
                    return null;
                }
            } else {
                return null;
            }
        }

        return f;
    }

    public static void configureEntry(final CatEntryBean newEntry, final XnatResourceInfo info, boolean modified) {
        if (info.getDescription() != null) {
            newEntry.setDescription(info.getDescription());
        }
        if (info.getFormat() != null) {
            newEntry.setFormat(info.getFormat());
        }
        if (info.getContent() != null) {
            newEntry.setContent(info.getContent());
        }
        if (info.getTags().size() > 0) {
            for (final String entry : info.getTags()) {
                final CatEntryTagBean t = new CatEntryTagBean();
                t.setTag(entry);
                newEntry.addTags_tag(t);
            }
        }
        if (info.getMeta().size() > 0) {
            for (final Map.Entry<String, String> entry : info.getMeta().entrySet()) {
                final CatEntryMetafieldBean meta = new CatEntryMetafieldBean();
                meta.setName(entry.getKey());
                meta.setMetafield(entry.getValue());
                newEntry.addMetafields_metafield(meta);
            }
        }

        if (modified) {
            if (info.getUser() != null && newEntry.getModifiedby() == null) {
                newEntry.setModifiedby(info.getUser().getUsername());
            }
            if (info.getLastModified() != null) {
                newEntry.setModifiedtime(info.getLastModified());
            }
            if (info.getEvent_id() != null && newEntry.getModifiedeventid() == null) {
                newEntry.setModifiedeventid(info.getEvent_id().toString());
            }
        } else {
            if (info.getUser() != null && newEntry.getCreatedby() == null) {
                newEntry.setCreatedby(info.getUser().getUsername());
            }
            if (info.getCreated() != null && newEntry.getCreatedtime() == null) {
                newEntry.setCreatedtime(info.getCreated());
            }
            if (info.getEvent_id() != null && newEntry.getCreatedeventid() == null) {
                newEntry.setCreatedeventid(info.getEvent_id().toString());
            }
        }

    }

    public static void configureEntry(final XnatResource newEntry, final XnatResourceInfo info, final XDATUser user) throws Exception {
        if (info.getDescription() != null) {
            newEntry.setDescription(info.getDescription());
        }
        if (info.getFormat() != null) {
            newEntry.setFormat(info.getFormat());
        }
        if (info.getContent() != null) {
            newEntry.setContent(info.getContent());
        }
        if (info.getTags().size() > 0) {
            for (final String entry : info.getTags()) {
                final XnatAbstractresourceTag t = new XnatAbstractresourceTag((UserI) user);
                t.setTag(entry);
                newEntry.setTags_tag(t);
            }
        }
        if (info.getMeta().size() > 0) {
            for (final Map.Entry<String, String> entry : info.getMeta().entrySet()) {
                final XnatAbstractresourceTag t = new XnatAbstractresourceTag((UserI) user);
                t.setTag(entry.getValue());
                t.setName(entry.getKey());
                newEntry.setTags_tag(t);
            }
        }
    }

    public static boolean storeCatalogEntry(final FileWriterWrapperI fi, String dest, final XnatResourcecatalog catResource, final XnatProjectdata proj, final boolean extract, final XnatResourceInfo info, final boolean overwrite, final EventMetaI ci) throws Exception {
        final File catFile = catResource.getCatalogFile(proj.getRootArchivePath());
        final String parentPath = catFile.getParent();
        final CatCatalogBean cat = catResource.getCleanCatalog(proj.getRootArchivePath(), false, null, null);

        String filename = fi.getName();

        int index = filename.lastIndexOf('\\');
        if (index < filename.lastIndexOf('/')) {
            index = filename.lastIndexOf('/');
        }

        if (index > 0) {
            filename = filename.substring(index + 1);
        }

        if (StringUtils.IsEmpty(dest)) {
            dest = filename;
        } else if (dest.startsWith("/")) {
            dest = dest.substring(1);
        }

        String compression_method = ".zip";
        if (filename.contains(".")) {
            compression_method = filename.substring(filename.lastIndexOf("."));
        }

        if (extract && (compression_method.equalsIgnoreCase(".tar") || compression_method.equalsIgnoreCase(".gz") || compression_method.equalsIgnoreCase(".zip") || compression_method.equalsIgnoreCase(".zar"))) {
            File destinationDir = catFile.getParentFile();
            final InputStream is = fi.getInputStream();

            ZipI zipper;
            if (compression_method.equalsIgnoreCase(".tar")) {
                zipper = new TarUtils();
            } else if (compression_method.equalsIgnoreCase(".gz")) {
                zipper = new TarUtils();
                zipper.setCompressionMethod(ZipOutputStream.DEFLATED);
            } else {
                zipper = new ZipUtils();
            }

            @SuppressWarnings("unchecked")
            final List<File> files = zipper.extract(is, destinationDir.getAbsolutePath(), overwrite, ci);

            for (final File f : files) {
                if (!f.isDirectory()) {
                    final String relative = destinationDir.toURI().relativize(f.toURI()).getPath();

                    final CatEntryI e = getEntryByURI(cat, relative);

                    if (e == null) {
                        final CatEntryBean newEntry = new CatEntryBean();
                        newEntry.setUri(relative);
                        newEntry.setName(f.getName());

                        configureEntry(newEntry, info, false);

                        cat.addEntries_entry(newEntry);
                    }
                }
            }
        } else {
            final File saveTo = new File(parentPath, (dest != null) ? dest : filename);

            if (saveTo.exists() && !overwrite) {
                throw new IOException("File already exists" + saveTo.getCanonicalPath());
            } else if (saveTo.exists()) {
                final CatEntryBean e = (CatEntryBean) getEntryByURI(cat, dest);

                CatalogUtils.moveToHistory(catFile, saveTo, e, ci);
            }

            if (!saveTo.getParentFile().mkdirs() && !saveTo.getParentFile().exists()) {
                throw new Exception("Failed to create required directory: " + saveTo.getParentFile().getAbsolutePath());
            }

            fi.write(saveTo);

            if (saveTo.isDirectory()) {
                @SuppressWarnings("unchecked")
                final Iterator<File> iter = org.apache.commons.io.FileUtils.iterateFiles(saveTo, null, true);
                while (iter.hasNext()) {
                    final File movedF = iter.next();

                    String relativePath = FileUtils.RelativizePath(saveTo, movedF).replace('\\', '/');
                    if (dest != null) {
                        relativePath = dest + "/" + relativePath;
                    }
                    updateEntry(cat, relativePath, movedF, info, ci);
                }

            } else {
                updateEntry(cat, dest, saveTo, info, ci);
            }
        }

        writeCatalogToFile(cat, catFile);

        return true;
    }

    public static void refreshAuditSummary(CatCatalogI cat) {
        CatCatalogMetafieldI field = null;
        for (CatCatalogMetafieldI mf : cat.getMetafields_metafield()) {
            if ("AUDIT".equals(mf.getName())) {
                field = mf;
                break;
            }
        }

        if (field == null) {
            field = new CatCatalogMetafieldBean();
            field.setName("AUDIT");
            try {
                cat.addMetafields_metafield(field);
            } catch (Exception ignored) {
            }
        }


        field.setMetafield(convertAuditToString(buildAuditSummary(cat)));
    }

    public static Map<String, Map<String, Integer>> retrieveAuditySummary(CatCatalogI cat) {
        if (cat == null) return new HashMap<String, Map<String, Integer>>();
        CatCatalogMetafieldI field = null;
        for (CatCatalogMetafieldI mf : cat.getMetafields_metafield()) {
            if ("AUDIT".equals(mf.getName())) {
                field = mf;
                break;
            }
        }

        if (field != null) {
            return convertAuditToMap(field.getMetafield());
        } else {
            return buildAuditSummary(cat);
        }

    }

    public static void addAuditEntry(Map<String, Map<String, Integer>> summary, String key, String action, Integer i) {
        if (!summary.containsKey(key)) {
            summary.put(key, new HashMap<String, Integer>());
        }

        if (!summary.get(key).containsKey(action)) {
            summary.get(key).put(action, 0);
        }

        summary.get(key).put(action, summary.get(key).get(action) + i);
    }

    public static void addAuditEntry(Map<String, Map<String, Integer>> summary, Integer eventId, Object d, String action, Integer i) {
        String key = eventId + ":" + d;
        addAuditEntry(summary, key, action, i);
    }

    public static void writeCatalogToFile(CatCatalogI xml, File dest) throws Exception {
        writeCatalogToFile(xml, dest, DEFAULT_CHECKSUM);
    }

    public static void writeCatalogToFile(CatCatalogI xml, File dest, boolean calculateChecksums) throws Exception {
        if (calculateChecksums) {
            CatalogUtils.calculateResourceChecksums(xml, dest);
        }

        if (!dest.getParentFile().exists()) {
            if (!dest.getParentFile().mkdirs() && !dest.getParentFile().exists()) {
                throw new Exception("Failed to create required directory: " + dest.getParentFile().getAbsolutePath());
            }
        }

        refreshAuditSummary(xml);

        final FileOutputStream fos = new FileOutputStream(dest);
        OutputStreamWriter fw;
        try {
            final FileLock fl = fos.getChannel().lock();
            try {
                fw = new OutputStreamWriter(fos);
                xml.toXML(fw);
                fw.flush();
            } finally {
                fl.release();
            }
        } finally {
            fos.close();
        }
    }

    public static File getCatalogFile(final String rootPath, final XnatResourcecatalogI resource) {
        String fullPath = getFullPath(rootPath, resource);
        if (fullPath.endsWith("\\")) {
            fullPath = fullPath.substring(0, fullPath.length() - 1);
        }
        if (fullPath.endsWith("/")) {
            fullPath = fullPath.substring(0, fullPath.length() - 1);
        }


        File f = new File(fullPath);
        if (!f.exists()) {
            f = new File(fullPath + ".gz");
        }

        if (!f.exists()) {
            f = new File(fullPath);

            CatCatalogBean cat = new CatCatalogBean();
            if (resource.getLabel() != null) {
                cat.setId(resource.getLabel());
            } else {
                cat.setId("" + Calendar.getInstance().getTimeInMillis());
            }

            try {
                if (!f.getParentFile().mkdirs() && !f.getParentFile().exists()) {
                    throw new Exception("Failed to create required directory: " + f.getParentFile().getAbsolutePath());
                }

                FileWriter fw = new FileWriter(f);
                cat.toXML(fw, true);
                fw.close();
            } catch (IOException exception) {
                logger.error("Error writing to the folder: " + f.getParentFile().getAbsolutePath(), exception);
            } catch (Exception exception) {
                logger.error("Error creating the folder: " + f.getParentFile().getAbsolutePath(), exception);
            }
        }

        return f;
    }

    public static CatCatalogBean getCatalog(File catF) {
        if (!catF.exists()) return null;
        try {
            InputStream fis = new FileInputStream(catF);
            if (catF.getName().endsWith(".gz")) {
                fis = new GZIPInputStream(fis);
            }

            BaseElement base;

            XDATXMLReader reader = new XDATXMLReader();
            base = reader.parse(fis);

            if (base instanceof CatCatalogBean) {
                return (CatCatalogBean) base;
            }
        } catch (FileNotFoundException e) {
            logger.error("", e);
        } catch (IOException e) {
            logger.error("", e);
        } catch (SAXException e) {
            logger.error("", e);
        }

        return null;
    }

    public static CatCatalogBean getCatalog(String rootPath, XnatResourcecatalogI resource) {
        File catF = null;
        try {
            catF = CatalogUtils.getCatalogFile(rootPath, resource);
            if (catF.getName().endsWith(".gz")) {
                    FileUtils.GUnzipFiles(catF);
                    catF = CatalogUtils.getCatalogFile(rootPath, resource);
            }
        } catch (FileNotFoundException exception) {
            logger.error("File not found at: " + rootPath, exception);
        } catch (IOException exception) {
            logger.error("Error reading file at: " + rootPath, exception);
        } catch (Exception exception) {
            logger.error("Unknown exception reading file at: " + rootPath, exception);
        }

        return catF != null ? getCatalog(catF) : null;
    }

    public static CatCatalogBean getCleanCatalog(String rootPath, XnatResourcecatalogI resource, boolean includeFullPaths) {
        return getCleanCatalog(rootPath, resource, includeFullPaths, null, null);
    }

    public static CatCatalogBean getCleanCatalog(String rootPath, XnatResourcecatalogI resource, boolean includeFullPaths, UserI user, EventMetaI c) {
        File catF = null;
        try {
            catF = handleCatalogFile(rootPath, resource, catF);

            InputStream fis = new FileInputStream(catF);
            if (catF.getName().endsWith(".gz")) {
                fis = new GZIPInputStream(fis);
            }

            BaseElement base;

            XDATXMLReader reader = new XDATXMLReader();
            base = reader.parse(fis);

            String parentPath = catF.getParent();

            if (base instanceof CatCatalogBean) {
                CatCatalogBean cat = (CatCatalogBean) base;
                formalizeCatalog(cat, parentPath, user, c);

                if (includeFullPaths) {
                    CatCatalogMetafieldBean mf = new CatCatalogMetafieldBean();
                    mf.setName("CATALOG_LOCATION");
                    mf.setMetafield(parentPath);
                    cat.addMetafields_metafield(mf);
                }

                return cat;
            }
        } catch (FileNotFoundException exception) {
            logger.error("Couldn't find file " + (catF != null ? "indicated by " + catF.getAbsolutePath() : "of unknown location"), exception);
        } catch (SAXException exception) {
            logger.error("Couldn't parse file " + (catF != null ? "indicated by " + catF.getAbsolutePath() : "of unknown location"), exception);
        } catch (IOException exception) {
            logger.error("Couldn't parse or unzip file " + (catF != null ? "indicated by " + catF.getAbsolutePath() : "of unknown location"), exception);
        } catch (Exception exception) {
            logger.error("Unknown error handling file " + (catF != null ? "indicated by " + catF.getAbsolutePath() : "of unknown location"), exception);
        }

        return null;
    }

    public static boolean formalizeCatalog(final CatCatalogI cat, final String catPath, UserI user, EventMetaI now) {
        return formalizeCatalog(cat, catPath, cat.getId(), user, now);
    }

    public static String getFullPath(String rootPath, XnatResourcecatalogI resource) {

        String fullPath = StringUtils.ReplaceStr(FileUtils.AppendRootPath(rootPath, resource.getUri()), "\\", "/");
        while (fullPath.contains("//")) {
            fullPath = StringUtils.ReplaceStr(fullPath, "//", "/");
        }

        if (!fullPath.endsWith("/")) {
            fullPath += "/";
        }

        return fullPath;
    }

    public boolean modifyEntry(CatCatalogI cat, CatEntryI oldEntry, CatEntryI newEntry) {
        for (int i = 0; i < cat.getEntries_entry().size(); i++) {
            CatEntryI e = cat.getEntries_entry().get(i);
            if (e.getUri().equals(oldEntry.getUri())) {
                cat.getEntries_entry().remove(i);
                cat.getEntries_entry().add(newEntry);
                return true;
            }
        }

        for (CatCatalogI subset : cat.getSets_entryset()) {
            if (modifyEntry(subset, oldEntry, newEntry)) {
                return true;
            }
        }

        return false;
    }

    public static List<File> findHistoricalCatFiles(File catFile) {
        final List<File> files = new ArrayList<File>();

        final File historyDir = FileUtils.BuildHistoryParentFile(catFile);

        final String name = catFile.getName();

        final FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File arg0, String arg1) {
                return (arg1.equals(name));
            }
        };

        if (historyDir.exists()) {
            final File[] historyFiles = historyDir.listFiles();
            if (historyFiles != null) {
                for (File d : historyFiles) {
                    if (d.isDirectory()) {
                        final File[] matched = d.listFiles(filter);
                        if (matched != null && matched.length > 0) {
                            files.addAll(Arrays.asList(matched));
                        }
                    }
                }
            }
        }

        return files;
    }

    public static boolean removeEntry(CatCatalogI cat, CatEntryI entry) {
        for (int i = 0; i < cat.getEntries_entry().size(); i++) {
            CatEntryI e = cat.getEntries_entry().get(i);
            if (e.getUri().equals(entry.getUri())) {
                cat.getEntries_entry().remove(i);
                return true;
            }
        }

        for (CatCatalogI subset : cat.getSets_entryset()) {
            if (removeEntry(subset, entry)) {
                return true;
            }
        }

        return false;
    }

    public static Boolean maintainFileHistory() {
        if (_maintainFileHistory == null) {
            _maintainFileHistory = XDAT.getBoolSiteConfigurationProperty("audit.maintain-file-history", false);
        }
        return _maintainFileHistory;
    }

    public static void moveToHistory(File catFile, File f, CatEntryBean entry, EventMetaI ci) throws Exception {
        //move existing file to audit trail
        if (CatalogUtils.maintainFileHistory()) {
            final File newFile = FileUtils.MoveToHistory(f, EventUtils.getTimestamp(ci));
            addCatHistoryEntry(catFile, newFile.getAbsolutePath(), entry, ci);
        }
    }

    public static void addCatHistoryEntry(File catFile, String f, CatEntryBean entry, EventMetaI ci) throws Exception {
        //move existing file to audit trail
        CatEntryBean newEntryBean = (CatEntryBean) entry.copy();
        newEntryBean.setUri(f);
        if (ci != null) {
            newEntryBean.setModifiedtime(ci.getEventDate());
            if (ci.getEventId() != null) {
                newEntryBean.setModifiedeventid(ci.getEventId().toString());
            }
            if (ci.getUser() != null) {
                newEntryBean.setModifiedby(ci.getUser().getUsername());
            }
        }

        File newCatFile = FileUtils.BuildHistoryFile(catFile, EventUtils.getTimestamp(ci));
        CatCatalogBean newCat;
        if (newCatFile.exists()) {
            newCat = CatalogUtils.getCatalog(newCatFile);
        } else {
            newCat = new CatCatalogBean();
        }

        newCat.addEntries_entry(newEntryBean);

        CatalogUtils.writeCatalogToFile(newCat, newCatFile);
    }

    public static boolean populateStats(XnatAbstractresource abstractResource, String rootPath) {
        Integer c = abstractResource.getCount(rootPath);
        Long s = abstractResource.getSize(rootPath);

        boolean modified = false;

        if (!c.equals(abstractResource.getFileCount())) {
            abstractResource.setFileCount(c);
            modified = true;
        }

        if (!s.equals(abstractResource.getFileSize())) {
            abstractResource.setFileSize(s);
            modified = true;
        }

        return modified;
    }

    private static void updateEntry(CatCatalogBean cat, String dest, File f, XnatResourceInfo info, EventMetaI ci) {
        final CatEntryBean e = (CatEntryBean) getEntryByURI(cat, dest);

        if (e == null) {
            final CatEntryBean newEntry = new CatEntryBean();
            newEntry.setUri(dest);
            newEntry.setName(f.getName());

            configureEntry(newEntry, info, false);

            cat.addEntries_entry(newEntry);
        } else {
            if (ci != null) {
                if (ci.getUser() != null)
                    e.setModifiedby(ci.getUser().getUsername());
                e.setModifiedtime(ci.getEventDate());
                if (ci.getEventId() != null) {
                    e.setModifiedeventid(ci.getEventId().toString());
                }
            }
        }
    }

    private static String convertAuditToString(Map<String, Map<String, Integer>> summary) {
        StringBuilder sb = new StringBuilder();
        int counter1 = 0;
        for (Map.Entry<String, Map<String, Integer>> entry : summary.entrySet()) {
            if (counter1++ > 0) sb.append("|");
            sb.append(entry.getKey()).append("=");
            int counter2 = 0;
            for (Map.Entry<String, Integer> sub : entry.getValue().entrySet()) {
                sb.append(sub.getKey()).append(":").append(sub.getValue());
                if (counter2++ > 0) sb.append(";");
            }

        }
        return sb.toString();
    }

    private static Map<String, Map<String, Integer>> convertAuditToMap(String audit) {
        Map<String, Map<String, Integer>> summary = new HashMap<String, Map<String, Integer>>();
        for (String changeSet : audit.split("|")) {
            String[] split1 = changeSet.split("=");
            if (split1.length > 1) {
                String key = split1[0];
                Map<String, Integer> counts = new HashMap<String, Integer>();
                for (String operation : split1[1].split(";")) {
                    String[] entry = operation.split(":");
                    counts.put(entry[0], Integer.valueOf(entry[1]));
                }
                summary.put(key, counts);
            }
        }
        return summary;
    }

    private static Map<String, Map<String, Integer>> buildAuditSummary(CatCatalogI cat) {
        Map<String, Map<String, Integer>> summary = new HashMap<String, Map<String, Integer>>();
        buildAuditSummary(cat, summary);
        return summary;
    }

    private static void buildAuditSummary(CatCatalogI cat, Map<String, Map<String, Integer>> summary) {
        for (CatCatalogI subSet : cat.getSets_entryset()) {
            buildAuditSummary(subSet, summary);
        }

        for (CatEntryI entry : cat.getEntries_entry()) {
            addAuditEntry(summary, entry.getCreatedeventid(), entry.getCreatedtime(), ChangeSummaryBuilderA.ADDED, 1);

            if (entry.getModifiedtime() != null) {
                addAuditEntry(summary, entry.getModifiedeventid(), entry.getModifiedtime(), ChangeSummaryBuilderA.REMOVED, 1);
            }
        }
    }

    private static File handleCatalogFile(final String rootPath, final XnatResourcecatalogI resource, File catF) throws Exception {
        catF = CatalogUtils.getCatalogFile(rootPath, resource);
        if (catF.getName().endsWith(".gz")) {
            try {
                FileUtils.GUnzipFiles(catF);
                catF = CatalogUtils.getCatalogFile(rootPath, resource);
            } catch (FileNotFoundException e) {
                logger.error("", e);
            } catch (IOException e) {
                logger.error("", e);
            }
        }
        return catF;
    }

    private static boolean formalizeCatalog(final CatCatalogI cat, final String catPath, String header, UserI user, EventMetaI now) {
        boolean modified = false;

        for (CatCatalogI subSet : cat.getSets_entryset()) {
            if (formalizeCatalog(subSet, catPath, header + "/" + subSet.getId(), user, now)) {
                modified = true;
            }
        }
        for (CatEntryI entry : cat.getEntries_entry()) {
            if (entry.getCreatedby() == null && user != null) {
                entry.setCreatedby(user.getUsername());
                modified = true;
            }
            if (entry.getCreatedtime() == null && now != null) {
                ((CatEntryBean) entry).setCreatedtime(now.getEventDate());
                modified = true;
            }
            if (entry.getCreatedeventid() == null && now != null && now.getEventId() != null) {
                ((CatEntryBean) entry).setCreatedeventid(now.getEventId().toString());
                modified = true;
            }
            if (entry.getId() == null || !entry.getId().equals("")) {
                String entryPath = StringUtils.ReplaceStr(FileUtils.AppendRootPath(catPath, entry.getUri()), "\\", "/");
                File f = getFileOnLocalFileSystem(entryPath);
                if (f != null) {
                    entry.setId(header + "/" + f.getName());
                    modified = true;
                } else {
                    logger.error("Missing Resource:" + entryPath);
                }
            }
        }

        return modified;
    }

    private static final Logger logger = Logger.getLogger(CatalogUtils.class);

    private static Boolean _maintainFileHistory = null;
}
