/*
 * org.nrg.xnat.dicom.DicomDataUsingTestCase
 * XNAT http://www.xnat.org
 * Copyright (c) 2014, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 7/10/13 9:04 PM
 */
package org.nrg.xnat.dicom;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.nrg.io.FileWalkIterator;

import java.io.*;
import java.net.URL;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DicomDataUsingTestCase extends TestCase implements Iterable<File> {
    private static final String SAMPLE_1_URL = "http://nrg.wustl.edu/projects/DICOM/sample1.zip";
    private static final File sample1;
    static {
        final String altPath = System.getProperty("dicom.sample.dir");
        if (null == altPath) {
            sample1 = new File(System.getProperty("java.io.tmpdir"), "dicom-sample1");
        } else {
            sample1 = new File(altPath);
        }
    }

    public Iterator<File> iterator() {
        return new FileWalkIterator(sample1, null);
    }
    
    protected File getSampleData() { return sample1; }

    protected DicomDataUsingTestCase() {
        final Logger logger = Logger.getLogger(DicomDataUsingTestCase.class);
        synchronized (DicomDataUsingTestCase.class) {
            if (sample1.isDirectory()) {
                // assume it's already filled.
            } else {
                try {
                    final URL url = new URL(SAMPLE_1_URL);
                    IOException ioe = null;
                    final ZipInputStream zin = new ZipInputStream(url.openStream());
                    try {
                        for (ZipEntry ze; null != (ze = zin.getNextEntry()); ) {
                            if (!ze.isDirectory()) {
                                final File f = new File(sample1, ze.getName());
                                f.getParentFile().mkdirs();
                                final OutputStream out = new BufferedOutputStream(new FileOutputStream(f));
                                try {
                                    IOUtils.copy(zin, out);
                                } catch (IOException e) {
                                    throw ioe = e;
                                } finally {
                                    try {
                                        out.close();
                                    } catch (IOException e) {
                                        if (null != ioe) {
                                            logger.error("Unable to close sample data file", e);
                                            throw ioe;
                                        } else {
                                            throw e;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        throw null == ioe ? ioe = e : ioe;
                    } finally {
                        try {
                            zin.close();
                        } catch (IOException e) {
                            if (null != ioe) {
                                logger.error("Unable to close zip entry", e);
                                throw ioe;
                            } else {
                                throw e;
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Unable to download required sample DICOM data from " + SAMPLE_1_URL, e);
                }
            }
        }
    }
}