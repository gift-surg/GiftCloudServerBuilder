/**
 * Copyright (c) 2011 Washington University
 */
package org.nrg.xnat.archive;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xnat.helpers.ZipEntryFileWriterWrapper;
import org.nrg.xnat.restlet.actions.importer.ImporterHandlerA;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public final class DicomZipImporter extends ImporterHandlerA {
    private final InputStream in;
    private final Object listenerControl;
    private final XDATUser u;
    private final Map<String,Object> params;

    /**
     * @param listenerControl
     * @param u
     * @param fw
     * @param params
     */
    public DicomZipImporter(final Object listenerControl,
            final XDATUser u,
            final FileWriterWrapperI fw,
            final Map<String, Object> params)
    throws ClientException,IOException {
        super(listenerControl, u, fw, params);
        this.listenerControl = listenerControl;
        this.u = u;
        this.params = params;
        this.in = fw.getInputStream();
    }

    /* (non-Javadoc)
     * @see org.nrg.xnat.restlet.actions.importer.ImporterHandlerA#call()
     */
    @Override
    public List<String> call() throws ClientException,ServerException {
        try {
            IOException ioexception = null;
            final ZipInputStream zin = new ZipInputStream(in);
            try {
                final Set<String> uris = Sets.newLinkedHashSet();
                ZipEntry ze;
                while (null != (ze = zin.getNextEntry())) {
                    if (!ze.isDirectory()) {
                        uris.addAll(new GradualDicomImporter(listenerControl, u,
                                new ZipEntryFileWriterWrapper(ze, zin), params)
                        .call());
                    }
                }
                return Lists.newArrayList(uris);
            } finally {
                try {
                    zin.close();
                } catch (IOException e) {
                    throw null == ioexception ? e : ioexception;
                }
            }
        } catch (IOException e) {
            throw new ClientException("unable to read data from zip file", e);
        }
    }
}
