/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.xnat.restlet.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Tim Olsen <olsent@mir.wustl.edu>
 * @author Kevin A. Archie <karchie@wustl.edu>
 *
 */
public interface FileWriterWrapperI {
	static enum UPLOAD_TYPE{INBODY,MULTIPART,OTHER};

	void write(File f) throws Exception;

	String getName();

	InputStream getInputStream() throws IOException;

	void delete();

	UPLOAD_TYPE getType();

}