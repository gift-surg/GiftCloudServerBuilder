/**
 * Copyright 2010,2011 Washington University
 */
package org.nrg.xnat.helpers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.restlet.resource.Representation;

/**
 * @author Tim Olsen <olsent@mir.wustl.edu>
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public class FileWriterWrapper implements FileWriterWrapperI {
    private final FileItem fi;
    private final Representation entry;
    private final String name;

    public FileWriterWrapper(final FileItem fi, final String name) {
        this.fi = fi;
        this.entry = null;
        this.name = name;
    }

    public FileWriterWrapper(final Representation representation, final String name) {
        this.fi = null;
        this.entry = representation;
        this.name = name;
    }

    /*
     * (non-Javadoc)
     * @see org.nrg.xnat.restlet.util.FileWriterWrapperI#delete()
     */
    public void delete(){
        if (fi!=null) {
            fi.delete();
        }
    }

    /*
     * (non-Javadoc)
     * @see org.nrg.xnat.restlet.util.FileWriterWrapperI#getInputStream()
     */
    public InputStream getInputStream() throws IOException {
        return null == fi ? entry.getStream() : fi.getInputStream();
    }

    /*
     * (non-Javadoc)
     * @see org.nrg.xnat.restlet.util.FileWriterWrapperI#getName()
     */
    public String getName(){
        return name;
    }

    /*
     * (non-Javadoc)
     * @see org.nrg.xnat.restlet.util.FileWriterWrapperI#getType()
     */
    public UPLOAD_TYPE getType() {
        return null == entry ? FileWriterWrapperI.UPLOAD_TYPE.MULTIPART : FileWriterWrapperI.UPLOAD_TYPE.INBODY;
    }

    /*
     * (non-Javadoc)
     * @see org.nrg.xnat.restlet.util.FileWriterWrapperI#write(java.io.File)
     */
    public void write(final File f) throws IOException,Exception {
        if (null == fi) {
            final FileOutputStream fw = new FileOutputStream(f);
            IOException ioexception = null;
            try {
                if (entry.getSize()>2000000) {
                    IOUtils.copyLarge(entry.getStream(), fw);
                } else {
                    IOUtils.copy(entry.getStream(), fw);
                }
            } catch (IOException e) {
                throw ioexception = e;
            } finally {
                try {
                    fw.close();
                } catch (IOException e) {
                    throw null == ioexception ? e : ioexception;
                }
            }
        } else {
            fi.write(f);
        }
    }
}