package org.nrg.xnat.helpers.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.nrg.xnat.restlet.util.FileWriterWrapperI.UPLOAD_TYPE;

public class StoredFile implements FileWriterWrapperI{
	final File stored;
	final boolean overwrite;
	
	public StoredFile(final File f, final boolean allowOverwrite){
		stored=f;
		this.overwrite=allowOverwrite;
	}
	
	public void write(File f) throws IOException {
		if(f.isDirectory()||stored.isDirectory()){
			org.nrg.xft.utils.FileUtils.MoveDir(stored, f, overwrite);
		}else{
			org.nrg.xft.utils.FileUtils.MoveFile(stored, f, overwrite);
		}
	}
	
	public String getName() {
		return stored.getName();
	}
	
	public InputStream getInputStream() throws IOException {
		return new FileInputStream(stored);
	}
	
	public void delete() {
		if(stored.exists())FileUtils.deleteQuietly(stored);
	}
	
	@Override
	public UPLOAD_TYPE getType() {
		return UPLOAD_TYPE.OTHER;
	}
}