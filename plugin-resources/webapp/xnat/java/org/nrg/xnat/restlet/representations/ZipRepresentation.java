// Copyright 2010 Washington University School of Medicine All Rights Reserved
package org.nrg.xnat.restlet.representations;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipOutputStream;

import org.nrg.xft.utils.zip.TarUtils;
import org.nrg.xft.utils.zip.ZipI;
import org.nrg.xft.utils.zip.ZipUtils;
import org.restlet.data.MediaType;
import org.restlet.resource.OutputRepresentation;

public class ZipRepresentation extends OutputRepresentation {
	private ArrayList<ZipEntry> _entries=new ArrayList<ZipEntry>();
	private ArrayList<String> _tokens=new ArrayList<String>();
	private MediaType mt=null;
	
	public ZipRepresentation(MediaType mt,String token) {
		super(mt);
		this.mt=mt;
		_tokens.add(token);
	}
	
	public ZipRepresentation(MediaType mt,ArrayList<String> token) {
		super(mt);
		this.mt=mt;
		_tokens=token;
	}
	
	public void addEntry(String p, File f){
		_entries.add(new ZipFileEntry(p,f));
	}
	
	public void addEntry(String p, InputStream is){
		_entries.add(new ZipStreamEntry(p,is));
	}
	
	public void addEntry(File f){
		String p = f.getAbsolutePath().replace('\\','/');
		int i=-1;
		String _token=null;
		
		for(String token:_tokens){
			_token=token;
			i=p.indexOf('/'+ _token + '/');
			if(i==-1){
				i=p.indexOf('/'+ _token);
				
				if(i==-1){
					i=p.indexOf(_token+'/');
					if(i>-1){
						i=(p.substring(0, i)).lastIndexOf('/') +1;
						break;
					}
				}else{
					i++;
					break;
				}
			}else{
				i++;
				break;
			}
		}
		
		if(i==-1){
			if(p.indexOf(":")>-1){
				p=p.substring(p.indexOf(":"));
				p=p.substring(p.indexOf("/"));
				p=_token+p;
			}else{
				p=_token+p;
			}
			
			_entries.add(new ZipFileEntry(p,f));
		}else{
			
			_entries.add(new ZipFileEntry(p.substring(i),f));
		}
	}
	
	public void addAll(List<File> fs){
		for(File f: fs){
			this.addEntry(f);
		}
	}
	
	public String getTokenName(){
		if(this._tokens.size()>1){
			return "various";
		}else{
			return this._tokens.get(0);
		}
	}
	
	public int getEntryCount(){
		return this._entries.size();
	}

	@Override
	public String getDownloadName() {
		if (this.mt.equals(MediaType.APPLICATION_GNU_TAR))
        {
            return getTokenName()+".tar.gz";
        }else{
            return getTokenName() +".zip";
        }
	}

	@Override
	public void write(OutputStream os) throws IOException {
		ZipI zip = null;
        if (this.mt.equals(MediaType.APPLICATION_GNU_TAR))
        {
            zip = new TarUtils();
            zip.setOutputStream(os,ZipOutputStream.DEFLATED);
            this.setDownloadName(getTokenName()+".tar.gz");
            this.setDownloadable(true);
        }else{
            zip = new ZipUtils();
            zip.setOutputStream(os,ZipOutputStream.DEFLATED);
            this.setDownloadName(getTokenName() +".zip");
            this.setDownloadable(true);
        }
            
        for(final ZipEntry ze: this._entries)
        {
        	if(ze instanceof ZipFileEntry){
                if (!((ZipFileEntry)ze).getF().isDirectory())
                {
                    zip.write(ze.getPath(),((ZipFileEntry)ze).getF());
                }
        	}else{
                zip.write(ze.getPath(),((ZipStreamEntry)ze).getInputStream());
        	}
        }
              
        // Complete the ZIP file
        zip.close();
	}

	public abstract class ZipEntry{
		String path=null;
		
		public String getPath() {
			return path;
		}
		public void setPath(String path) {
			this.path = path;
		}
	}
    
	public class ZipFileEntry extends ZipEntry {
		public ZipFileEntry(String p,File f){
			path=p;
			file=f;
		}
		
		File file=null;
		
		public File getF() {
			return file;
		}
		public void setF(File f) {
			this.file = f;
		}
	}

    
	public class ZipStreamEntry extends ZipEntry {
		public ZipStreamEntry(String p,InputStream _is){
			path=p;
			is=_is;
		}
		
		private InputStream is=null;
		
		public InputStream getInputStream() {
			return is;
		}
		public void setInputStream(InputStream _is) {
			this.is = _is;
		}
	}
}


