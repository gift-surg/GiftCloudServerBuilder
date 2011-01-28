/**
 * Copyright 2010 Washington University
 */
package org.nrg.xnat.helpers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.fileupload.FileItem;
import org.restlet.resource.Representation;

public class FileWriterWrapper{
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
	
	public String getName(){
		return name;
	}
	

	
	public InputStream getInputStream() throws IOException,Exception{
		if(fi!=null){
			return fi.getInputStream();
		}else{
			return entry.getStream();
		}
	}
	
	public void delete(){
		if(fi!=null){
			fi.delete();
		}
	}
}