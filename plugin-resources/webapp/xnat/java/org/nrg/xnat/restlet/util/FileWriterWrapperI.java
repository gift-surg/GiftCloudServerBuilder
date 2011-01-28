/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.xnat.restlet.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface FileWriterWrapperI {
	public static enum UPLOAD_TYPE{INBODY,MULTIPART};

	public abstract void write(File f) throws IOException, Exception;

	public abstract String getName();

	public abstract InputStream getInputStream() throws IOException, Exception;

	public abstract void delete();

	public abstract UPLOAD_TYPE getType();

}