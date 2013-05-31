package org.nrg.xnat.helpers.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;

public class StoredFile implements FileWriterWrapperI{
    private final File stored;
    private final boolean overwrite;
    private final String nestedPath;
    private final boolean isReference;
	
	public StoredFile(final File f, final boolean allowOverwrite){
		stored=f;
		this.overwrite=allowOverwrite;
        this.nestedPath = null;
        this.isReference = false;
	}
	
    public StoredFile(final File f, final boolean allowOverwrite, final String nestedPath, final boolean isReference) {
		stored=f;
		this.overwrite=allowOverwrite;
        this.nestedPath = nestedPath;
        this.isReference = isReference;
	}

    @Override
	public void write(File f) throws IOException {
		if(f.isDirectory()||stored.isDirectory()){
			if (isReference) {
			org.nrg.xft.utils.FileUtils.MoveDir(stored, f, overwrite);
		}else{
                org.nrg.xft.utils.FileUtils.CopyDir(stored, f, overwrite);
            }
		} else if (isReference) {
			org.nrg.xft.utils.FileUtils.CopyFile(stored, f, overwrite);
		} else {
			org.nrg.xft.utils.FileUtils.MoveFile(stored, f, overwrite);
		}
	}
	
    @Override
	public String getName() {
		return stored.getName();
	}
	
    @Override
    public String getNestedPath() {
        return nestedPath;
    }

    @Override
	public InputStream getInputStream() throws IOException {
		return new FileInputStream(stored);
	}
	
    @Override
	public void delete() {
		if(stored.exists()) {
            FileUtils.deleteQuietly(stored);
        }
	}
	
	@Override
	public UPLOAD_TYPE getType() {
		return UPLOAD_TYPE.OTHER;
	}
}