/**
 * Copyright (c) 2010 Washington University
 */
package org.nrg.xnat.restlet.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.fileupload.FileItem;
import org.restlet.resource.Representation;

public class FileWriterWrapper implements FileWriterWrapperI{
	public FileItem fi=null;
	public Representation entry=null;
	public String name=null;
	
	public FileWriterWrapper(FileItem f,String n){
		fi=f;
		name=n;
	}

	public FileWriterWrapper(Representation f,String n){
		entry=f;
		name=n;
	}
	
	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.FileWriterWrapperI#write(java.io.File)
	 */
	public void write(File f) throws IOException,Exception{
		if(fi!=null){
			fi.write(f);
		}else{
			FileOutputStream fw = new FileOutputStream(f);
			if(entry.getSize()>2000000){
				org.apache.commons.io.IOUtils.copyLarge(entry.getStream(), fw);
			}else{
				org.apache.commons.io.IOUtils.copy(entry.getStream(), fw);
			}
			fw.close();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.FileWriterWrapperI#getName()
	 */
	public String getName(){
		return name;
	}
	

	
	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.FileWriterWrapperI#getInputStream()
	 */
	public InputStream getInputStream() throws IOException,Exception{
		if(fi!=null){
			return fi.getInputStream();
		}else{
			return entry.getStream();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.nrg.xnat.restlet.util.FileWriterWrapperI#delete()
	 */
	public void delete(){
		if(fi!=null){
			fi.delete();
		}
	}

	@Override
	public UPLOAD_TYPE getType() {
		if(entry!=null){
			return FileWriterWrapperI.UPLOAD_TYPE.INBODY;
		}else{
			return FileWriterWrapperI.UPLOAD_TYPE.MULTIPART;
		}
		
	}
}